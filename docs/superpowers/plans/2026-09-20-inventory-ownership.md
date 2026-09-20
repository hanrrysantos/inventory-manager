# Inventory Ownership Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Isolar catálogo e estoque por conta autenticada usando `owner_id`.

**Architecture:** `User` é o proprietário de `Category` e `Product`; `Batch` e
`InventoryLog` são protegidos pela relação com `Product`. Controllers obtêm a
identidade do JWT e serviços usam `ownerId` em todas as leituras e mutações.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, Flyway, PostgreSQL,
JUnit 5, Mockito e MockMvc.

**Spec:** `docs/superpowers/specs/2026-09-20-inventory-ownership-design.md`

## Global Constraints

- Não aceitar `ownerId` em payloads HTTP.
- Não editar migrations existentes; criar uma nova migration.
- Manter endpoints e algoritmo FEFO existentes.
- Recurso de outro proprietário deve parecer inexistente (`404`).

## Review Focus

- SKU e nome de categoria iguais devem poder existir em contas diferentes.
- Um usuário não pode usar `productId` de outra conta para criar ou consumir lote.
- Dashboard e alertas não podem somar produtos de outras contas.
- Dados legados sem proprietário não podem aparecer em consultas autenticadas.
- Usuário autenticado deve continuar resolvido pelo subject de e-mail do JWT.

### Task 1: Model and migration

Modify entities and repositories, add `V4__Add_Owners_To_Inventory.sql`, and
cover owner associations plus scoped uniqueness with repository tests.

### Task 2: Product and category services/controllers

Pass authenticated owner identity from controllers to services, scope CRUD and
category lookup, and add tests for cross-account `404` and creation ownership.

### Task 3: Inventory, dashboard and notification scoping

Propagate owner through batch/FEFO/log/dashboard/alert flows, preserving
transaction behavior and adding isolation tests.

### Task 4: Full verification and review

Run focused tests, `bash ./mvnw clean test`, package, inspect migration and
diff, then perform code review.
