# Gerenciamento do catálogo

## Objetivo

Completar no frontend as operações de produtos e categorias que já são
suportadas integralmente pela API, respeitando as permissões de `USER` e
`ADMIN`.

## Escopo

### Produtos

- Manter a listagem paginada em `/products`.
- Permitir ordenação por nome, SKU ou ID, em ordem crescente ou decrescente.
- Permitir alternar entre todos os produtos e produtos com estoque baixo por
  meio de `GET /api/v1/products` e `GET /api/v1/products/low-stock`.
- Exibir detalhes obtidos por `GET /api/v1/products/{id}`.
- Para `ADMIN`, permitir:
  - cadastrar nome, SKU, estoque mínimo e categoria;
  - editar somente nome e estoque mínimo, conforme o contrato atual;
  - excluir um produto após confirmação.

### Categorias

- Adicionar `/categories` à navegação autenticada.
- Listar categorias com paginação e ordenação por nome ou ID.
- Exibir os detalhes retornados por `GET /api/v1/categories/{id}`.
- Para `ADMIN`, permitir cadastrar, editar e excluir uma categoria após
  confirmação.

## Permissões

Usuários `USER` e `ADMIN` podem consultar produtos, estoque baixo e
categorias. Somente `ADMIN` visualiza controles de criação, edição e
exclusão. O frontend usa a role de `/api/v1/users/me` para compor a interface;
a API permanece responsável por autorizar cada requisição.

Uma resposta `403` recebida mesmo após a verificação visual será mostrada como
erro da operação, sem encerrar a sessão.

## Interface

As páginas seguem o layout, os estados de carregamento, erro, vazio e a
paginação já existentes.

Operações de criação, edição, detalhes e confirmação usam o elemento nativo
`<dialog>`, sem adicionar dependências. Cada diálogo terá título acessível,
fechamento explícito e pelo teclado, foco inicial e restauração do foco pelo
comportamento nativo.

Os formulários reutilizam React Hook Form, Zod e o tradutor de erros da API já
instalados. Durante o envio, os controles ficam desabilitados. Sucesso fecha o
diálogo, atualiza as consultas afetadas e mostra uma mensagem de confirmação
na página. Falha mantém os dados preenchidos e mostra a mensagem retornada pela
API.

## Estado e integração

Paginação, ordenação e o filtro de estoque baixo ficam na URL para preservar
o estado ao recarregar ou compartilhar a página. Qualquer mudança de filtro ou
ordenação retorna para a primeira página.

TanStack Query gerencia consultas e mutações. Após criar, editar ou excluir,
serão invalidadas as consultas de produtos, categorias e dashboard que possam
ter sido afetadas. Não será criada uma camada genérica adicional sobre o
cliente HTTP.

## Validação

Os contratos do frontend refletirão os limites atuais da API:

- produto: nome obrigatório com até 255 caracteres, SKU obrigatório com até
  50 caracteres, estoque mínimo inteiro maior ou igual a zero e categoria
  obrigatória;
- categoria: nome obrigatório com até 100 caracteres e descrição opcional.

## Testes

O desenvolvimento seguirá ciclos de teste primeiro com Vitest, Testing Library
e MSW. A cobertura comportamental incluirá:

- URLs e payloads de todas as operações de produtos e categorias;
- paginação, ordenação e filtro de estoque baixo;
- controles administrativos visíveis somente para `ADMIN`;
- validação e envio dos formulários;
- confirmação de exclusão;
- atualização da interface e mensagens de erro e sucesso.

## Fora do escopo

- Busca por nome ou SKU.
- Filtro por categoria ou por outros estados de estoque.
- Lotes, entrada, consumo e histórico de movimentações.
- Fornecedores, relatórios e configurações.
- Alteração de SKU ou categoria durante a edição de produto.

Esses itens dependem de outros contratos ou pertencem às próximas etapas.
