# Repository Guidelines

## Projeto

Este monorepo contém uma API REST em Java 21 com Spring Boot 3 e um frontend
em React, TypeScript e Vite.

O backend fica em:

`backend/`

O frontend fica em:

`frontend/`

O código principal do backend está em:

`backend/src/main/java/br/com/hanrry/inventory`

Configurações e migrations Flyway ficam em:

`backend/src/main/resources`

Os testes do backend ficam em:

`backend/src/test/java/br/com/hanrry/inventory`

O projeto está passando por uma evolução arquitetural incremental de uma
organização por camadas técnicas para uma organização por domínio.

O código existente é a fonte de verdade sobre o **estado atual da implementação**.

As especificações em `docs/specs/` são a fonte de verdade sobre o
**comportamento esperado das funcionalidades documentadas**.

Não assuma que toda a arquitetura-alvo, todas as especificações ou todos os
planos já estão implementados.

Quando houver divergência entre código, testes, plano, arquitetura e
especificação, não altere silenciosamente a documentação para refletir o
comportamento atual.

Identifique e reporte a divergência.

---

## Documentação

A documentação SDD do **sistema** (monorepo) fica em:

`docs/`

```text
docs/
├── specs/
├── plans/
├── tasks/
├── adr/
└── architecture/
```

Documentação **específica** de cada app:

- `backend/docs/` — API, persistência, execução local, OpenAPI;
- `frontend/docs/` — UI, padrões de cliente, build Vite.

Índice: [`docs/README.md`](docs/README.md).

### `docs/specs/`

Contém as especificações das funcionalidades e comportamentos do sistema.

As specs definem principalmente **o que o sistema deve fazer**.

Uma spec pode conter:

- contexto da funcionalidade;
- objetivo;
- regras de negócio;
- comportamentos esperados;
- entradas e saídas;
- cenários de erro;
- casos extremos;
- requisitos não funcionais relevantes;
- critérios de aceitação.

Specs devem evitar detalhes de implementação quando esses detalhes não forem
parte do requisito.

Para funcionalidades documentadas, a spec é a fonte de verdade sobre o
comportamento esperado.

Não altere uma spec durante a implementação sem autorização explícita.

Não adapte silenciosamente uma spec para fazer o comportamento atual do código
parecer correto.

### `docs/plans/`

Contém os planos técnicos de implementação derivados das specs, da arquitetura
e do estado atual do projeto.

Os plans definem principalmente **como uma mudança será implementada**.

Um plan pode conter:

- análise do estado atual;
- componentes afetados;
- classes e módulos envolvidos;
- estratégia técnica;
- alterações de persistência;
- migrations necessárias;
- transações;
- concorrência;
- integrações;
- estratégia de testes;
- riscos técnicos;
- sequência de implementação;
- validações necessárias.

O plano define o escopo da etapa atual.

Não antecipe decisões, refatorações ou tarefas pertencentes a etapas
posteriores.

Antes de implementar um plano, considere sempre o estado atual do código.

### `docs/tasks/`

Contém checklists executáveis derivados dos planos.

As tasks representam **os passos concretos necessários para executar um
plano**.

Cada tarefa deve ser:

- pequena;
- objetiva;
- ordenada;
- verificável;
- limitada ao escopo definido pelo plano.

Quando apropriado, uma task deve incluir:

- implementação;
- testes;
- validação;
- revisão.

Não marque uma tarefa como concluída antes de executar as validações
correspondentes.

Não avance automaticamente para a próxima tarefa sem autorização quando o
workflow atual exigir execução individual por etapa.

### `docs/adr/`

Contém Architecture Decision Records.

ADRs registram **decisões arquiteturais importantes e duradouras**.

Um ADR pode documentar:

- contexto;
- problema;
- decisão tomada;
- alternativas consideradas;
- motivos da decisão;
- consequências positivas;
- consequências negativas;
- impactos futuros.

Use ADR quando uma decisão:

- afetar múltiplos módulos;
- tiver impacto arquitetural relevante;
- introduzir uma tecnologia ou padrão importante;
- definir uma restrição duradoura;
- precisar permanecer registrada como parte do histórico arquitetural.

Não use ADR para detalhes pequenos ou temporários de implementação que
pertencem apenas a um plano específico.

ADRs existentes devem ser considerados decisões já tomadas, salvo quando uma
tarefa explicitamente autorizar sua revisão ou substituição.

### `docs/architecture/`

Contém a visão consolidada da arquitetura do **sistema**.

Consulte [`docs/architecture/overview.md`](docs/architecture/overview.md) e o
índice em [`docs/architecture/README.md`](docs/architecture/README.md) para entender:

- limites entre módulos;
- responsabilidades;
- dependências permitidas;
- organização por domínio;
- direção das dependências;
- restrições arquiteturais;
- estratégia de evolução incremental.

Antes de realizar mudanças estruturais, consulte:

1. a spec correspondente, quando existir;
2. o plano correspondente;
3. `docs/architecture/overview.md`;
4. ADRs relacionados, quando existirem.

Não utilize a arquitetura-alvo como justificativa para antecipar refatorações
que ainda não fazem parte do plano atual.

---

## Fluxo SDD

Para mudanças orientadas por especificação, siga o fluxo:

```text
SPEC
  ↓
PLAN
  ↓
TASKS
  ↓
IMPLEMENTAÇÃO
  ↓
TESTES
  ↓
CODE REVIEW
  ↓
VALIDAÇÃO CONTRA A SPEC
```

Cada artefato possui uma responsabilidade diferente:

- `spec` define **o que** deve acontecer;
- `plan` define **como** a mudança será implementada;
- `tasks` define **quais passos** devem ser executados;
- `adr` registra **por que** uma decisão arquitetural relevante foi tomada;
- `docs/architecture/` descreve **como a arquitetura do sistema está organizada**.

Sempre que possível, mantenha correspondência entre:

```text
docs/specs/<feature>.md
docs/plans/<feature>.md
docs/tasks/<feature>.md
```

Nem toda alteração precisa obrigatoriamente de spec, plan e tasks.

Correções pequenas, manutenção simples ou alterações estritamente locais podem
ser executadas sem criar documentação artificial, desde que não envolvam:

- novas regras de negócio;
- mudanças relevantes de comportamento;
- mudanças arquiteturais;
- alterações de contrato;
- concorrência;
- transações complexas;
- integrações relevantes;
- funcionalidades novas ou significativamente alteradas.

---

## Prioridade das Fontes

Ao analisar uma tarefa, diferencie estado atual de comportamento esperado.

Use a seguinte interpretação:

```text
código
→ fonte de verdade sobre o estado atual da implementação

spec
→ fonte de verdade sobre o comportamento esperado

plan
→ estratégia aprovada para implementar a mudança

tasks
→ sequência executável do plano

docs/architecture/
→ limites e restrições arquiteturais

ADR
→ decisões arquiteturais já registradas
```

Não trate código existente como prova de que uma spec está errada.

Não trate uma spec futura como prova de que o código atual já deveria estar
organizado dessa forma.

Quando houver conflito, reporte a divergência.

---

## Comandos

- `cd backend && bash ./mvnw clean test`: executa a suíte completa e gera o
  relatório JaCoCo.
- `cd backend && bash ./mvnw package`: compila e empacota a aplicação.
- `cd backend && bash ./mvnw spring-boot:run`: inicia a API localmente na porta
  `8080`.
- `docker build -t inventory-manager ./backend`: cria a imagem Docker
  multiestágio.
- `cd backend && docker compose up -d --build`: inicia a API e o PostgreSQL.

O Maven Wrapper atualmente deve ser executado dentro de `backend/` com:

```bash
bash ./mvnw
```

O relatório JaCoCo fica em:

`backend/target/site/jacoco/`

---

## Estilo e Convenções

Use quatro espaços de indentação.

Convenções de nomenclatura:

- classes e records: `PascalCase`;
- métodos e variáveis: `camelCase`;
- constantes: `UPPER_SNAKE_CASE`.

Siga as convenções de Spring Boot, Spring Data JPA, Lombok e MapStruct já
adotadas pelo projeto.

Use DTOs e mappers para preservar os contratos da API quando apropriado.

Evite introduzir:

- abstrações desnecessárias;
- novas dependências sem justificativa;
- refatorações fora do escopo;
- novos padrões arquiteturais sem necessidade;
- alterações não relacionadas à tarefa atual.

Durante a evolução arquitetural, siga os limites de domínio definidos em:

`docs/architecture/overview.md`

e no plano correspondente.

Não reorganize módulos que pertençam a tarefas posteriores.

Prefira mudanças incrementais compatíveis com a arquitetura existente e com o
estado atual da migração.

---

## Testes

O projeto utiliza:

- JUnit 5;
- Mockito;
- MockMvc;
- JaCoCo;
- Testcontainers.

Organize os testes de forma coerente com os respectivos módulos do código de
produção.

Preserve os testes existentes.

Escolha o nível de teste adequado ao comportamento alterado.

Quando apropriado, utilize:

- testes unitários para regras isoladas;
- testes de integração para persistência e colaboração entre componentes;
- MockMvc para contratos HTTP;
- Testcontainers para comportamentos dependentes do PostgreSQL;
- testes de concorrência para comportamentos que dependam de sincronização,
  locking ou isolamento transacional.

Quando uma tarefa for exclusivamente estrutural, preserve o comportamento
atual.

Não altere regras de negócio ou código de produção apenas para fazer um teste
de caracterização passar.

Critérios de aceitação presentes em uma spec devem possuir validação adequada
quando forem implementados.

Execute as validações definidas no plano antes de considerar uma tarefa
concluída.

---

## Workflow

Ao executar uma feature ou mudança documentada:

1. Leia a spec correspondente em `docs/specs/`, quando existir.
2. Leia o plano correspondente em `docs/plans/`.
3. Leia as tasks correspondentes em `docs/tasks/`, quando existirem.
4. Consulte `docs/architecture/overview.md` para mudanças estruturais.
5. Consulte ADRs relacionados quando a mudança envolver decisões arquiteturais
   existentes.
6. Analise o código atual antes de implementar mudanças.
7. Execute somente a tarefa explicitamente autorizada.
8. Não antecipe tarefas posteriores.
9. Não introduza alterações fora do escopo definido.
10. Não altere a spec sem autorização explícita.
11. Execute os testes e validações definidos no plano.
12. Após implementação e testes, execute `code-review`.
13. Em caso de `CHANGES_REQUESTED`, não execute `fix-findings` sem autorização.
14. Valide a implementação contra os critérios de aceitação da spec.
15. Não faça push sem autorização.
16. Não avance para a próxima tarefa sem autorização.
17. Ao concluir, reporte:
    - alterações realizadas;
    - arquivos relevantes modificados;
    - testes executados;
    - resultado dos testes;
    - resultado do code review;
    - critérios da spec validados;
    - divergências encontradas;
    - comportamentos inesperados.

Se código, testes, plano, arquitetura e spec divergirem, reporte a
inconsistência.

Não altere silenciosamente requisitos, specs, ADRs ou arquitetura para fazer a
implementação atual parecer correta.

As instruções detalhadas das Skills estão em:

`.agents/skills/`

---

## Branches

Use nomes de branches curtos, descritivos e coerentes com Conventional Commits.

O formato preferencial é:

```text
tipo/descricao-curta
```

Use `kebab-case` na descrição.

Exemplos:

```text
feat/inventory-low-stock-alert
fix/batch-expired-consumption
test/inventory-concurrency
refactor/product-domain
docs/sdd-workflow
chore/update-dependencies
build/docker-image
ci/backend-tests
perf/inventory-consumption
```

Tipos preferenciais:

- `feat`: nova funcionalidade;
- `fix`: correção de bug;
- `test`: criação ou alteração relevante de testes;
- `refactor`: refatoração sem mudança intencional de comportamento;
- `docs`: documentação;
- `chore`: manutenção;
- `build`: build ou dependências;
- `ci`: integração contínua;
- `perf`: melhoria de desempenho.

A descrição da branch deve identificar claramente o objetivo principal da
mudança.

Evite nomes genéricos como:

```text
feature/new-feature
fix/bug
update/project
dev/test
changes
```

Prefira:

```text
feat/inventory-low-stock-alert
fix/auth-expired-token
refactor/inventory-domain
docs/inventory-spec
```

Uma branch deve representar uma unidade coerente de trabalho.

Não misture features ou correções não relacionadas na mesma branch.

Quando uma mudança estiver associada a uma spec, plan ou task, use um nome de
branch relacionado à funcionalidade documentada.

Não crie, troque ou publique branches sem autorização quando estiver executando
um plano que exija aprovação por etapa.

---

## Commits

Use Conventional Commits.

O formato padrão é:

```text
tipo(escopo): descrição curta
```

Exemplos:

```text
feat(inventory): adiciona consumo de estoque por fefo
fix(batch): impede consumo de lotes expirados
test(inventory): adiciona teste de concorrência no consumo
refactor(product): reorganiza domínio de produtos
docs(sdd): adiciona especificação de controle de estoque
chore(deps): atualiza dependências do backend
build(docker): ajusta imagem multiestagio
ci(github): adiciona validacao da suite de testes
perf(inventory): reduz consultas durante consumo de estoque
```

Tipos preferenciais:

- `feat`: nova funcionalidade;
- `fix`: correção de bug;
- `test`: criação ou alteração de testes;
- `refactor`: refatoração sem mudança intencional de comportamento;
- `docs`: documentação;
- `chore`: tarefas de manutenção;
- `build`: alterações no processo de build ou dependências;
- `ci`: alterações de integração contínua;
- `perf`: melhoria de desempenho;
- `style`: alterações de formatação sem impacto em comportamento.

O `escopo` deve identificar de forma curta a área afetada.

Exemplos de escopo:

```text
inventory
batch
product
auth
user
email
database
migration
docker
frontend
docs
sdd
architecture
```

A descrição deve:

- ser curta;
- ser objetiva;
- usar letras minúsculas;
- descrever a principal alteração;
- não terminar com ponto.

Evite mensagens genéricas como:

```text
fix: ajustes
chore: mudancas
feat: melhorias
```

Prefira:

```text
fix(inventory): corrige calculo de estoque disponivel
test(batch): cobre ordenacao fefo com mesma validade
refactor(auth): isola validacao de token jwt
```

Cada commit deve representar uma unidade lógica de mudança.

Não misture alterações não relacionadas no mesmo commit.

Nunca use coautoria em commits: não inclua trailers `Co-authored-by:`,
`Co-Authored-By:` nem equivalentes na mensagem de commit.

Não faça commit ou push sem autorização quando estiver executando um plano que
exija aprovação por etapa.

---

## Pull Requests

Mantenha pull requests focados na tarefa executada.

Pull requests devem explicar:

- o problema;
- a solução adotada;
- as principais alterações;
- impactos relevantes;
- como os testes foram executados;
- resultado das validações.

Quando a mudança estiver associada a uma spec ou plan, referencie os documentos
correspondentes.

Não misture alterações não relacionadas no mesmo pull request.

Nunca use coautoria em pull requests: não adicione coautores no GitHub nem
sugira trailers de coautoria nos commits incluídos no PR.

Não faça push sem autorização durante a execução de um plano.

---

## Segurança e Configuração

Use variáveis de ambiente para:

- banco de dados;
- e-mail;
- JWT;
- APIs externas;
- tokens;
- outras configurações sensíveis.

Nunca versione:

- `.env`;
- tokens;
- senhas;
- secrets;
- credenciais reais;
- chaves privadas.

Alterações no schema do banco devem ser feitas por novas migrations em:

`backend/src/main/resources/db/migration`

Nunca edite migrations Flyway já aplicadas.

Nunca inclua secrets reais em:

- código;
- testes;
- documentação;
- exemplos;
- logs;
- arquivos de configuração versionados.