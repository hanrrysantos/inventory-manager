---
name: code-review
description: Use when reviewing code changes, pull requests, diffs, commits, or completed implementation work before approval, merge, or integration.
---

# Code Review

Realize revisão técnica orientada a risco. Priorize bugs concretos, regressões, segurança e comportamento incorreto. Não transforme preferências pessoais em findings.

## Escopo

Comece pelas alterações atuais (`git diff`, commit ou PR fornecido).

Expanda o contexto somente quando necessário para entender:
- código chamado ou afetado pela mudança;
- contratos, interfaces e modelos relacionados;
- testes existentes;
- configuração relevante.

Não revise o projeto inteiro sem necessidade.

Por padrão, permaneça **read-only**. Não modifique código durante o review. Só implemente correções quando o usuário ou orquestrador solicitar explicitamente uma fase de correção.

## Processo obrigatório

1. Entenda a intenção da mudança antes de julgá-la.
2. Leia o diff completo.
3. Expanda apenas o contexto necessário.
4. Procure primeiro problemas de correção e regressão.
5. Verifique segurança, persistência, concorrência, transações, performance e tratamento de erros quando aplicáveis.
6. Avalie se mudanças de comportamento possuem testes proporcionais ao risco.
7. Execute testes relevantes quando possível.
8. Produza somente findings sustentados por evidência concreta.
9. Termine com um parecer explícito.

Nunca afirme que um teste passou se ele não foi executado. Informe claramente testes não executados, falhos ou impossíveis de executar.

## Prioridades  

Procure principalmente por:

- comportamento incorreto e regressões;
- violação de regras de negócio;
- perda, corrupção ou inconsistência de dados;
- vulnerabilidades e falhas de autorização;
- problemas de concorrência e atomicidade;
- limites transacionais incorretos;
- chamadas externas que comprometam consistência;
- consultas ou operações com impacto relevante de performance;
- tratamento incorreto de erros e estados parciais;
- mudanças de comportamento sem testes adequados.

Não gere findings apenas por:
- preferência de estilo;
- naming aceitável;
- formatação;
- abstrações hipoteticamente melhores;
- possibilidade genérica de refatoração;
- código fora do escopo sem relação direta com a mudança.

Não proponha arquitetura adicional sem um problema concreto que a justifique.

## Java e Spring

Quando detectar Java/Spring, considere também:

- limites e propagação de `@Transactional`;
- I/O, e-mail, HTTP ou mensageria dentro de transações de banco;
- optimistic/pessimistic locking e condições de corrida;
- N+1, carregamento excessivo e consultas desnecessárias;
- uso incorreto de JPA/Hibernate;
- validação de entrada e Jakarta Validation;
- autorização em endpoints e serviços;
- exposição de entidades ou dados sensíveis;
- tratamento global de exceções e status HTTP;
- configuração, profiles e secrets;
- segurança e compatibilidade de migrations Flyway;
- cobertura de testes para regras de negócio e persistência.

Esses itens são heurísticas, não motivos automáticos para gerar findings.

## Severidade

Use somente:

**CRITICAL** — risco imediato de comprometimento grave, perda/corrupção significativa de dados ou falha crítica de segurança/negócio.

**HIGH** — bug relevante, regressão, falha de autorização, inconsistência, concorrência, transação incorreta ou outro problema com impacto substancial.

**MEDIUM** — problema real de impacto moderado em comportamento, confiabilidade, arquitetura, tratamento de erros, testes ou manutenção.

**LOW** — melhoria concreta de baixo impacto. Nunca use LOW para simples preferência pessoal.

Não infle severidades.

CRITICAL e HIGH sempre bloqueiam aprovação.

MEDIUM bloqueia quando o impacto torna inseguro integrar a mudança sem correção.

LOW nunca bloqueia sozinho.

## Evidência obrigatória

Todo finding deve demonstrar:

- severidade;
- arquivo e localização;
- problema concreto;
- impacto;
- evidência observável no código;
- recomendação objetiva.

Se não houver evidência suficiente, investigue mais. Se ainda houver incerteza, apresente como questão ou ponto a verificar, não como finding confirmado.

Use este formato:

### [HIGH] Título objetivo

**Arquivo:** `src/.../Arquivo.java`  
**Local:** `metodo()` ou linhas relevantes

**Problema:** descrição objetiva.

**Impacto:** consequência concreta.

**Evidência:** o que no código sustenta a conclusão.

**Recomendação:** correção ou direção mínima necessária.

## Testes

A exigência de testes deve ser proporcional ao risco.

Uma alteração crítica de regra de negócio sem cobertura pode ser HIGH. Uma mudança de baixo risco pode não exigir teste adicional.

Quando possível, execute primeiro os testes diretamente relacionados à mudança. Amplie para uma suíte maior somente quando o risco ou alcance justificar.

Falha em teste relevante é evidência importante, mas investigue se a falha realmente foi causada pela mudança antes de gerar finding.

## Parecer final

Após os findings, termine com:

### Parecer

**Status:** `APPROVED`, `APPROVED_WITH_NOTES` ou `CHANGES_REQUESTED`

**Findings:** quantidade por severidade.

**Bloqueadores:** findings que impedem integração, ou `Nenhum`.

**Testes:** comandos executados e resultado; se nenhum foi executado, declare isso.

**Pode fazer merge:** `Sim` ou `Não`.

Use `APPROVED` quando não houver problemas relevantes.

Use `APPROVED_WITH_NOTES` quando houver apenas observações não bloqueantes.

Use `CHANGES_REQUESTED` quando existir qualquer finding bloqueante.

## Regra final

Um review sem findings é um resultado válido.

Nunca invente problemas para parecer minucioso.

O objetivo não é produzir comentários. O objetivo é decidir, com evidência, se a mudança pode ser integrada com segurança.