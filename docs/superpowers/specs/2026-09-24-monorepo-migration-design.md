# Migração para monorepo

## Objetivo

Reunir backend e frontend no repositório `inventory-manager`, usando o atual
repositório do backend como destino e preservando o histórico completo dos dois
projetos.

O resultado deve permitir abrir e modificar toda a aplicação em uma única IDE,
sem alterar o comportamento das aplicações nem antecipar a reorganização dos
deploys.

## Estrutura final

```text
inventory-manager/
├── README.md
├── .github/
├── .gitignore
├── .gitattributes
├── AGENTS.md
├── docs/
│   └── superpowers/
├── backend/
│   ├── README.md
│   ├── pom.xml
│   ├── mvnw
│   ├── src/
│   ├── docs/
│   ├── scripts/
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── .env.example
└── frontend/
    ├── README.md
    ├── package.json
    ├── src/
    ├── public/
    └── vercel.json
```

Arquivos de orientação do repositório e automações do GitHub permanecem na
raiz. Documentação específica do backend fica em `backend/docs`; a
especificação da migração permanece em `docs/superpowers` por tratar do
repositório inteiro.

## Preservação do histórico

O backend será movido com `git mv`, preservando seu histórico no repositório
atual.

O frontend será importado da branch `main` do repositório
`hanrrysantos/inventory-manager-frontend` por `git subtree`, sem a opção
`--squash`. Assim, os 59 commits existentes continuam alcançáveis com seus
hashes, autores, datas e mensagens originais.

A importação produzirá um commit de integração conectando os dois históricos.
O repositório original do frontend não será removido nem arquivado nesta etapa.

## Arquivos locais e ignorados

O `.env` local do backend será movido para `backend/.env` sem ser versionado.
Arquivos gerados, incluindo `target/`, continuarão ignorados e não serão
movidos para o histórico.

O `.gitignore` da raiz cobrirá artefatos e configurações locais dos dois
projetos. O `.gitignore` importado do frontend poderá permanecer em
`frontend/.gitignore` quando contiver regras específicas do projeto.

## Automação

O workflow Maven permanecerá em `.github/workflows`, pois o GitHub somente
descobre workflows nesse caminho na raiz. Seus comandos, cache e filtros de
caminho serão ajustados para `backend/`.

Os arquivos Docker do backend serão movidos com a aplicação e continuarão
operando a partir do diretório `backend/`.

A configuração atual da Vercel será importada junto com o frontend. O frontend
não possui workflow próprio no GitHub. Mudanças nas plataformas de deploy,
domínios ou serviços ficam fora do escopo desta etapa.

## README raiz

O novo `README.md` da raiz apresentará a aplicação completa, indicará as
tecnologias de cada parte e direcionará para `backend/README.md` e
`frontend/README.md`. Instruções detalhadas permanecem nos READMEs de cada
projeto.

## Validação

Antes da conclusão serão executados:

- verificação de que os dois históricos estão alcançáveis;
- validação do workflow e dos caminhos alterados;
- suíte do backend com `bash ./mvnw clean test`, em `backend/`;
- build do frontend com `npm ci` e `npm run build`, em `frontend/`;
- `git diff --check` e confirmação de que arquivos sensíveis não foram
  versionados.

## Fora do escopo

- alterar Vercel, Render, Supabase, Resend ou domínios;
- criar uma ferramenta de workspace ou gerenciador de monorepo;
- unificar os ciclos de release das aplicações;
- remover ou arquivar o repositório original do frontend;
- alterar regras de negócio ou contratos da API.
