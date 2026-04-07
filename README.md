# Inventory Manager — API Inteligente para Gestão de Estoque

![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring](https://img.shields.io/badge/spring-%236DB33F.svg?style=for-the-badge&logo=spring&logoColor=white)
![Docker](https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white)
![Postgres](https://img.shields.io/badge/postgres-%23316192.svg?style=for-the-badge&logo=postgresql&logoColor=white)
![Supabase](https://img.shields.io/badge/Supabase-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white)
![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=for-the-badge&logo=swagger&logoColor=white)
![Render](https://img.shields.io/badge/Render-%46E3B7.svg?style=for-the-badge&logo=render&logoColor=white)
![Apache Maven](https://img.shields.io/badge/Apache%20Maven-C71A36?style=for-the-badge&logo=Apache%20Maven&logoColor=white)
![Hibernate](https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=Hibernate&logoColor=white)
![Postman](https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white)

---

##  Visão Geral

O **Inventory Manager** é uma solução robusta para controle inteligente de estoque e gestão de lotes. Mais do que um simples registro de entradas e saídas, a API foi projetada para automatizar processos críticos, reduzir riscos operacionais e auxiliar gestores na tomada de decisão estratégica.

O diferencial deste sistema não está apenas no armazenamento de dados, mas na forma como ele processa a informação para mitigar falhas humanas comuns no controle de inventário. Através de algoritmos de monitoramento e integração de serviços de comunicação, a API transforma dados brutos em ações preventivas.

---

##  Funcionalidades Principais

**Autenticação e Autorização**: Sistema de Login e Registro de usuários com JWT, utilizando segurança stateless e controle de acesso baseado em roles - ADMIN e USER.

**Gestão Inteligente de Lotes**: Controle preciso de produtos organizado por lotes, incluindo datas de fabricação, validade e preços unitários.

**Alertas Automáticos**: Monitoramento proativo que identifica produtos abaixo do nível mínimo, gerando automaticamente relatórios em PDF e enviando alertas por e-mail para o gestor.

**Estratégia de Consumo**: Lógica de saída de estoque que prioriza automaticamente os lotes com vencimento mais próximo, minimizando perdas e desperdícios de produtos.

**Rastreabilidade**: Registro detalhado de todas as movimentações de inventário (entradas e saídas) através de logs, garantindo transparência no histórico de cada produto.

**Documentação Interativa**: Interface OpenAPI 3.0 configurada com exemplos reais, permitindo o teste imediato de todos os endpoints e facilitando a integração com o front-end.

**Infraestrutura**: Aplicação totalmente conteinerizada com Docker utilizando multi-stage builds para imagens leves, além de versionamento de banco de dados via Flyway.

**Segurança**: Senhas criptografadas (BCrypt) e uso de Constraints (Unique e Foreign Keys) para vincular corretamente movimentações de estoque, lotes e produtos.

---

## Demonstração Visual

<table width="100%">
  <tr>
    <td align="center" width="33%">
      <b>Documentação Swagger</b><br>
      <img src="https://github.com/user-attachments/assets/f898032f-63af-4885-b8bd-0408406910fa" width="100%" alt="Swagger UI">
      <p><i>Interface interativa para testes de endpoints</i></p>
    </td>
    <td align="center" width="33%">
      <b>Alerta por E-mail</b><br>
      <img src="https://github.com/user-attachments/assets/10b3b170-d52f-4a79-8ca3-6fad61722bc5" width="100%" alt="E-mail de Alerta">
      <p><i>Notificação automática de estoque crítico</i></p>
    </td>
    <td align="center" width="33%">
      <b>Relatório PDF</b><br>
      <img src="https://github.com/user-attachments/assets/a176c88b-486a-4850-82ea-fb696d522b92" width="100%" alt="Relatório PDF">
      <p><i>Documento detalhado para reposição</i></p>
    </td>
  </tr>
</table>

> O sistema utiliza tarefas agendadas (`@Scheduled`) para monitorar o inventário e disparar alertas sempre que um produto atinge o nível mínimo de segurança.

---

## Tecnologias Utilizadas

**Backend**

Java 21, Spring Boot 3, Spring Security, Spring Data JPA, Spring Mail, MapStruct, Lombok, OpenPDF, JWT, Maven

**Banco de Dados**

PostgreSQL, Flyway, Supabase, Hibernate 

**Infraestrutura**

Docker, Render, Swagger, Postman

---

## Arquitetura

A arquitetura em camadas foi escolhida para garantir separação de responsabilidades, facilitar testes e permitir evolução futura para microserviços.

1. **Controller:** Exposição dos endpoints REST ((`/api/v1/products`, `/api/v1/categories`, `/api/v1/batches`, `/api/v1/users`)).
2. **Service:** Regras de negócio, validações e cálculos.
3. **Repository:** Camada de acesso a dados (JPA/Hibernate).
4. **Security:** Filtros de segurança (JWT).
5. **DTO**: Evitando a exposição direta das entidades do banco e melhorando a segurança da API.

---

## Teste agora mesmo

A API está online e pronta para testes através da documentação interativa: 
https://inventory.hanrry.top/swagger-ui/index.html

1. Acesse o Swagger
2. Utilize o endpoint /auth/login com um usuário existente
3. Copie o token JWT retornado
4. Clique no botão "Authorize" no Swagger
5. Cole o token no formato: Bearer {token}
6. Agora você pode acessar os endpoints protegidos

---

## Instalação e Execução

### Pré-requisitos
- Docker e Docker compose instalados 
- Java 21 (se for rodar via IDE)
- Crie um arquivo `.env` na raiz do projeto com as seguintes chaves:
  
| Variável | Descrição |
| :--- | :--- |
| **DB_URL** | URL de conexão com o banco PostgreSQL |
| **DB_USERNAME** | Usuário do banco de dados |
| **DB_PASSWORD** | Senha do banco de dados |
| **EMAIL_USER** | E-mail de origem para os alertas |
| **EMAIL_CODE** | Senha de app para autenticação |
| **JWT_SECRET** | Chave secreta para assinatura dos tokens |
| **JWT_EXPIRATION** | Tempo de expiração do token |

### 1. Clone o repositório
```bash
git clone https://github.com/hanrrysantos/Inventory-Manager
cd inventory-manager
```
### 2. Execute via Docker
```bash
docker-compose up -d
```

## Estrutura de Dados

A modelagem foi desenhada para suportar alta integridade de dados e rastreabilidade total das movimentações de estoque. O banco de dados utiliza restrições de integridade (Constraints), chaves estrangeiras (FK) e índices únicos para evitar inconsistências.

<div align="center">
<img width="500" height="500" alt="image" src="https://github.com/user-attachments/assets/c12bc591-4048-4fee-9542-3d4f419cc480">
</div>

**Rastreabilidade**: A tabela tb_inventory_logs mantém um histórico imutável de todas as entradas e saídas, vinculando cada movimentação a um produto e, opcionalmente, a um lote específico.

**Gestão por Lotes**: A relação entre tb_products e tb_batches permite o controle de validade e custo médio, essencial para a estratégia de consumo FEFO implementada.

**Normalização**: Separação clara entre categorias e produtos para facilitar a escalabilidade e filtros de busca.

---

## Autor

**Hanrry** Desenvolvedor Backend Java em formação | Foco em Spring Boot e Arquitetura de Software

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/hanrrysantos)
[![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/hanrrysantos)

---

## 📜 Licença

Este projeto é licenciado sob a Apache License 2.0.

![Licença](https://img.shields.io/badge/license-Apache%202.0-blue?style=for-the-badge)
![Versão](https://img.shields.io/badge/version-v2.0-green?style=for-the-badge)
![Status](https://img.shields.io/badge/status-conclu%C3%ADdo-brightgreen?style=for-the-badge)
