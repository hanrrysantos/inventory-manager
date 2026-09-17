# Preparação do backend para o frontend

## Objetivo

Este documento descreve as correções, alterações e adições necessárias no
backend antes e durante o desenvolvimento do frontend do Inventory Manager.

O frontend será uma aplicação web responsiva baseada em:

- React;
- TypeScript;
- Vite.

A referência visual é um painel administrativo de estoque com navegação lateral,
cartões de indicadores, tabela de produtos, filtros, busca e uma área de itens
que precisam de atenção. A interface deve transmitir a mesma ideia geral da
imagem de referência, mas os dados exibidos precisam ser derivados de contratos
reais da API.

Este documento não autoriza a implementação das alterações. Ele serve como
base para definir o próximo plano de desenvolvimento do backend e a integração
com o frontend.

## Estado atual identificado

O backend já fornece:

- autenticação JWT;
- usuários com roles `ADMIN` e `USER`;
- CRUD de produtos;
- CRUD de categorias;
- criação de lotes;
- entrada de estoque em lote;
- consumo de estoque com FEFO;
- listagem de lotes vencidos;
- listagem de produtos abaixo do estoque mínimo;
- documentação OpenAPI/Swagger;
- PostgreSQL, Flyway e controle transacional do estoque.

Os principais contratos existentes estão sob `/api/v1`.

## Bloqueios obrigatórios antes da integração

### 1. Configurar CORS

O frontend será executado em uma origem diferente do backend durante o
desenvolvimento e, provavelmente, também em produção. O backend precisa aceitar
explicitamente as origens configuradas para o frontend.

Requisitos:

- permitir as origens de desenvolvimento, como `http://localhost:5173`;
- permitir a origem oficial do frontend em produção por variável de ambiente;
- permitir os métodos usados pela API: `GET`, `POST`, `PUT`, `PATCH` e `DELETE`;
- permitir os headers `Authorization` e `Content-Type`;
- permitir as respostas ao preflight `OPTIONS`;
- não usar `*` como origem quando houver autenticação baseada em credenciais;
- documentar a configuração junto das variáveis de ambiente.

### 2. Expor a identidade do usuário autenticado

O login atualmente retorna somente o token JWT. O frontend precisa saber o nome,
e-mail e role do usuário para montar o cabeçalho, o menu e as permissões da
interface.

Adicionar um endpoint autenticado, preferencialmente:

```text
GET /api/v1/users/me
```

Resposta sugerida:

```json
{
  "id": 1,
  "name": "Ana Souza",
  "email": "ana@example.com",
  "role": "ADMIN",
  "createdAt": "2026-09-17T12:00:00"
}
```

O contrato de usuário precisará expor a role sem expor senha. O backend deve
obter o usuário a partir do subject do JWT, e não confiar em um `id` enviado
pelo cliente para determinar a identidade atual.

Como alternativa, a role pode ser incluída no `AuthResponseDTO`, mas o endpoint
`/me` continua sendo recomendado para hidratar a sessão após recarregar a
página.

### 3. Padronizar autenticação e respostas de erro

O frontend precisa distinguir claramente entre:

- token ausente ou expirado;
- credenciais inválidas;
- acesso proibido por role;
- validação de dados;
- recurso não encontrado;
- conflito de negócio;
- estoque insuficiente;
- erro inesperado.

Manter o formato `StandardError`, mas garantir que todos os fluxos relevantes
retornem consistentemente:

```json
{
  "instant": "2026-09-17T12:00:00Z",
  "status": 409,
  "error": "ProductAlreadyExistsException",
  "message": "Product already exists",
  "path": "/api/v1/products"
}
```

Também é necessário validar e documentar o comportamento para respostas `401`
e `403` geradas pelo Spring Security, pois elas podem não utilizar o mesmo
payload do `GlobalExceptionHandler`.

## Endpoints que devem ser adicionados

### 1. Resumo do dashboard

Para evitar que o frontend faça várias consultas e calcule regras de negócio,
adicionar um endpoint de leitura específico para o painel:

```text
GET /api/v1/dashboard/summary
```

Resposta sugerida:

```json
{
  "totalQuantity": 714,
  "productCount": 8,
  "lowStockCount": 3,
  "outOfStockCount": 1,
  "expiredBatchCount": 2,
  "inventoryValue": 21480.00,
  "attentionItems": [
    {
      "productId": 3,
      "productName": "Sabonete Natural Lavanda",
      "sku": "SAB-207",
      "quantity": 0,
      "minStock": 25,
      "status": "OUT_OF_STOCK"
    }
  ]
}
```

O endpoint deve centralizar os cálculos do dashboard, especialmente:

- quantidade total em estoque;
- quantidade de produtos cadastrados;
- produtos abaixo do mínimo;
- produtos sem estoque;
- quantidade de lotes vencidos;
- valor total em estoque;
- itens que precisam de atenção.

O valor em estoque deve ter uma regra explícita. A recomendação é utilizar a
quantidade atual de cada lote multiplicada pelo preço unitário do lote.

### 2. Histórico de movimentações

O backend possui `InventoryLog`, mas não expõe consulta HTTP. Adicionar:

```text
GET /api/v1/inventory-logs
GET /api/v1/inventory-logs/{id}
```

Filtros recomendados:

- `productId`;
- `batchId`;
- `type` (`INPUT` ou `OUTPUT`);
- `from`;
- `to`;
- paginação;
- ordenação por data.

Resposta sugerida:

```json
{
  "content": [
    {
      "id": 10,
      "type": "OUTPUT",
      "timestamp": "2026-09-17T10:30:00",
      "quantity": 12,
      "batchId": 4,
      "productId": 2,
      "productName": "Chá Verde Orgânico",
      "batchNumber": "LOT-2026-04"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Movimentações devem continuar sendo imutáveis para o frontend. Não deve ser
criada uma operação de edição ou exclusão de logs.

### 3. Consulta de lotes por produto

Adicionar endpoints de consulta para permitir a tela de detalhes do produto e a
gestão operacional dos lotes:

```text
GET /api/v1/products/{productId}/batches
GET /api/v1/batches/{id}
```

Filtros recomendados para a listagem:

- `status=AVAILABLE`;
- `status=EXPIRED`;
- `expiryBefore`;
- paginação;
- ordenação por validade.

Cada lote deve retornar informações suficientes para exibir:

- número do lote;
- produto;
- quantidade atual;
- data de fabricação;
- data de validade;
- preço unitário;
- status calculado (`AVAILABLE`, `EXPIRED` ou `EMPTY`).

O frontend não deve reimplementar a regra FEFO. A ordenação usada para consumo
continua sendo responsabilidade do backend.

### 4. Consulta de produto com detalhes

O endpoint de produto deve continuar simples para tabelas, mas é recomendável
adicionar uma resposta detalhada ou endpoints específicos para evitar que o
frontend precise buscar diversos recursos manualmente.

Opção recomendada:

```text
GET /api/v1/products/{id}/details
```

Esse contrato pode incluir:

- dados cadastrais;
- categoria;
- estoque total;
- estoque mínimo;
- status do estoque;
- lotes;
- últimas movimentações.

Esse endpoint deve ser criado somente se a tela de detalhes realmente precisar
dessas informações. Não é necessário transformar o endpoint principal de
produtos em uma resposta excessivamente grande.

### 5. Operações em lote e respostas úteis

O consumo atual retorna `204 No Content`. Isso é válido, mas uma resposta com o
resultado da operação seria melhor para a experiência do frontend:

```json
{
  "productId": 2,
  "requestedQuantity": 12,
  "consumedQuantity": 12,
  "remainingQuantity": 88,
  "affectedBatches": [
    {
      "batchId": 4,
      "quantityConsumed": 12,
      "remainingQuantity": 3
    }
  ],
  "lowStock": false
}
```

Essa alteração deve preservar a atomicidade, o FEFO, o locking pessimista e o
rollback quando não houver estoque suficiente.

## Correções e melhorias nos contratos existentes

### Produtos

- Corrigir e padronizar o nome `UpdateProdcutRequestDTO` para
  `UpdateProductRequestDTO`, mantendo compatibilidade se necessário.
- Definir validação para `name`, `sku`, `minStock` e `categoryId`.
- Documentar que a atualização atual não altera SKU nem categoria.
- Definir o status calculado do estoque:
  - `IN_STOCK`;
  - `LOW_STOCK`;
  - `OUT_OF_STOCK`.
- Avaliar busca por nome ou SKU.
- Adicionar paginação antes que o catálogo cresça.

### Categorias

- Validar nome obrigatório e tamanho máximo.
- Garantir resposta de conflito consistente para nomes duplicados.
- Retornar erro claro quando a categoria possuir produtos e não puder ser
  removida.

### Lotes

- Validar `batchNumber`, `quantity`, `manufacturingDate`, `expiryDate`,
  `price` e `productId`.
- Garantir que datas de fabricação e validade sejam coerentes.
- Definir claramente se lote vencido pode receber entrada de estoque.
- Retornar status do lote no DTO.
- Permitir consulta por produto.
- Garantir que a operação de adição de estoque retorne o saldo atualizado.

### Usuários

- Adicionar `role` ao DTO de resposta, sem expor senha.
- Criar `GET /api/v1/users/me`.
- Definir se usuários comuns podem alterar o próprio perfil ou se toda edição
  permanece exclusiva de `ADMIN`.
- Retornar informações consistentes para o menu do frontend.

### Autenticação

- Definir o comportamento de login com credenciais inválidas.
- Definir o comportamento de token expirado.
- Avaliar retorno de `user` ou `role` no login.
- Documentar duração do token.
- Considerar um endpoint de renovação somente se a sessão exigir longa duração.

## Validações de entrada necessárias

Os DTOs de entrada devem possuir validações de backend, além da validação do
frontend.

Regras mínimas:

- nomes obrigatórios e não vazios;
- e-mails válidos;
- senhas com tamanho mínimo;
- quantidades maiores que zero nas entradas e saídas;
- estoque mínimo maior ou igual a zero;
- preços maiores ou iguais a zero;
- SKU obrigatório e com tamanho máximo;
- número de lote obrigatório;
- produto e categoria existentes;
- validade não anterior à fabricação;
- quantidade consumida maior que zero.

Os controllers devem utilizar `@Valid` nos corpos que possuem validação.

## Autorização que o frontend deve refletir

### `USER` e `ADMIN`

Podem:

- consultar produtos;
- consultar categorias;
- consultar lotes;
- adicionar estoque a lote;
- consumir estoque;
- consultar o dashboard;
- consultar movimentações, caso o endpoint seja criado para ambas as roles.

### Somente `ADMIN`

Pode:

- criar, editar e excluir produtos;
- criar, editar e excluir categorias;
- criar lotes;
- consultar e administrar usuários.

O frontend deve ocultar ou desabilitar ações conforme a role, mas o backend deve
continuar sendo a autoridade final da autorização.

## Requisitos específicos do dashboard

A interface de referência sugere a seguinte composição:

### Navegação lateral

Itens possíveis:

- Painel;
- Produtos;
- Lotes;
- Movimentações;
- Lotes vencidos;
- Relatórios;
- Usuários, somente para `ADMIN`;
- Configurações.

O item “Fornecedores” presente na referência visual não deve ser implementado
agora, pois o backend não possui domínio, entidade ou endpoint de fornecedores.
Ele deve ficar reservado para uma etapa futura.

### Cabeçalho

Deve apresentar:

- título da tela;
- saudação opcional;
- usuário atual;
- role formatada;
- ação de logout;
- indicador de alertas, quando existir uma fonte real de notificações.

Não exibir notificações fictícias. O indicador pode começar mostrando apenas o
contador de itens críticos retornado pelo dashboard.

### Cartões de indicadores

Indicadores recomendados:

- total de itens em estoque;
- quantidade de produtos;
- produtos com estoque baixo;
- produtos sem estoque;
- lotes vencidos;
- valor total em estoque.

Os valores devem vir do endpoint de resumo ou de contratos bem definidos. O
frontend não deve somar quantidades de páginas parcialmente carregadas.

### Tabela de produtos

Colunas recomendadas:

- produto;
- SKU;
- categoria;
- quantidade atual;
- estoque mínimo;
- valor ou preço de referência;
- status;
- ações.

Filtros:

- todos;
- em estoque;
- estoque baixo;
- sem estoque;
- categoria;
- busca por nome ou SKU.

O backend precisa fornecer paginação, busca ou um endpoint adequado quando o
catálogo não puder mais ser carregado integralmente.

### Área “Precisa de atenção”

Deve listar, no mínimo:

- produtos sem estoque;
- produtos abaixo do mínimo;
- lotes vencidos ou próximos do vencimento, caso essa regra seja adicionada.

Cada item deve permitir navegar para o detalhe do produto ou lote. A regra de
prioridade deve vir do backend, evitando que cada tela calcule uma prioridade
diferente.

## Relatórios

O backend já possui geração de PDF para alertas, mas não existe ainda uma
experiência de relatório no frontend.

Antes de criar a tela “Relatórios”, definir quais relatórios serão suportados:

- estoque atual;
- movimentações por período;
- lotes vencidos;
- produtos abaixo do mínimo;
- valor do estoque.

Para cada relatório, definir:

- endpoint;
- filtros;
- formato de resposta;
- exportação PDF ou CSV;
- permissões;
- limite de período e volume.

Não criar uma tela genérica de relatórios sem contratos definidos.

## Observabilidade e integração

Antes do uso em produção pelo frontend, é recomendável:

- registrar erros com correlation ID;
- incluir um identificador de requisição nas respostas ou headers;
- documentar timeout esperado;
- documentar comportamento de cold start do deploy atual;
- configurar health check;
- garantir que o Swagger esteja disponível no ambiente de desenvolvimento;
- versionar a especificação OpenAPI usada para gerar tipos do frontend.

## OpenAPI e geração de tipos

O frontend deve consumir a especificação OpenAPI, evitando duplicação manual de
interfaces sempre que possível.

Fluxo recomendado:

1. estabilizar os contratos do backend;
2. garantir que o Swagger descreva schemas, parâmetros e respostas reais;
3. exportar a especificação OpenAPI;
4. gerar tipos TypeScript;
5. criar uma camada de serviços no frontend sobre esses tipos.

A geração de tipos não substitui a camada de domínio do frontend. Ela deve
representar os contratos HTTP, enquanto os componentes utilizam modelos
adequados para apresentação.

## Ordem recomendada de execução no backend

### Fase 1 — Bloqueios de integração

1. CORS configurável por ambiente.
2. Padronização de `401`, `403` e erros de validação.
3. Endpoint `/users/me`.
4. Role no DTO de usuário.
5. Documentação OpenAPI corrigida e atualizada.

### Fase 2 — Contratos do dashboard

1. Endpoint `/dashboard/summary`.
2. Status de estoque padronizado.
3. Produtos sem estoque e estoque baixo claramente diferenciados.
4. Regra de valor total do estoque.
5. Lista de itens que precisam de atenção.

### Fase 3 — Operação diária

1. Consulta de lotes por produto.
2. Consulta individual de lote.
3. Histórico de movimentações.
4. Filtros e paginação.
5. Resposta detalhada para consumo de estoque.

### Fase 4 — Relatórios e evolução

1. Definição dos relatórios.
2. Exportações.
3. Notificações no painel.
4. Fornecedores, somente quando o domínio for aprovado.

## O que não deve ser feito neste momento

- Não criar fornecedores apenas para reproduzir o menu da referência visual.
- Não duplicar as regras FEFO no frontend.
- Não calcular autorização somente no frontend.
- Não acessar o banco diretamente pelo frontend.
- Não expor entidades JPA como contrato público.
- Não criar microserviço separado para o dashboard.
- Não introduzir WebSocket antes de existir uma necessidade real de atualização
  em tempo real.
- Não criar gráficos com dados inventados ou estimados.
- Não alterar as migrations já aplicadas; mudanças de schema devem usar novas
  migrations Flyway.

## Critérios para iniciar o frontend

O desenvolvimento do frontend pode começar com segurança quando, no mínimo:

- CORS estiver configurado;
- login, logout e expiração de token estiverem documentados;
- o frontend conseguir obter o usuário atual e sua role;
- os contratos de produto, categoria e lote estiverem validados;
- existir um endpoint de resumo do dashboard ou uma decisão explícita de usar
  múltiplos endpoints temporariamente;
- os erros de validação, autenticação e autorização tiverem formato conhecido;
- Swagger/OpenAPI refletir os contratos reais;
- o backend estiver acessível em um ambiente de desenvolvimento estável.

## Decisão visual registrada

O frontend deverá seguir uma linguagem visual semelhante à referência anexada:

- layout administrativo com sidebar;
- cores claras e aparência limpa;
- verde como cor principal de ações e estado saudável;
- cartões com bordas suaves e indicadores resumidos;
- tabelas legíveis com status visuais;
- área de atenção destacada, sem excesso de informação;
- responsividade para telas menores;
- ações operacionais rápidas para entrada, saída e consulta de estoque.

A referência visual orienta a experiência, mas não altera o escopo funcional do
backend. Qualquer item visual sem suporte de domínio deve ser tratado como uma
decisão futura, não como dado fictício.
