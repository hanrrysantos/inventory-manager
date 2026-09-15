---
name: fix-findings
description: Use this when there are code review findings or a clearly identified code problem that needs to be investigated, corrected, and validated.
---

# Fix Findings

Corrija problemas de código já identificados com foco em causa raiz, mudança mínima e validação objetiva.

Esta Skill implementa correções. Ela não realiza code review amplo e não aprova o próprio trabalho.

## Entrada

Aceite como entrada:

- findings estruturados de code review;
- bugs ou problemas claramente descritos pelo usuário ou orquestrador.

Se a solicitação for vaga, como "corrija o código" ou "melhore tudo", não faça alterações indiscriminadas. Obtenha um problema ou escopo concreto antes de modificar o código.

## Processo obrigatório

Para cada problema ou grupo com a mesma causa raiz:

1. Leia o finding e o código relacionado.
2. Confirme que o problema realmente existe.
3. Identifique a causa raiz antes de editar.
4. Determine a menor correção segura e suficiente.
5. Quando fizer sentido, reproduza o problema com teste antes da correção.
6. Aplique a correção.
7. Execute os testes diretamente relacionados.
8. Amplie os testes somente quando o risco ou alcance justificar.
9. Avalie o resultado com evidências.
10. Reporte o status sem autoaprovar a mudança.

Não altere código antes de entender por que a alteração deve resolver o problema.

## Confirmação do finding

Findings não são verdades absolutas.

Confirme o problema usando código, testes, logs, contratos, documentação ou outras evidências disponíveis.

Se o finding não puder ser confirmado, não faça uma alteração apenas para satisfazer o reviewer.

Retorne `FINDING_NOT_CONFIRMED` e apresente a evidência que contradiz ou não sustenta o finding.

Se houver incerteza relevante, investigue antes de editar.

## Causa raiz

Não trate apenas sintomas quando a causa raiz puder ser identificada dentro do escopo.

Evite:

- adicionar condicionais apenas para esconder uma falha;
- capturar exceções sem tratar a causa;
- desabilitar testes;
- remover validações para fazer testes passarem;
- adicionar retries arbitrários;
- alterar expectativas de testes apenas para obter sucesso.

A correção deve explicar por que o problema deixa de ocorrer.

## Mudança mínima

Faça a menor mudança que resolva a causa raiz com segurança.

Não aproveite um finding para:

- refatorar módulos não relacionados;
- reorganizar arquitetura por preferência;
- renomear código fora do escopo;
- adicionar abstrações sem necessidade concreta;
- fazer limpeza geral do projeto.

Uma alteração maior é aceitável somente quando necessária para uma correção segura ou quando explicitamente solicitada.

## Limites de autonomia

Não faça silenciosamente:

- instalação de novas dependências;
- mudanças incompatíveis em APIs públicas;
- migrations destrutivas;
- alterações relevantes de schema;
- mudanças arquiteturais significativas.

Quando uma dessas ações for necessária, pare antes de executá-la, explique por que ela é necessária e solicite autorização.

Não substitua uma solução simples por nova infraestrutura sem justificativa concreta.

## Testes

Use testes proporcionalmente ao risco e ao tipo de problema.

Quando o problema for reproduzível e o teste agregar valor, prefira:

1. criar ou ajustar um teste que demonstre a falha;
2. executar e confirmar a falha;
3. aplicar a correção;
4. executar novamente e confirmar que passou.

Quando um teste de reprodução não for prático, use outra evidência concreta para confirmar o problema e execute os testes relevantes após a correção.

Nunca afirme que um teste passou se ele não foi executado.

Nunca altere ou remova um teste válido apenas para fazer a implementação passar.

## Falhas durante a correção

Se um teste falhar após a alteração, investigue a causa antes de editar novamente.

Continue iterando somente quando houver:

- hipótese concreta;
- evidência nova;
- progresso verificável.

Não entre em ciclos de tentativa e erro.

Interrompa e retorne `FIX_BLOCKED` quando:

- não houver hipótese concreta para continuar;
- a correção depender de informação indisponível;
- o problema real estiver fora do escopo;
- uma ação exigir autorização;
- continuar alterando código representar risco injustificado.

Inclua evidências do bloqueio.

## Múltiplos findings

Agrupe findings somente quando compartilharem a mesma causa raiz e puderem ser resolvidos e validados pela mesma alteração.

Trate findings independentes separadamente.

Não transforme vários findings pequenos em uma refatoração ampla apenas por conveniência.

## Status de saída

Use somente um destes status para cada problema ou grupo:

### `FIX_APPLIED`

Use quando:

- o problema foi confirmado;
- a causa raiz foi identificada;
- uma correção foi aplicada;
- as validações relevantes foram executadas.

`FIX_APPLIED` não significa que a mudança está aprovada.

### `FINDING_NOT_CONFIRMED`

Use quando a investigação não sustentar o finding.

Não altere o código apenas para eliminar o comentário do reviewer.

Apresente a evidência encontrada para que uma nova revisão possa reavaliar o finding.

### `FIX_BLOCKED`

Use quando não for possível produzir uma correção segura dentro do escopo ou autonomia disponível.

Explique exatamente o que bloqueou a correção e qual decisão ou informação é necessária para continuar.

## Formato da resposta

Para cada problema ou grupo, reporte:

### [STATUS] Título

**Finding:** problema tratado.

**Causa raiz:** causa confirmada ou, quando não confirmada, motivo da divergência.

**Alterações:** arquivos e comportamento modificados, ou `Nenhuma`.

**Validação:** testes, comandos ou evidências utilizados.

**Resultado:** consequência observada após a investigação/correção.

**Próximo passo:** nova execução de `code-review`, autorização necessária ou outra ação objetiva.

Se testes não foram executados, declare explicitamente o motivo.

## Integração com Code Review

Após `FIX_APPLIED`, a correção deve retornar para uma revisão independente.

O fluxo esperado é:

`code-review` → `fix-findings` → testes → `code-review`

A `fix-findings` nunca declara `APPROVED`, nunca decide que uma alteração pode fazer merge e nunca substitui a revisão independente.

Se o novo review encontrar outro problema, trate-o como um novo ciclo.

## Regra final

Não tente fazer o finding desaparecer.

Faça a causa do problema desaparecer.

Corrija somente o que puder justificar com evidência, valide o resultado e deixe a aprovação final para um reviewer independente.