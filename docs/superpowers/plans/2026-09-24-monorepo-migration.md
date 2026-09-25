# Monorepo Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reunir backend e frontend no repositório `inventory-manager`, em diretórios próprios, preservando integralmente os dois históricos Git.

**Architecture:** O repositório atual continua sendo a origem do monorepo. O backend é reposicionado com `git mv`; o frontend é anexado com `git subtree` sem `--squash`. Metadados compartilhados e workflows permanecem na raiz, enquanto configurações específicas acompanham cada aplicação.

**Tech Stack:** Git, Git subtree, Java 21, Maven Wrapper, Spring Boot 3, Node.js 22.12+/24, npm, React, TypeScript, Vite, Docker Compose e GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-24-monorepo-migration-design.md`

## Global Constraints

- Usar o repositório `inventory-manager` como destino; não criar outro repositório.
- Preservar hashes, autores, datas e mensagens dos 59 commits atuais do frontend.
- Importar o frontend sem `--squash` a partir de `hanrrysantos/inventory-manager-frontend`, branch `main`, commit `96de71d40bcdd5e4fa20d0342a55ec4ed929dab7`.
- Não alterar regras de negócio, contratos da API, plataformas de deploy, domínios ou ciclos de release.
- Não adicionar gerenciador de monorepo, dependência ou abstração de workspace.
- Manter `.github/`, `.agents/`, `AGENTS.md`, `.gitignore` e `.gitattributes` na raiz.
- No CI, alterar somente o diretório de execução para `backend/`; preservar nome, gatilhos e etapas existentes.
- Manter segredos e arquivos `.env` fora do histórico.
- Não fazer push.
- Após implementação e testes, executar `code-review`; em caso de `CHANGES_REQUESTED`, não executar `fix-findings` sem autorização.

## Review Focus

- Histórico do frontend: o commit `96de71d` deve ser ancestral de `HEAD` e sua linha histórica deve continuar com 59 commits.
- Histórico do backend: o commit anterior à migração deve permanecer ancestral de `HEAD`, com renomes detectáveis pelo Git.
- Segredos locais: `backend/.env` e `frontend/.env` devem continuar ignorados e nenhum `.env` deve estar rastreado.
- Caminhos de automação: Maven e Docker devem executar em `backend/`, sem alterar nome, gatilhos ou etapas do workflow.
- Documentação: nenhum README deve instruir o usuário a clonar o antigo repositório separado do frontend.

---

### Task 1: Reposicionar o backend e criar a raiz do monorepo

**Files:**
- Move: `.dockerignore` → `backend/.dockerignore`
- Move: `.env.example` → `backend/.env.example`
- Move: `.mvn/` → `backend/.mvn/`
- Move: `Dockerfile` → `backend/Dockerfile`
- Move: `README.md` → `backend/README.md`
- Move: `docker-compose.yml` → `backend/docker-compose.yml`
- Move: `mvnw` → `backend/mvnw`
- Move: `mvnw.cmd` → `backend/mvnw.cmd`
- Move: `pom.xml` → `backend/pom.xml`
- Move: `scripts/` → `backend/scripts/`
- Move: `src/` → `backend/src/`
- Move: documentação específica de `docs/` → `backend/docs/`
- Create: `README.md`
- Modify: `.github/workflows/maven.yml`
- Modify: `.gitattributes`
- Modify: `AGENTS.md`
- Modify: `backend/README.md`

**Interfaces:**
- Consumes: repositório backend limpo em `main`, commit `0d9615526a08b317c729e414fd82f3fd8d54db01`.
- Produces: backend autocontido em `backend/`, metadados compartilhados na raiz e CI funcional a partir do novo caminho.

- [ ] **Step 1: Registrar o estado inicial e confirmar a árvore limpa**

Run:

```bash
git status --short
git rev-parse HEAD
git rev-parse origin/main
git ls-files .env backend/.env frontend/.env
```

Expected: `git status --short` e `git ls-files` não imprimem nada; `HEAD` é `0d96155`; `origin/main` pode estar um commit atrás até o usuário enviar a especificação.

- [ ] **Step 2: Executar a validação de base do backend**

Run:

```bash
bash ./mvnw clean test
docker compose config --quiet
```

Expected: Maven termina com `BUILD SUCCESS`; Docker Compose termina com código 0.

- [ ] **Step 3: Mover os arquivos rastreados do backend**

Run:

```bash
mkdir -p backend
git mv .dockerignore .env.example .mvn Dockerfile README.md docker-compose.yml mvnw mvnw.cmd pom.xml scripts src backend/
git mv docs backend/docs
mkdir -p docs/superpowers/specs docs/superpowers/plans
git mv backend/docs/superpowers/specs/2026-09-24-monorepo-migration-design.md docs/superpowers/specs/
git mv backend/docs/superpowers/plans/2026-09-24-monorepo-migration.md docs/superpowers/plans/
```

Expected: arquivos específicos da API ficam sob `backend/`; somente os documentos desta migração retornam para `docs/superpowers/` na raiz.

- [ ] **Step 4: Reposicionar a configuração local sem versioná-la**

Run:

```bash
if [ -f .env ]; then mv .env backend/.env; fi
git check-ignore -v backend/.env
git ls-files backend/.env
```

Expected: `git check-ignore` aponta uma regra de `.gitignore`; `git ls-files` não imprime nada.

- [ ] **Step 5: Criar o README da raiz**

Create `README.md` with this structure and links:

````markdown
# Inventory Manager

Aplicação full stack para controle de estoque por lotes, com consumo FEFO,
rastreabilidade de movimentações e proteção contra operações concorrentes.

## Projetos

| Projeto | Tecnologias | Documentação |
| --- | --- | --- |
| Backend | Java 21, Spring Boot 3, PostgreSQL | [backend/README.md](backend/README.md) |
| Frontend | React, TypeScript, Vite | [frontend/README.md](frontend/README.md) |

## Executar localmente

```bash
git clone https://github.com/hanrrysantos/inventory-manager.git
cd inventory-manager
```

Consulte o README de cada projeto para instalar dependências, configurar as
variáveis de ambiente e iniciar as aplicações.

## Aplicação publicada

- Frontend: https://controle-de-estoque.hanrry.top
- API: https://api-controle-de-estoque.hanrry.top/swagger-ui/index.html
````

Expected: a raiz explica o conjunto sem duplicar instruções específicas dos dois projetos.

- [ ] **Step 6: Atualizar caminhos compartilhados**

Modify `.gitattributes`:

```gitattributes
/backend/mvnw text eol=lf
*.cmd text eol=crlf
```

Modify `AGENTS.md` so every backend path and command is rooted at `backend/`:

```text
backend/src/main/java/br/com/hanrry/inventory
backend/src/main/resources
backend/src/test/java/br/com/hanrry/inventory
backend/docs/architecture.md
backend/docs/plans/
cd backend && bash ./mvnw clean test
cd backend && bash ./mvnw package
cd backend && bash ./mvnw spring-boot:run
docker build -t inventory-manager ./backend
cd backend && docker compose up -d --build
```

Also describe `frontend/` as the React/TypeScript/Vite application and keep `.agents/skills/` rooted at the repository root.

Modify `backend/README.md`:

- change clone/setup commands to `git clone .../inventory-manager.git`, `cd inventory-manager/backend`;
- replace the external frontend repository link with `../frontend`;
- keep backend-relative links such as `src/` and `docs/` unchanged.

- [ ] **Step 7: Adaptar o workflow do backend**

Replace `.github/workflows/maven.yml` with:

```yaml
name: CI - Build and Tests

on:
  push:
    branches: [ "main" ]
  pull_request:
    branches: [ "main" ]
  workflow_dispatch:

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          distribution: "temurin"
          java-version: "21"

      - name: Give permission to Maven Wrapper
        run: chmod +x mvnw

      - name: Run tests with JaCoCo
        run: ./mvnw clean test

      - name: Build Docker image
        run: docker build -t inventory-manager .
```

- [ ] **Step 8: Validar o backend no novo caminho**

Run:

```bash
cd backend
bash ./mvnw clean test
docker compose config --quiet
docker build -t inventory-manager .
```

Expected: Maven mostra `BUILD SUCCESS`; Compose e Docker terminam com código 0.

Run from the repository root:

```bash
git diff --check
git status --short
git diff --summary
```

Expected: sem erros de whitespace; o resumo identifica renomes em vez de exclusões e recriações do backend.

- [ ] **Step 9: Commitar a nova estrutura do backend**

```bash
git add .gitattributes .github/workflows/maven.yml AGENTS.md README.md backend docs
git commit -m "chore(repo): move backend para diretorio dedicado"
```

Expected: árvore limpa, exceto arquivos locais ignorados.

---

### Task 2: Importar o frontend preservando o histórico

**Files:**
- Create by subtree: `frontend/`
- Modify: `frontend/README.md`

**Interfaces:**
- Consumes: raiz limpa e backend disponível em `backend/`.
- Produces: frontend completo em `frontend/`, com o commit remoto `96de71d` ligado ao grafo do monorepo.

- [ ] **Step 1: Confirmar a origem exata antes da importação**

Run:

```bash
git status --short
git ls-remote --symref git@github.com:hanrrysantos/inventory-manager-frontend.git HEAD
```

Expected: árvore limpa; `HEAD` remoto aponta para `refs/heads/main` e `96de71d40bcdd5e4fa20d0342a55ec4ed929dab7`.

- [ ] **Step 2: Importar a branch sem squash**

Run:

```bash
git subtree add --prefix=frontend git@github.com:hanrrysantos/inventory-manager-frontend.git main -m "chore(repo): importa frontend com historico"
```

Expected: `frontend/package.json`, `frontend/src/`, `frontend/README.md` e `frontend/vercel.json` existem; um commit de integração conecta os históricos.

- [ ] **Step 3: Verificar imediatamente o histórico importado**

Run:

```bash
git merge-base --is-ancestor 96de71d40bcdd5e4fa20d0342a55ec4ed929dab7 HEAD
git rev-list --count 96de71d40bcdd5e4fa20d0342a55ec4ed929dab7
git show -s --format='%H%n%an%n%ad%n%s' 96de71d40bcdd5e4fa20d0342a55ec4ed929dab7
```

Expected: ancestry termina com código 0; contagem é `59`; o último comando mostra o commit original do frontend.

- [ ] **Step 4: Atualizar as instruções do frontend para o monorepo**

Modify `frontend/README.md`:

- replace “Frontend e API são mantidos em repositórios separados” with “Frontend e API são mantidos no mesmo monorepo”;
- replace the backend repository URL with `[backend](../backend)`;
- use these local setup commands:

```bash
git clone https://github.com/hanrrysantos/inventory-manager.git
cd inventory-manager/frontend
npm ci
cp .env.example .env
npm run dev
```

- keep deployment configuration and runtime behavior unchanged.

- [ ] **Step 5: Instalar e validar o frontend no novo caminho**

Run:

```bash
cd frontend
npm ci
npm run lint
npm test
npm run build
```

Expected: instalação termina sem alterar `package-lock.json`; lint, testes e build terminam com código 0.

- [ ] **Step 6: Commitar somente a documentação pós-importação**

```bash
git add frontend/README.md
git diff --cached --check
git commit -m "docs(frontend): atualiza instrucoes para monorepo"
```

Expected: o commit contém apenas `frontend/README.md`.

---

### Task 3: Validar e revisar o monorepo completo

**Files:**
- Verify: repository tree and Git graph
- Verify: `backend/`
- Verify: `frontend/`
- Review: all migration commits

**Interfaces:**
- Consumes: backend reposicionado e frontend importado.
- Produces: evidência de históricos preservados, builds funcionais e árvore segura para push pelo usuário.

- [ ] **Step 1: Validar estrutura, documentação e arquivos sensíveis**

Run:

```bash
test -f README.md
test -f backend/pom.xml
test -f backend/README.md
test -f frontend/package.json
test -f frontend/README.md
test -f .github/workflows/maven.yml
git check-ignore -q backend/.env
! git ls-files | rg '(^|/)\.env$'
! rg -n 'inventory-manager-frontend\.git' README.md backend/README.md frontend/README.md
git diff --check
```

Expected: todos os comandos terminam com código 0 e não imprimem segredos ou referências de clone ao repositório separado.

- [ ] **Step 2: Validar ambos os históricos**

Run:

```bash
git merge-base --is-ancestor 0d9615526a08b317c729e414fd82f3fd8d54db01 HEAD
git merge-base --is-ancestor 96de71d40bcdd5e4fa20d0342a55ec4ed929dab7 HEAD
test "$(git rev-list --count 96de71d40bcdd5e4fa20d0342a55ec4ed929dab7)" = "59"
git log --graph --oneline --decorate -20
```

Expected: ambos os testes de ancestralidade e a contagem terminam com código 0; o grafo mostra as duas linhas históricas conectadas.

- [ ] **Step 3: Reexecutar as validações completas**

Run:

```bash
cd backend && bash ./mvnw clean test
cd ../frontend && npm run lint && npm test && npm run build
cd .. && docker compose -f backend/docker-compose.yml config --quiet
docker build -t inventory-manager ./backend
```

Expected: backend mostra `BUILD SUCCESS`; frontend e Docker terminam com código 0.

- [ ] **Step 4: Conferir o estado final antes do review**

Run:

```bash
git status --branch --short
git diff --check
git log --format='%h %s%n%b' origin/main..HEAD
```

Expected: árvore limpa, branch apenas à frente de `origin/main`, commits focados e nenhum trailer `Co-authored-by`.

- [ ] **Step 5: Executar code-review**

Invoke the repository `code-review` skill against `origin/main..HEAD`, focusing on lost files, broken relative paths, ignored secrets, workflow paths and preservation of both histories.

Expected: report `APPROVED` or `CHANGES_REQUESTED`. If `CHANGES_REQUESTED`, report findings and stop without invoking `fix-findings` until the user authorizes it.

- [ ] **Step 6: Entregar para o push do usuário**

Report:

- commits created and their purposes;
- backend test count/result;
- frontend lint/test/build result;
- Docker validation result;
- frontend history ancestry and 59-commit count;
- code-review result;
- confirmation that no push was performed.
