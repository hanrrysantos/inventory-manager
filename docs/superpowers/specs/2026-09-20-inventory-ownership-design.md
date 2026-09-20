# Isolamento de estoque por proprietário

## Objetivo

Cada conta autenticada deve enxergar e alterar somente o estoque associado ao
seu próprio usuário. A API não aceitará `ownerId` vindo do cliente; o
proprietário será derivado do usuário autenticado pelo JWT.

## Decisão de domínio

Usar `owner` nas entidades e `ownerId` em filtros/serviços. No banco, usar
`owner_id` como chave estrangeira para `tb_users(id)`. Essa nomenclatura
expressa a regra de posse sem amarrar o domínio a uma futura decisão de
workspace/tenant.

## Modelo e persistência

- `Category` e `Product` recebem associação obrigatória com `User owner`.
- `Batch` permanece associado a `Product`; sua posse é derivada pelo produto.
- `InventoryLog` permanece associado ao lote/produto; suas leituras e escritas
  são autorizadas pela posse do produto.
- Índices e unicidade passam a ser por proprietário quando a regra é de
  catálogo (`category.name` e `product.sku`).
- A migration nova adicionará as colunas, foreign keys e constraints sem
  editar migrations já aplicadas.
- Dados existentes serão tratados explicitamente pela migration/configuração de
  implantação; a aplicação não exibirá registros sem proprietário.

## Fluxo de autenticação e autorização

O subject atual do JWT é o e-mail. O serviço autenticado resolverá esse
subject para `User` e repassará o `ownerId` aos serviços de catálogo e estoque.
Controllers usarão `Authentication`/principal somente para obter a identidade;
nenhum endpoint confiará em um identificador de usuário no payload.

As operações de usuário comum serão permitidas apenas dentro do próprio
estoque. Operações administrativas de usuários continuarão restritas pelas
regras existentes.

## Consultas e regras de negócio

- Listagens, busca por id/SKU, alteração e exclusão de produtos/categorias
  usarão predicates por `ownerId`.
- Criação definirá o proprietário a partir da autenticação.
- Lotes, consumo FEFO, lotes vencidos, estoque baixo e logs validarão a posse
  do produto antes de executar.
- Dashboard e alertas serão filtrados pelo proprietário autenticado ou pelo
  proprietário do job quando executados de forma agendada.
- A resposta para recurso existente de outro usuário será `404`, evitando
  revelar sua existência.

## Testes

Adicionar testes unitários para propagação do `ownerId` e testes de integração
com duas contas: cada conta cria e consulta seu próprio produto/lote, não
consegue ler ou alterar o produto da outra conta, e os cálculos de dashboard,
FEFO e alertas permanecem isolados.

## Fora do escopo

Não serão introduzidos workspace compartilhado, multi-tenancy, Keycloak,
alteração do formato do JWT, novos endpoints ou mudanças no algoritmo FEFO.
