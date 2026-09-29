# Documentação específica do backend

Guia e referências **locais** da API Spring Boot. Comportamento esperado,
specs, planos e arquitetura do sistema ficam em [`../../docs/`](../../docs/).

## O que documentar aqui

- convenções de pacotes Java além do resumo em `docs/architecture/`;
- notas sobre Flyway, perfis Spring e variáveis de ambiente da API;
- runbooks de Docker Compose local;
- geração ou consumo de OpenAPI a partir dos controllers;
- detalhes de testes de integração (Testcontainers, etc.).

## O que não duplicar aqui

- regras de negócio e critérios de aceitação → [`../../docs/specs/`](../../docs/specs/);
- planos de implementação → [`../../docs/plans/`](../../docs/plans/);
- decisões transversais → [`../../docs/adr/`](../../docs/adr/).

Início rápido da API: [`../README.md`](../README.md).
