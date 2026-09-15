# Repository Guidelines

## Projeto

Este repositório contém uma API REST em Java 21 com Spring Boot 3.

O código principal está em:

`src/main/java/br/com/hanrry/inventory`

Configurações e migrations Flyway ficam em:

`src/main/resources`

Os testes ficam em:

`src/test/java/br/com/hanrry/inventory`

O projeto está passando por uma evolução arquitetural incremental de uma
organização por camadas técnicas para uma organização por domínio.

O código existente continua sendo a fonte de verdade sobre o estado atual.
Não assuma que toda a arquitetura-alvo já está implementada.

## Documentação

Para decisões arquiteturais, limites dos módulos, responsabilidades e
restrições da evolução do projeto, consulte:

`docs/architecture.md`

Para tarefas de implementação planejadas, consulte:

`docs/plans/`

Antes de realizar mudanças estruturais, consulte o plano correspondente e
`docs/architecture.md`.

O plano define o escopo da etapa atual. Não antecipe decisões ou tarefas de
etapas posteriores.

## Comandos

- `bash ./mvnw clean test`: executa a suíte completa e gera o relatório JaCoCo.
- `bash ./mvnw package`: compila e empacota a aplicação em `target/`.
- `bash ./mvnw spring-boot:run`: inicia a API localmente na porta `8080`.
- `docker build -t inventory-manager .`: cria a imagem Docker multiestágio.
- `docker run --env-file .env -p 8080:8080 inventory-manager`: executa a imagem com as configurações locais.

O Maven Wrapper atualmente deve ser executado com `bash ./mvnw`.

O relatório JaCoCo fica em:

`target/site/jacoco/`

## Estilo e Convenções

Use quatro espaços de indentação.

- classes e records: `PascalCase`;
- métodos e variáveis: `camelCase`;
- constantes: `UPPER_SNAKE_CASE`.

Siga as convenções de Spring Boot, Spring Data JPA, Lombok e MapStruct já
adotadas pelo projeto.

Use DTOs e mappers para preservar os contratos da API quando apropriado.

Evite introduzir abstrações, dependências ou refatorações fora do escopo da
tarefa atual.

Durante a evolução arquitetural, siga os limites de domínio definidos em
`docs/architecture.md` e no plano correspondente.

Não reorganize módulos que pertençam a tarefas posteriores.

## Testes

O projeto utiliza JUnit 5, Mockito, MockMvc, JaCoCo e Testcontainers.

Organize os testes de forma coerente com os respectivos módulos do código de
produção.

Preserve os testes existentes e escolha o nível de teste adequado ao
comportamento alterado.

Quando uma tarefa for exclusivamente estrutural, preserve o comportamento
atual. Não altere regras de negócio ou código de produção apenas para fazer um
teste de caracterização passar.

Execute as validações definidas no plano antes de considerar uma tarefa
concluída.

## Workflow

Ao executar tarefas definidas em `docs/plans/`:

1. Execute somente a tarefa explicitamente autorizada.
2. Não antecipe tarefas posteriores.
3. Execute as validações definidas no plano.
4. Após implementação e testes, execute `code-review`.
5. Em caso de `CHANGES_REQUESTED`, não execute `fix-findings` sem autorização.
6. Não faça push nem avance para a próxima tarefa sem autorização.
7. Ao concluir, reporte alterações, testes, resultado do review e comportamentos inesperados.

As instruções detalhadas das Skills estão em:

`.agents/skills/`

## Commits e Pull Requests

Use mensagens de commit curtas, objetivas e em minúsculas, por exemplo:

`adicionando testes de integracao`

Mantenha commits e pull requests focados na tarefa executada.

Não misture alterações não relacionadas.

Pull requests devem explicar o problema e a solução, listar alterações
relevantes e informar como os testes foram executados.

Não faça push sem autorização durante a execução de um plano.

## Segurança e Configuração

Use variáveis de ambiente para banco de dados, e-mail, JWT e outras
configurações sensíveis.

Nunca versione `.env`, tokens, senhas, secrets ou credenciais reais.

Alterações no schema do banco devem ser feitas por novas migrations em:

`src/main/resources/db/migration`

Nunca edite migrations Flyway já aplicadas.