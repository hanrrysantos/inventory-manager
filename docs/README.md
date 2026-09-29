# Documentação do Inventory Manager (SDD)

Documentação de **produto e engenharia do sistema** (monorepo). Processo e
fontes de verdade: [`AGENTS.md`](../AGENTS.md).

```text
SPEC  →  PLAN  →  TASKS  →  implementação  →  testes  →  validação contra a spec
```

| Pasta | Responsabilidade |
| --- | --- |
| [`specs/`](specs/) | O que o sistema deve fazer |
| [`plans/`](plans/) | Como implementar cada mudança |
| [`tasks/`](tasks/) | Checklists executáveis |
| [`adr/`](adr/) | Decisões arquiteturais duradouras (por quê) |
| [`architecture/`](architecture/) | Visão estrutural e evolução do sistema |

Documentação **local** de cada app:

- [`../backend/docs/`](../backend/docs/) — API, persistência, execução do backend
- [`../frontend/docs/`](../frontend/docs/) — UI, integração no cliente

READMEs operacionais: [`../backend/README.md`](../backend/README.md),
[`../frontend/README.md`](../frontend/README.md).
