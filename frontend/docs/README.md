# Documentação específica do frontend

Guia e referências **locais** do EstoqueHub (React/Vite). Specs, planos e
arquitetura do sistema ficam em [`../../docs/`](../../docs/).

## O que documentar aqui

- padrões de componentes, rotas e estado (TanStack Query, formulários);
- decisões de UX/a11y específicas da UI;
- notas sobre variáveis `VITE_*` e build na Vercel;
- planos de tela quando o escopo for **apenas frontend**, referenciando a spec
  em [`../../docs/specs/`](../../docs/specs/).

## O que não duplicar aqui

- contratos HTTP e regras de negócio → [`../../docs/specs/`](../../docs/specs/)
  e OpenAPI da API;
- limites de módulos backend → [`../../docs/architecture/`](../../docs/architecture/).

Integração com a API: [`../../docs/specs/frontend-readiness.md`](../../docs/specs/frontend-readiness.md).

Início rápido da UI: [`../README.md`](../README.md).
