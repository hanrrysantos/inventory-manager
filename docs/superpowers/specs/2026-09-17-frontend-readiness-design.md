# Preparação do backend para integração com o frontend

## Objetivo

Preparar os contratos mínimos do backend para que uma aplicação React,
TypeScript e Vite consiga autenticar usuários, reconstruir a sessão, tratar
erros de forma previsível e carregar um dashboard sem duplicar regras de
estoque no cliente.

O escopo desta especificação cobre somente as Fases 1 e 2 de
`docs/frontend-readiness.md`. Histórico de movimentações, consultas detalhadas
de lotes, relatórios e implementação do frontend ficam fora desta etapa.

## Estado atual relevante

- A autenticação usa JWT stateless.
- O subject do JWT é o e-mail do usuário.
- `UserResponseDTO` não expõe `role`.
- Não existe `GET /api/v1/users/me`.
- O Spring Security ainda não possui respostas JSON próprias para `401` e `403`.
- O tratamento global não cobre de forma específica erros de validação.
- Não há configuração explícita de CORS.
- Não existe endpoint de resumo do dashboard.
- `ProductMapper` calcula `totalQuantity` somando as quantidades atuais dos
  lotes; essa regra não será alterada nesta etapa.

## Decisões de contrato

### CORS

Adicionar uma configuração de CORS baseada na propriedade de ambiente
`FRONTEND_ORIGINS`, aceitando uma lista separada por vírgulas.

Valor padrão de desenvolvimento:

```text
http://localhost:5173
```

A configuração permitirá somente as origens declaradas, os métodos `GET`,
`POST`, `PUT`, `PATCH`, `DELETE` e `OPTIONS`, e os headers `Authorization` e
`Content-Type`. A aplicação não usará `*` como origem.

O CORS será integrado ao `SecurityFilterChain`, permitindo que preflight
`OPTIONS` seja respondido antes da autenticação.

### Identidade do usuário autenticado

Adicionar:

```text
GET /api/v1/users/me
```

O endpoint exigirá autenticação e usará o e-mail do principal autenticado para
buscar o usuário no banco. O cliente não poderá escolher o `id` da identidade
consultada.

Resposta:

```json
{
  "id": 1,
  "name": "Ana Souza",
  "email": "ana@example.com",
  "role": "ADMIN",
  "createdAt": "2026-09-17T12:00:00"
}
```

`role` será adicionado ao `UserResponseDTO`, ao mapper e à documentação. A
senha nunca será exposta.

### Erros HTTP

As respostas de erro continuarão usando `StandardError`:

```json
{
  "instant": "2026-09-17T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication is required",
  "path": "/api/v1/users/me"
}
```

Serão padronizados:

- `401` para ausência ou invalidez de autenticação;
- `403` para autenticação válida sem a role exigida;
- `400` para corpo com falha de validação;
- `404`, `409`, `422` e erros de negócio já existentes, preservando seus
  significados atuais.

O `AuthenticationEntryPoint` e o `AccessDeniedHandler` escreverão JSON sem
delegar para o HTML padrão do Spring Security. O handler global tratará
`MethodArgumentNotValidException` e retornará uma mensagem estável contendo os
campos inválidos.

As validações adicionadas nesta etapa serão limitadas aos DTOs usados pelos
fluxos necessários para o frontend: autenticação, usuário, produto, categoria
e lote. A regra de negócio de estoque não será movida para o frontend.

### Dashboard

Adicionar:

```text
GET /api/v1/dashboard/summary
```

O endpoint exigirá `USER` ou `ADMIN` e retornará:

```json
{
  "totalQuantity": 714,
  "productCount": 8,
  "lowStockCount": 3,
  "outOfStockCount": 1,
  "expiredBatchCount": 2,
  "inventoryValue": 21480.00,
  "attentionItems": [
    {
      "productId": 3,
      "productName": "Sabonete Natural Lavanda",
      "sku": "SAB-207",
      "quantity": 0,
      "minStock": 25,
      "status": "OUT_OF_STOCK"
    }
  ]
}
```

O cálculo será centralizado em um serviço de leitura e não exporá entidades
JPA. `totalQuantity` manterá a mesma regra do `ProductMapper`, somando a
quantidade atual dos lotes. `inventoryValue` será a soma de `quantity * price`
de cada lote. `expiredBatchCount` contará lotes cuja validade é anterior à
data atual.

Para manter compatibilidade com o comportamento atual, o status de estoque
será calculado com a quantidade total atual do produto:

- `OUT_OF_STOCK` quando `quantity == 0`;
- `LOW_STOCK` quando `quantity <= minStock` e `quantity > 0`;
- `IN_STOCK` quando `quantity > minStock`.

Os itens de atenção serão os produtos `LOW_STOCK` ou `OUT_OF_STOCK`, ordenados
primeiro por severidade e depois por nome. A ordenação e os cálculos pertencem
ao backend.

## Arquitetura

- Configuração CORS e handlers de segurança ficarão em `shared/config` ou
  `auth/config`, conforme a responsabilidade existente, sem criar módulo novo.
- `/users/me` será implementado no módulo `user`, com método específico no
  `UserService` e busca por e-mail no `UserRepository`.
- O dashboard ficará em `dashboard/controller`, `dashboard/service` e
  `dashboard/dto`, com repositories de leitura nos módulos existentes ou uma
  consulta dedicada no módulo dashboard.
- Consultas agregadas deverão ocorrer no banco quando isso reduzir carregamento
  desnecessário; o contrato público será sempre DTO.
- Nenhuma migration será necessária para esta etapa.
- Nenhuma alteração será feita no fluxo transacional de `BatchService`, no FEFO
  ou nos locks pessimistas.

## Testes

### Segurança e integração

- preflight `OPTIONS` com origem permitida retorna os headers CORS esperados;
- origem não permitida não recebe autorização CORS;
- `/users/me` sem token retorna `401` em `StandardError`;
- `/users/me` autenticado retorna usuário e role corretos;
- usuário `USER` acessa `/users/me` sem poder consultar outro usuário;
- endpoint protegido com role insuficiente retorna `403` em `StandardError`;
- corpo inválido retorna `400` com os campos inválidos;
- Swagger continua público e o contexto Spring inicia.

### Dashboard

- usuário não autenticado recebe `401`;
- `USER` e `ADMIN` conseguem consultar o resumo;
- os totais, contagens, valor e status são calculados corretamente;
- produtos empatados no status seguem a ordenação definida;
- lotes vencidos são contados corretamente;
- nenhum dado de senha ou entidade JPA aparece na resposta;
- os testes de integração usam PostgreSQL para validar agregações persistidas.

## Fora do escopo

- frontend React/Vite;
- histórico de movimentações;
- endpoints de detalhe de lote;
- relatórios e exportações;
- fornecedores;
- notificações em tempo real;
- RabbitMQ, WebSocket, Redis ou microserviços;
- alteração de migrations ou modelo de dados.

## Critério de conclusão

As Fases 1 e 2 estarão concluídas quando os contratos acima estiverem
implementados e documentados no OpenAPI, os testes direcionados e a suíte
completa passarem, e o frontend puder obter CORS, sessão, permissões, erros e
resumo do dashboard sem acessar o banco ou duplicar regras de estoque.
