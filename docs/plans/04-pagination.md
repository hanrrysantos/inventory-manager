# Paginação nos endpoints de listagem — Implementation Plan

> **For agentic workers:** execute uma tarefa por vez, usando a Skill `code-review` após cada tarefa. Não execute a próxima tarefa automaticamente.

**Goal:** padronizar paginação nos endpoints de listagem existentes e nos novos contratos previstos para operação diária, com resposta HTTP estável para o frontend, filtros por owner quando aplicável e testes que preservem as regras de negócio atuais.

**Architecture:** contrato paginado em `shared`; controllers recebem `Pageable` e retornam `PageResponse<T>`; services resolvem owner via `OwnerContext` e delegam a repositórios `Page<>`; mappers continuam convertendo entidades em DTOs. Endpoints futuros (`inventory-logs`, lotes por produto) nascem já paginados, sem passagem intermediária por `List` na API pública.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, Spring Data Web (`Pageable`), OpenAPI 3, JUnit 5, Mockito, MockMvc, Testcontainers PostgreSQL quando a tarefa exigir persistência real.

**Spec:** decisões de produto em `docs/frontend-readiness.md` (Fase 3 — filtros e paginação); limites de módulo em `docs/architecture.md`.

## Global Constraints

- Use `bash ./mvnw ...`; `mvnw` permanece com permissão 644.
- Execute somente a tarefa autorizada e aguarde autorização antes da próxima.
- Não edite migrations Flyway já aplicadas.
- Não introduza RabbitMQ, observabilidade, Redis ou microserviços.
- Não altere regras de FEFO, locking pessimista, consumo ou cálculo de estoque mínimo (`totalQuantity <= minStock`, incluindo igualdade) salvo quando a tarefa explicitamente mover a mesma regra para uma query equivalente.
- Não expor `org.springframework.data.domain.Page` diretamente na API REST.
- Preservar autorização existente em `SecurityConfig` (usuários somente `ADMIN`; produtos/categorias/lotes conforme hoje).
- Cada tarefa termina com testes e `code-review` limitado ao diff da tarefa.
- Em `CHANGES_REQUESTED`, parar e reportar; não executar `fix-findings` automaticamente.
- Não fazer push, não criar commits automaticamente e não avançar entre tarefas.

## Estado atual preservado

- Listagens retornam `ResponseEntity<List<...>>` sem query params de página.
- `ProductService.getLowStockProducts` carrega todos os produtos e filtra em memória com `ProductMapper.calculateTotalQuantity` (soma de **todas** as quantidades de lotes, inclusive vencidos — comportamento atual do mapper).
- `BatchService.findExpiredBatches` usa `findByExpiryDateBefore(LocalDate.now())` e filtra owner em memória.
- `OwnerContext` restringe produtos e categorias via `findAllByOwner` / `findByIdAndOwner`; usuários não possuem filtro por owner.
- Não existem controllers HTTP para `InventoryLog`; apenas persistência via `InventoryLogService`.

---

## Especificação aprovada

### Escopo deste plano

Inclui:

- DTO genérico `PageResponse<T>` e configuração de `Pageable` (tamanho máximo, defaults).
- Paginação de `GET /api/v1/products`, `GET /api/v1/categories`, `GET /api/v1/users`.
- Paginação de `GET /api/v1/products/low-stock` com a **mesma regra** de estoque baixo, preferencialmente via query no banco.
- Paginação de `GET /api/v1/batches/expired` com filtro de owner no banco.
- Documentação OpenAPI (`*ControllerDocs`) alinhada aos parâmetros e ao schema de resposta.
- Atualização dos testes de controller e service afetados.

Inclui quando autorizada a tarefa correspondente (endpoints ainda inexistentes):

- `GET /api/v1/inventory-logs` paginado, com filtros descritos em `docs/frontend-readiness.md`.
- `GET /api/v1/products/{productId}/batches` paginado.

Não inclui:

- Busca textual (`q`, nome, SKU) — tarefa futura combinada com paginação.
- Paginação do dashboard ou agregados.
- Compatibilidade dual (retornar array quando `page`/`size` ausentes); ver decisão abaixo.
- Alteração de regras de consumo, validade ou notificações.

### Contrato HTTP

**Query parameters:**

| Parâmetro | Default | Validação |
| --- | --- | --- |
| `page` | `0` | Inteiro ≥ 0; caso contrário `400` |
| `size` | `20` | Inteiro entre 1 e 100 inclusive; caso contrário `400` |
| `sort` | por endpoint | Formato Spring `propriedade,(asc\|desc)`; propriedades fora da whitelist → `400` ou ignorar conforme política da Tarefa 1 (documentar escolha no OpenAPI) |

**Corpo de resposta (substitui o array na raiz):**

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

**Decisão de compatibilidade:** resposta paginada é o **único** formato das listagens cobertas por este plano. Clientes que esperavam `[]` na raiz devem migrar para `content`. Não haverá modo legado.

**Sort permitido por recurso:**

| Recurso | Propriedades | Default |
| --- | --- | --- |
| Produtos | `id`, `name`, `sku` | `name,asc` |
| Categorias | `id`, `name` | `name,asc` |
| Usuários | `id`, `name`, `email` | `name,asc` |
| Low stock | `id`, `name`, `sku` | `name,asc` |
| Lotes expirados | `id`, `expiryDate`, `batchNumber` | `expiryDate,asc` |
| Inventory logs (futuro) | `timestamp`, `id` | `timestamp,desc` |
| Lotes por produto (futuro) | `expiryDate`, `id`, `batchNumber` | `expiryDate,asc` |

### Infraestrutura compartilhada

**Arquivos novos (Tarefa 1):**

- `src/main/java/br/com/hanrry/inventory/shared/dto/PageResponse.java`
- `src/main/java/br/com/hanrry/inventory/shared/config/PaginationConfig.java` (ou equivalente em config existente)

**Interface esperada:**

```java
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) { ... }
}
```

Configurar `PageableHandlerMethodArgumentResolverCustomizer` com `setMaxPageSize(100)` e page size default 20 (via `@PageableDefault` nos controllers ou defaults globais documentados).

---

## Queries de referência (JPQL)

### Produtos e categorias (owner)

Padrão Spring Data quando possível:

```java
Page<Product> findAllByOwner(User owner, Pageable pageable);
```

Para ambiente sem owner autenticado (testes legados com `ownerContext == null`), manter ramo `productRepository.findAll(pageable)`.

Query explícita equivalente ao `findAllByOwner` atual:

```jpql
SELECT p FROM Product p
WHERE p.owner = :owner OR p.owner IS NULL
```

Com `Pageable`, aplicar sort apenas sobre campos da entidade `Product`.

### Lotes expirados com owner no banco

Substituir listagem + `stream().filter` por:

```jpql
SELECT b FROM Batch b
JOIN b.product p
WHERE b.expiryDate < :today
  AND (:owner IS NULL OR p.owner = :owner OR p.owner IS NULL)
```

Parâmetros: `:today = LocalDate.now()`, `:owner` nullable (`User` ou null quando `ownerContext.currentUser()` for null).

Método sugerido no `BatchRepository`:

```java
@Query("""
        SELECT b FROM Batch b
        JOIN b.product p
        WHERE b.expiryDate < :today
          AND (:owner IS NULL OR p.owner = :owner OR p.owner IS NULL)
        """)
Page<Batch> findExpiredBatches(
        @Param("today") LocalDate today,
        @Param("owner") User owner,
        Pageable pageable);
```

**Nota:** alinhar semântica com o comportamento atual: `expiryDateBefore(now)` exclui lotes com validade **igual** a hoje da listagem de expirados.

### Estoque baixo paginado (equivalente ao mapper)

Regra atual em `ProductMapper.calculateTotalQuantity`: soma de `batch.quantity` para todos os lotes do produto (sem excluir vencidos). Filtro: `totalQuantity <= minStock`.

**Contagem e página (JPQL):**

```jpql
SELECT p FROM Product p
WHERE (:owner IS NULL OR p.owner = :owner OR p.owner IS NULL)
  AND (
    SELECT COALESCE(SUM(b.quantity), 0)
    FROM Batch b
    WHERE b.product = p
  ) <= p.minStock
```

Usar `Page<Product>` com `Pageable`. Para evitar N+1 ao mapear DTOs com `totalQuantity`, preferir `@EntityGraph(attributePaths = "batches")` na query ou fetch join controlado — avaliar impacto de memória; alternativa aceitável neste plano: manter lazy load dentro da transação read-only do service (como hoje) desde que a paginação limite `size` máximo 100.

**Variante count (Spring Data deriva de `Page` automaticamente)** — se performance exigir query dedicada na revisão:

```jpql
SELECT COUNT(p) FROM Product p
WHERE (:owner IS NULL OR p.owner = :owner OR p.owner IS NULL)
  AND (
    SELECT COALESCE(SUM(b.quantity), 0)
    FROM Batch b
    WHERE b.product = p
  ) <= p.minStock
```

**Teste de caracterização obrigório:** produto com lote vencido ainda conta quantidade na soma; produto com `totalQuantity == minStock` permanece na lista.

### Inventory logs (Tarefa 7 — endpoint novo)

Entidade: `InventoryLog` (`timestamp`, `type`, `product`, `batch`).

Listagem paginada com filtros opcionais:

```jpql
SELECT l FROM InventoryLog l
JOIN l.product p
WHERE (:productId IS NULL OR p.id = :productId)
  AND (:batchId IS NULL OR l.batch.id = :batchId)
  AND (:type IS NULL OR l.type = :type)
  AND (:from IS NULL OR l.timestamp >= :from)
  AND (:to IS NULL OR l.timestamp <= :to)
  AND (:owner IS NULL OR p.owner = :owner OR p.owner IS NULL)
```

Default sort: `timestamp DESC`. DTO de resposta conforme `docs/frontend-readiness.md` (incluir `productName`, `batchNumber` via join ou mapper).

### Lotes por produto (Tarefa 8 — endpoint novo)

Base:

```jpql
SELECT b FROM Batch b
JOIN b.product p
WHERE p.id = :productId
  AND (:owner IS NULL OR p.owner = :owner OR p.owner IS NULL)
```

Filtros adicionais (implementação incremental dentro da tarefa):

- `status=AVAILABLE`: `b.quantity > 0` e `b.expiryDate >= :today`
- `status=EXPIRED`: `b.expiryDate < :today`
- `status=EMPTY`: `b.quantity = 0`
- `expiryBefore`: `b.expiryDate < :expiryBefore`

Validar existência do produto e owner antes da query (reutilizar padrão `findProductById`).

---

## Plano de execução

### Tarefa 1: Fundação `PageResponse` e configuração de paginação

**Objetivo:** introduzir o contrato paginado compartilhado e limites de `Pageable` sem alterar endpoints ainda.

**Arquivos:**

- Criar: `src/main/java/br/com/hanrry/inventory/shared/dto/PageResponse.java`
- Criar: `src/main/java/br/com/hanrry/inventory/shared/config/PaginationConfig.java`
- Criar: `src/test/java/br/com/hanrry/inventory/shared/dto/PageResponseTest.java`

**Implementação:** factory `from(Page<E>, Function<E,T>)`; cálculo `totalPages` via `page.getTotalPages()`; testes unitários com `PageImpl` mock.

**Validação:**

```bash
bash ./mvnw -Dtest=PageResponseTest test
```

**Conclusão:** DTO e config prontos para uso nos controllers.

**Review:** `code-review` somente sobre o diff da Tarefa 1.

---

### Tarefa 2: Paginação de produtos

**Objetivo:** `GET /api/v1/products` retorna `PageResponse<ProductResponseDTO>`.

**Arquivos:**

- Modificar: `ProductRepository.java` — métodos `Page<Product> findAll(Pageable)` e `Page<Product> findAllByOwner(User owner, Pageable)` (query existente + `Pageable`)
- Modificar: `ProductService.java`, `ProductController.java`, `ProductControllerDocs.java`
- Modificar: `ProductControllerTest.java`, `ProductServiceTest.java`

**Controller:** `@PageableDefault(size = 20, sort = "name") Pageable pageable`.

**Validação:**

```bash
bash ./mvnw -Dtest=ProductControllerTest,ProductServiceTest test
```

**Conclusão:** listagem paginada com owner; MockMvc valida JSON `content`, `page`, `size`, `totalElements`, `totalPages`.

**Review:** `code-review` sobre o diff da Tarefa 2.

---

### Tarefa 3: Paginação de categorias

**Objetivo:** `GET /api/v1/categories` retorna `PageResponse<CategoryResponseDTO>`.

**Arquivos:**

- Modificar: `CategoryRepository.java`, `CategoryService.java`, `CategoryController.java`, `CategoryControllerDocs.java`
- Modificar: `CategoryControllerTest.java`, `CategoryServiceTest.java`

**Validação:**

```bash
bash ./mvnw -Dtest=CategoryControllerTest,CategoryServiceTest test
```

**Conclusão:** paridade com produtos (owner + defaults).

**Review:** `code-review` sobre o diff da Tarefa 3.

---

### Tarefa 4: Paginação de usuários

**Objetivo:** `GET /api/v1/users` retorna `PageResponse<UserResponseDTO>` (somente `ADMIN`).

**Arquivos:**

- Modificar: `UserService.java`, `UserController.java`, `UserControllerDocs.java`
- Modificar: `UserControllerTest.java`, `UserServiceTest.java`

**Validação:**

```bash
bash ./mvnw -Dtest=UserControllerTest,UserServiceTest test
```

**Conclusão:** paginação sem filtro de owner; autorização inalterada.

**Review:** `code-review` sobre o diff da Tarefa 4.

---

### Tarefa 5: Lotes expirados paginados (owner no banco)

**Objetivo:** `GET /api/v1/batches/expired` retorna `PageResponse<BatchResponseDTO>` usando query JPQL da seção anterior.

**Arquivos:**

- Modificar: `BatchRepository.java`, `BatchService.java`, `BatchController.java`, `BatchControllerDocs.java`
- Modificar: `BatchControllerTest.java`, `BatchServiceTest.java`

**Validação:**

```bash
bash ./mvnw -Dtest=BatchControllerTest,BatchServiceTest test
```

**Conclusão:** sem filtro de owner em memória; sort default `expiryDate,asc`.

**Review:** `code-review` sobre o diff da Tarefa 5.

---

### Tarefa 6: Produtos com estoque baixo paginados

**Objetivo:** `GET /api/v1/products/low-stock` retorna `PageResponse<ProductResponseDTO>` com regra equivalente ao filtro atual.

**Arquivos:**

- Modificar: `ProductRepository.java` — query JPQL de low stock + `Pageable`
- Modificar: `ProductService.java`, `ProductController.java`, `ProductControllerDocs.java`
- Modificar: `ProductServiceTest.java`, `ProductControllerTest.java`

**Testes obrigatórios:**

- produto com soma de lotes `<= minStock` incluído;
- igualdade `totalQuantity == minStock` incluída;
- lote vencido ainda soma na quantidade total;
- paginação: `totalElements` reflete conjunto filtrado, não catálogo inteiro.

**Validação:**

```bash
bash ./mvnw -Dtest=ProductServiceTest,ProductControllerTest test
```

**Conclusão:** elimina load-all para low stock na listagem HTTP.

**Review:** `code-review` sobre o diff da Tarefa 6.

---

### Tarefa 7: Histórico de movimentações paginado (endpoint novo)

**Objetivo:** expor `GET /api/v1/inventory-logs` conforme `docs/frontend-readiness.md`.

**Pré-requisito:** autorização explícita — endpoint novo.

**Arquivos:**

- Criar: `InventoryLogController.java`, `InventoryLogControllerDocs.java` (ou pacote coerente com `inventory`)
- Modificar: `InventoryLogRepository.java`, `InventoryLogService.java`, DTO/mapper se necessário
- Modificar: `SecurityConfig.java` — matchers GET para logs (`USER`, `ADMIN`)
- Criar/modificar: testes de controller e service

**Validação:**

```bash
bash ./mvnw -Dtest=*InventoryLog* test
```

**Conclusão:** listagem paginada com filtros `productId`, `batchId`, `type`, `from`, `to` e owner.

**Review:** `code-review` sobre o diff da Tarefa 7.

---

### Tarefa 8: Lotes por produto paginados (endpoint novo)

**Objetivo:** expor `GET /api/v1/products/{productId}/batches` com filtros de status e paginação.

**Pré-requisito:** autorização explícita — endpoint novo.

**Arquivos:**

- Criar ou estender controller (sub-recurso em `ProductController` ou controller dedicado — escolher um estilo e manter consistência com o projeto)
- Modificar: `BatchRepository.java`, `BatchService.java`
- Modificar: `SecurityConfig.java` se path novo
- Criar: testes MockMvc + service

**Validação:**

```bash
bash ./mvnw -Dtest=ProductControllerTest,BatchServiceTest,*Batch* test
```

(Ajustar suíte `-Dtest` ao conjunto real de classes criadas.)

**Conclusão:** produto inexistente ou sem permissão de owner retorna erro já padronizado; listagem paginada.

**Review:** `code-review` sobre o diff da Tarefa 8.

---

### Tarefa 9: Validação final e OpenAPI

**Objetivo:** suíte completa, schemas Swagger coerentes e aderência arquitetural.

**Arquivos:** correções mínimas em documentação OpenAPI ou testes somente se falhas forem causadas pelas tarefas anteriores.

**Validações obrigatórias:**

```bash
bash ./mvnw clean test
bash ./mvnw package -DskipTests
git diff --check
```

Confirmar: nenhuma listagem coberta retorna array na raiz; `size` > 100 rejeitado; `OwnerContext` preservado; JaCoCo gerado em `target/site/jacoco/`.

**Conclusão:** plano de paginação entregue para integração frontend.

**Review:** `code-review` sobre o diff acumulado das tarefas 1–8; se `CHANGES_REQUESTED`, parar sem `fix-findings`.

---

## Auto-revisão contra `docs/architecture.md`

- Contratos HTTP permanecem DTOs; entidades JPA não são expostas.
- Domínio `product` e `inventory` mantêm responsabilidades; paginação é concern transversal em `shared`.
- Regras de estoque baixo e FEFO não são reimplementadas no frontend; low stock continua calculado no backend.
- Endpoints novos de log e lotes respeitam limites do módulo `inventory` e ownership de produto.
- Mudança incremental: infra primeiro, depois listagens existentes, depois endpoints previstos na Fase 3 do readiness.

## Ordem recomendada para o frontend

1. Tarefas 1–4 (catálogo + usuários) — desbloqueiam tabelas principais.
2. Tarefas 5–6 — alertas operacionais.
3. Tarefas 7–8 — detalhe de produto e histórico (dependem de telas da Fase 3).

Este documento é o plano executável. Nenhuma tarefa deve ser implementada sem autorização explícita para a tarefa correspondente.
