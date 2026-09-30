# Eventos de reposição após commit

## Contexto

O consumo de estoque e o alerta de reposição compartilham a mesma transação.
`BatchService.consumeStock` chama `StockAlertService.checkInventoryAndNotify()`
antes do commit. Esse método gera o PDF e envia o e-mail pelo Resend.

Essa ordem produz dois problemas:

- uma falha no Resend pode reverter um consumo já correto;
- o gestor pode ser notificado de uma movimentação que depois sofre rollback.

A arquitetura-alvo descreve `RestockNeededEvent` publicado somente após
commit bem-sucedido. Esta spec define o **primeiro incremento** dessa
evolução: evento in-process após commit. RabbitMQ, outbox, retry, DLQ e
histórico persistido de notificações permanecem fora.

## Objetivo

Desacoplar o efeito externo do alerta da transação de estoque, sem alterar a
regra atual de quando o alerta é disparado nem o conteúdo do relatório.

Após um consumo que não é revertido, o domínio de `inventory` deve registrar
um fato de reposição. O módulo `notification` processa esse fato somente
depois do commit: relê os produtos com estoque baixo, gera o PDF e envia o
e-mail.

## Decisões desta etapa

Estas decisões são requisitos, não detalhes opcionais de implementação:

1. O mecanismo é in-process. Não entra RabbitMQ, Transactional Outbox,
   persistência de `PENDING/SENT/FAILED` nem idempotência contra redelivery.
2. O gatilho permanece o de hoje: qualquer `consumeStock` que não lance
   `InsufficientStockException` produz o evento, inclusive os casos atuais
   em que a quantidade pedida é zero ou negativa e nenhum lote é alterado.
3. `createBatch` e `addStock` continuam sem disparar alerta.
4. O evento é um sinal. O listener relê o estoque baixo como
   `StockAlertService` já faz. Não se introduz regra de “cruzou o limiar”
   nem PDF por produto.
5. A fórmula de reposição permanece `minStock - totalQuantity`, com a
   condição atual `totalQuantity <= minStock`.
6. `inventory` deixa de depender de `notification`.
7. O `@Scheduled` de `StockAlertService` e o destinatário `RESEND_TO`
   permanecem iguais.
8. Isolamento de alerta por `owner`, destinatário por conta e varredura do
   job sem contexto de usuário ficam fora desta spec.
9. Falha no PDF ou no Resend depois do commit não desfaz lote nem log e não
   falha a resposta HTTP do consumo.

## Comportamentos esperados

### Consumo confirmado

Quando `consumeStock` confirma a transação, o sistema publica
`RestockNeededEvent`. Depois do commit, `notification` executa a verificação
já existente de estoque baixo.

Se existirem produtos com estoque baixo, o PDF e o e-mail atuais são
enviados. Se não existirem, nenhum e-mail é enviado.

O evento pode carregar identificador único, `productId` do produto consumido
e instante da ocorrência. Esses dados não alteram a montagem do relatório
nesta etapa.

### Consumo revertido

Quando `consumeStock` lança `InsufficientStockException`, a transação é
revertida. Nenhum `RestockNeededEvent` é publicado e nenhum e-mail de
reposição é enviado por causa dessa requisição.

### Falha do alerta após commit

Se a geração do PDF ou o envio pelo Resend falhar depois do commit:

- os lotes e os logs do consumo permanecem persistidos;
- a API de consumo responde sucesso, com o mesmo contrato HTTP atual de
  consumo bem-sucedido;
- a falha é registrada em log;
- não há nova tentativa nesta etapa além do job agendado já existente.

Uma transação revertida nunca produz notificação válida.

### Entrada de estoque

Cadastro de lote e `addStock` não publicam `RestockNeededEvent` e não
disparam o alerta.

### Agendamento

O job `@Scheduled(initialDelay = 10000, fixedRate = 36000000)` continua
chamando `checkInventoryAndNotify()` diretamente. Ele não passa a emitir
eventos nesta etapa.

## Entradas e saídas

Não há endpoint, payload HTTP, status ou campo de resposta novo.

`POST /api/v1/batches/consume` permanece o contrato atual. A única mudança
observável para o cliente é que uma falha posterior do Resend deixa de
impedir a confirmação do consumo.

## Cenários de erro

| Situação | Resultado |
| --- | --- |
| Estoque insuficiente | Rollback; sem evento; sem e-mail desta requisição |
| Resend ou PDF falha após commit | Estoque confirmado; resposta de sucesso; falha apenas no log |
| Nenhum produto com estoque baixo após o commit | Evento publicado; nenhum e-mail |
| Job agendado falha | Comportamento atual do job; não reverte movimentações anteriores |

## Requisitos não funcionais

- O consumo não espera um provedor externo para confirmar a transação.
- O listener do evento não participa da transação de estoque.
- O tempo de resposta do consumo pode continuar incluindo a execução
  síncrona do alerta após o commit. Assincronicidade por fila fica para
  o plano de RabbitMQ.

## Testes

A implementação deve provar:

- consumo revertido não publica evento e não envia e-mail;
- consumo confirmado publica o evento;
- falha de `EmailSender` após o commit não desfaz lote nem log;
- falha de `EmailSender` não altera o status HTTP de consumo bem-sucedido;
- a regra de estoque baixo e o cálculo do PDF permanecem os atuais;
- FEFO, locks pessimistas e testes de concorrência existentes continuam
  válidos;
- `createBatch` e `addStock` não publicam o evento.

## Critérios de aceitação

1. `BatchService` não chama `StockAlertService`.
2. `consumeStock` confirmado publica `RestockNeededEvent` somente para
   ser processado após o commit.
3. `consumeStock` revertido não publica evento e não envia e-mail.
4. Falha no alerta após o commit não reverte estoque nem falha a resposta
   HTTP do consumo.
5. O relatório PDF, o destinatário `RESEND_TO` e o job agendado permanecem
   iguais.
6. Não há RabbitMQ, outbox, tabela de notificação nem mudança de regra de
   reposição.

## Fora do escopo

- RabbitMQ, retry, DLQ e Transactional Outbox.
- Persistência do estado da notificação (`PENDING`, `SENT`, `FAILED`).
- Idempotência contra redelivery.
- Nova regra de “produto cruzou o limiar”.
- PDF ou e-mail por produto isolado.
- Isolamento de alerta por proprietário e destinatário por conta.
- Alteração do `@Scheduled`.
- Observabilidade, frontend e novos endpoints.
- Renomear `PdfService` para `RestockReportGenerator`.

Esses itens pertencem a etapas posteriores da arquitetura ou a outras specs.
