# MVP Hardening and Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Corrigir os findings do primeiro review, tornar o login resiliente ao cold start da API, publicar a branch via pull request e disponibilizar o frontend em produção.

**Architecture:** As correções permanecem nos componentes existentes, sem novas dependências. O cliente HTTP terá um timeout compatível com a hospedagem atual e mensagens específicas para indisponibilidade; a publicação será uma SPA estática gerada pelo Vite com fallback das rotas para `index.html`.

**Tech Stack:** React, TypeScript, Vite, Axios, TanStack Query, Vitest, Testing Library, GitHub e Vercel ou Cloudflare Pages.

**Spec:** `docs/superpowers/specs/2026-09-17-frontend-mvp-design.md`

## Global Constraints

- Não adicionar novas telas ou operações de escrita do estoque.
- Corrigir cada comportamento com teste falhando antes da implementação.
- Preservar `VITE_API_URL` como única fonte da URL do backend.
- Não versionar tokens, credenciais ou arquivos `.env`.
- Não fazer merge automático do pull request.
- A origem pública do frontend deverá ser adicionada ao CORS do backend.

---

### Task 1: Acessibilidade do drawer e dos filtros

**Files:**
- Modify: `src/components/layout/AppLayout.tsx`
- Modify: `src/components/layout/AppLayout.test.tsx`
- Modify: `src/features/products/ProductFilters.tsx`
- Modify: `src/features/products/ProductsPage.test.tsx`

**Interfaces:**
- Produces: drawer modal que recebe foco, fecha com `Escape`, contém o foco e o devolve ao acionador.
- Produces: filtros que expõem a seleção por `aria-pressed`.

- [x] **Step 1: Escrever testes falhando de teclado e estado acessível**

Adicionar testes que abrem o drawer, verificam foco no botão de fechar, fecham com `Escape`, verificam retorno do foco ao acionador e confirmam `aria-pressed` no filtro selecionado.

- [x] **Step 2: Confirmar RED**

Run:

```bash
npm test -- src/components/layout/AppLayout.test.tsx src/features/products/ProductsPage.test.tsx
```

Expected: FAIL porque o drawer não gerencia foco/teclado e os filtros não possuem `aria-pressed`.

- [x] **Step 3: Implementar a correção mínima**

Usar refs e efeito no `AppLayout` para focar o diálogo, tratar `Escape`, circular `Tab` e restaurar o foco. Marcar o conteúdo de fundo como `inert` enquanto o drawer estiver aberto. Adicionar `aria-pressed={status === filter.value}` aos botões de filtro.

- [x] **Step 4: Confirmar GREEN**

Executar novamente os dois arquivos de teste e confirmar saída sem warnings.

---

### Task 2: Estado de erro do painel de atenção

**Files:**
- Modify: `src/features/dashboard/DashboardPage.tsx`
- Modify: `src/features/dashboard/DashboardPage.test.tsx`

**Interfaces:**
- Produces: estado terminal de erro no painel de atenção quando `/dashboard/summary` falha.

- [x] **Step 1: Escrever teste falhando do erro terminal**

No cenário existente de erro do resumo, verificar que a região de itens críticos informa indisponibilidade e não mantém `Carregando itens críticos...`.

- [x] **Step 2: Confirmar RED**

Run: `npm test -- src/features/dashboard/DashboardPage.test.tsx`

Expected: FAIL porque a região permanece no loader.

- [x] **Step 3: Implementar a correção mínima**

Renderizar `ErrorState` na região lateral quando `summaryQuery.isError`, usando o mesmo `refetch` do resumo.

- [x] **Step 4: Confirmar GREEN**

Executar o teste do dashboard novamente.

---

### Task 3: Cold start e mensagens de conexão

**Files:**
- Modify: `src/services/api-client.ts`
- Modify: `src/services/api-error.ts`
- Create: `src/services/api-error.test.ts`
- Modify: `src/features/auth/LoginPage.tsx`
- Modify: `README.md`

**Interfaces:**
- Produces: `getApiErrorMessage(error, fallback)` distinguindo timeout e falha de conexão.
- Produces: timeout HTTP de 90 segundos para acomodar o cold start observado.

- [x] **Step 1: Escrever testes falhando para timeout e rede**

Criar `api-error.test.ts` com `AxiosError` de código `ECONNABORTED` esperando uma orientação para aguardar e tentar novamente, e erro sem `response` esperando uma mensagem de conexão.

- [x] **Step 2: Confirmar RED**

Run: `npm test -- src/services/api-error.test.ts`

Expected: FAIL porque ambos os casos retornam apenas o fallback.

- [x] **Step 3: Implementar mensagens e timeout**

Tratar `ECONNABORTED`/`ETIMEDOUT`, tratar ausência de `response`, elevar o timeout para `90_000` e informar na tela de login que o primeiro acesso pode levar até um minuto.

- [x] **Step 4: Confirmar GREEN**

Executar o teste unitário e o fluxo integrado de autenticação.

---

### Task 4: Verificação, review e integração GitHub

**Files:**
- Modify: este plano, marcando resultados reais.

- [x] **Step 1: Executar validação completa**

Run:

```bash
npm test
npm run lint
npm run typecheck
npm run build
git diff --check
```

- [x] **Step 2: Executar novo code review**

Revisar o diff completo contra `main`. Em caso de finding bloqueante, parar antes de push/PR.

Result: `APPROVED_WITH_NOTES`, sem findings bloqueantes. A transição entre
breakpoints com o drawer já aberto permanece como cenário visual não automatizado.

- [x] **Step 3: Commitar e publicar a branch**

Criar commit focado, executar `git push -u origin feat/frontend-mvp` e abrir PR contra `main`, sem fazer merge.

Result: branch publicada e PR aberto contra `main` em
`https://github.com/hanrrysantos/inventory-manager-frontend/pull/1`.

---

### Task 5: Publicação da SPA

**Files:**
- Create or Modify: configuração mínima exigida pela plataforma escolhida
- Modify: `README.md`

- [x] **Step 1: Selecionar plataforma autenticada**

Preferir Vercel; usar Cloudflare Pages se for a integração já disponível. Se nenhuma conta estiver autenticada, preparar toda a configuração e solicitar somente a autenticação ao usuário.

- [x] **Step 2: Configurar build e fallback SPA**

Usar `npm run build`, diretório `dist`, variável `VITE_API_URL=https://inventory.hanrry.top` e fallback de `/dashboard` e `/products` para `index.html`.

- [ ] **Step 3: Publicar e validar**

Validar HTTP 200 para `/`, `/login`, `/dashboard` e `/products`, testar login real e registrar a URL no README.

Partial result: deploy publicado em `https://controledeestoque.hanrry.top` e
as quatro rotas responderam HTTP 200. O teste de login aguarda a liberação do
CORS no backend.

- [ ] **Step 4: Atualizar CORS do backend se necessário**

Adicionar somente a origem final do frontend em `FRONTEND_ORIGINS` na hospedagem do backend. Se o ambiente externo não estiver acessível, reportar exatamente a variável e o valor pendentes.

Pending external configuration: o preflight retornou HTTP 403. Configurar na
hospedagem do backend `FRONTEND_ORIGINS=https://controledeestoque.hanrry.top`
(ou acrescentar essa origem à lista atual, separada por vírgula) e reiniciar o
serviço.
