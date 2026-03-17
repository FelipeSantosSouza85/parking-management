# Tasks - Parking Garage Management System

## Visão Geral

Este documento contém o plano de implementação do sistema de gerenciamento de estacionamento, organizado em tasks seguindo a abordagem **TDD** com **fundação primeiro**.

---

## Resumo de Progresso

| # | Task | Status |
|---|------|--------|
| 1 | Setup & Configuração Base | ⏳ Pendente |
| 2 | Módulo Shared - Utilitários | ⏳ Pendente |
| 3 | Módulo Garage - Domínio & Persistência | ⏳ Pendente |
| 4 | Módulo Garage - Sincronização | ⏳ Pendente |
| 5 | Módulo Parking - Domínio & Persistência | ⏳ Pendente |
| 6 | Módulo Parking - Regras de Precificação | ⏳ Pendente |
| 7 | Módulo Parking - Use Case ENTRY | ⏳ Pendente |
| 8 | Módulo Parking - Use Case PARKED | ⏳ Pendente |
| 9 | Módulo Parking - Use Case EXIT | ⏳ Pendente |
| 10 | Módulo Webhook | ⏳ Pendente |
| 11 | Módulo Revenue | ⏳ Pendente |
| 12 | Testes de Integração & Concorrência | ⏳ Pendente |

---

## Task 1 - Setup & Configuração Base ⏳

### Objetivo
Configurar a infraestrutura base do projeto.

### Entregáveis
- [ ] `application.yml` (datasource, JPA com ddl-auto: update, Jackson, server port)
- [ ] `application-test.yml` (Testcontainers, JPA com ddl-auto: create-drop)
- [ ] `docker-compose.yml` (MySQL + Garage Simulator)
- [ ] `GlobalExceptionHandler` + `ApiErrorResponse`
- [ ] Exceções base: `ValidationException`, `ResourceNotFoundException`, `ConflictException` (estendem `RuntimeException`)
- [ ] Remover dependências do Flyway do `pom.xml`

### Configurações Definidas
| Recurso | Configuração |
|---------|--------------|
| MySQL | `localhost:3307` (container `mysql-dev`) |
| Database | `parking_management` |
| Simulador | `http://localhost:3000` |
| Aplicação | `http://localhost:3003` |
| JPA (dev) | `ddl-auto: update` |
| JPA (test) | `ddl-auto: create-drop` |

### Nota sobre Schema
O Hibernate gerenciará automaticamente a criação e atualização das tabelas através da propriedade `ddl-auto`. Não será utilizado Flyway para migrations.

---

## Task 2 - Módulo Shared - Exceções de Domínio ⏳

### Objetivo
Implementar exceções específicas de domínio para o fluxo de parking.

### Entregáveis
- [ ] `GarageFullException` (extends `ConflictException`)
- [ ] `SpotAlreadyOccupiedException` (extends `ConflictException`)
- [ ] `SpotNotFoundException` (extends `ResourceNotFoundException`)
- [ ] `ActiveSessionNotFoundException` (extends `ResourceNotFoundException`)
- [ ] `ActiveSessionAlreadyExistsException` (extends `ConflictException`)
- [ ] `InvalidSessionTransitionException` (extends `ConflictException`)
- [ ] Atualizar `GlobalExceptionHandler` com handlers específicos
- [ ] Testes unitários para `GlobalExceptionHandler`

### Nota de Design
A lógica de cálculo de tempo (tolerância 30 min, arredondamento de horas) e operações monetárias será incorporada diretamente no `PricingCalculator` (Task 6), pois são regras de negócio específicas de precificação, não utilitários genéricos.

---

## Task 3 - Módulo Garage - Domínio & Persistência ✅

### Objetivo
Criar as entidades e persistência do módulo Garage com separação de camadas.

### Entregáveis
- [x] Domínio (POJO puro): `GarageSector`, `ParkingSpot`, `GarageOccupancy`
- [x] Ports: `GarageSectorPort`, `ParkingSpotPort`, `GarageOccupancyPort`
- [x] Entidades JPA: `GarageSectorEntity`, `ParkingSpotEntity`, `GarageOccupancyEntity`
- [x] Mappers: `GarageSectorMapper`, `ParkingSpotMapper`, `GarageOccupancyMapper`
- [x] JpaRepository + Adapters (com lock pessimista onde necessário)

### Índices (via JPA annotations nas entities)
- `garage_sector`: unique on `sector_code`
- `parking_spot`: unique on `external_spot_id`, composite on `(lat, lng)`

---

## Task 4 - Módulo Garage - Sincronização ⏳

### Objetivo
Implementar a sincronização de dados do simulador externo.

### Entregáveis
- [ ] `GarageSimulatorClient` - interface declarativa REST
- [ ] DTOs de resposta do simulador (`GarageConfigurationResponse`)
- [ ] `GarageSynchronizationService` - sync de setores e spots
- [ ] `GarageInitializationService` - listener de startup (`ApplicationReadyEvent`)
- [ ] Testes de integração

### Endpoint do Simulador
```
GET http://localhost:3000/garage
```

### Payload Esperado
```json
{
  "garage": [
    {
      "sector": "A",
      "base_price": 40.5,
      "max_capacity": 10,
      "open_hour": "00:00",
      "close_hour": "23:59",
      "duration_limit_minutes": 1440
    }
  ],
  "spots": [
    {
      "id": 1,
      "sector": "A",
      "lat": -23.561684,
      "lng": -46.655981,
      "occupied": true
    }
  ]
}
```

---

## Task 5 - Módulo Parking - Domínio & Persistência ⏳

### Objetivo
Criar a entidade ParkingSession e sua persistência.

### Entregáveis
- [ ] Enum `ParkingSessionStatus` (ENTERED, PARKED, EXITED, REJECTED)
- [ ] Entidade `ParkingSession`
  - `id`, `licensePlate`, `status`
  - `entryTime`, `parkedTime`, `exitTime`
  - `parkingSpot` (nullable), `sector` (nullable)
  - `occupancyRateAtEntry`, `priceAdjustmentRateAtEntry`
  - `hourlyPriceApplied` (nullable até PARKED)
  - `chargedAmount` (nullable até EXIT)
  - `createdAt`, `updatedAt`
- [ ] `ParkingSessionRepository` (com lock pessimista e queries customizadas)

### Índices (via JPA annotations)
- Composite index on `(license_plate, status)` (`@Table(indexes)`)
- Composite index on `(sector_id, exit_time)` (`@Table(indexes)`)

---

## Task 6 - Módulo Parking - Regras de Precificação ⏳

### Objetivo
Implementar as regras de precificação dinâmica.

### Entregáveis
- [ ] `PricingAdjustmentPolicy` - tabela de ajuste por ocupação
- [ ] `PricingCalculator` - cálculo de preço final
- [ ] Testes unitários completos

### Regras de Negócio

#### Tolerância Gratuita
- Até **30 minutos**: gratuito
- Mais de 30 minutos: cobrado por hora
- Horas arredondadas **para cima**

#### Exemplos
| Duração | Horas Cobradas |
|---------|----------------|
| 20 min | 0 |
| 31 min | 1 |
| 61 min | 2 |

#### Ajuste Dinâmico por Ocupação
| Ocupação | Ajuste |
|----------|--------|
| < 25% | -10% |
| ≤ 50% | 0% |
| ≤ 75% | +10% |
| ≤ 100% | +25% |

#### Fórmula do Preço Final por Hora
```
hourlyPriceApplied = sector.basePrice * (1 + priceAdjustmentRateAtEntry)
```

#### Fórmula da Cobrança Final
```
chargedAmount = hourlyPriceApplied * chargeableHours
```

---

## Task 7 - Módulo Parking - Use Case ENTRY ⏳

### Objetivo
Implementar o processamento do evento ENTRY.

### Entregáveis
- [ ] `EntryVehicleService`
- [ ] Testes unitários
- [ ] Testes de integração

### Fluxo
1. Validar payload
2. Verificar se já existe sessão ativa para a placa
3. Lock pessimista em `GarageOccupancy`
4. Validar se a garagem está cheia
5. Calcular ajuste dinâmico baseado na ocupação global
6. Criar `ParkingSession` com status `ENTERED`
7. Incrementar `occupiedCount` em `GarageOccupancy`

### Dados Capturados
- `occupancyRateAtEntry`
- `priceAdjustmentRateAtEntry`

---

## Task 8 - Módulo Parking - Use Case PARKED ⏳

### Objetivo
Implementar o processamento do evento PARKED.

### Entregáveis
- [ ] `ParkVehicleService`
- [ ] Testes unitários
- [ ] Testes de integração

### Fluxo
1. Validar payload
2. Buscar sessão ativa por placa (com lock)
3. Buscar spot por `lat/lng` (com lock)
4. Validar que o spot existe
5. Validar que o spot está livre
6. Marcar spot como ocupado
7. Associar `spot` e `sector` à sessão
8. Definir `parkedTime`
9. Calcular e armazenar `hourlyPriceApplied`
10. Atualizar status para `PARKED`

---

## Task 9 - Módulo Parking - Use Case EXIT ⏳

### Objetivo
Implementar o processamento do evento EXIT.

### Entregáveis
- [ ] `ExitVehicleService`
- [ ] Testes unitários
- [ ] Testes de integração

### Fluxo
1. Validar payload
2. Buscar sessão ativa por placa (com lock)
3. Definir `exitTime`
4. **Se sessão está PARKED:**
   - Calcular duração
   - Calcular cobrança final
   - Liberar `ParkingSpot`
5. **Se sessão está ENTERED (sem spot):**
   - Fechar sessão com cobrança zero
6. Marcar sessão como `EXITED`
7. Decrementar `occupiedCount` em `GarageOccupancy`

### Caso Especial: Entry Sem Parking
Um veículo pode entrar e sair sem estacionar:
- Lifecycle válido: `ENTRY` → `EXIT`
- Nenhum spot é atribuído
- Nenhum setor é atribuído
- **Cobrança: R$ 0,00**

---

## Task 10 - Módulo Webhook ⏳

### Objetivo
Implementar o endpoint de webhook para receber eventos do simulador.

### Entregáveis
- [ ] `WebhookEventType` enum (ENTRY, PARKED, EXIT)
- [ ] `WebhookEventRequest` DTO com validação condicional
- [ ] `WebhookEventDispatcher` - roteamento por tipo de evento
- [ ] `WebhookController` - endpoint `POST /webhook`
- [ ] Testes de API com MockMvc

### Endpoint
```
POST http://localhost:3003/webhook
```

### Payloads

#### ENTRY
```json
{
  "license_plate": "ZUL0001",
  "entry_time": "2025-01-01T12:00:00.000Z",
  "event_type": "ENTRY"
}
```

#### PARKED
```json
{
  "license_plate": "ZUL0001",
  "lat": -23.561684,
  "lng": -46.655981,
  "event_type": "PARKED"
}
```

#### EXIT
```json
{
  "license_plate": "ZUL0001",
  "exit_time": "2025-01-01T14:00:00.000Z",
  "event_type": "EXIT"
}
```

### HTTP Status Codes
| Status | Significado |
|--------|-------------|
| 200 OK | Evento processado com sucesso |
| 400 Bad Request | Payload inválido |
| 404 Not Found | Recurso não encontrado |
| 409 Conflict | Regra de negócio ou transição inválida |
| 500 Internal Server Error | Falha inesperada |

---

## Task 11 - Módulo Revenue ⏳

### Objetivo
Implementar a API de consulta de receita.

### Entregáveis
- [ ] `RevenueSummary` (agregação)
- [ ] `RevenueQueryRepository` (query customizada)
- [ ] `RevenueQueryService`
- [ ] `RevenueResponse` DTO
- [ ] `RevenueController` - endpoint `GET /revenue`
- [ ] Testes de API com MockMvc

### Endpoint
```
GET http://localhost:3003/revenue?date=2025-01-01&sector=A
```

### Query Parameters
| Parâmetro | Obrigatório | Descrição |
|-----------|-------------|-----------|
| `date` | Sim | Data no formato `YYYY-MM-DD` |
| `sector` | Sim | Código do setor |

### Response
```json
{
  "amount": 121.50,
  "currency": "BRL",
  "timestamp": "2025-01-01T23:59:59Z"
}
```

### Regra de Agregação
Somar `charged_amount` de sessões com:
- Status = `EXITED`
- `sector_id` = setor informado
- Data de `exit_time` = data informada

---

## Task 12 - Testes de Integração & Concorrência ⏳

### Objetivo
Garantir a robustez do sistema através de testes completos.

### Entregáveis
- [ ] Testes E2E do fluxo completo (ENTRY → PARKED → EXIT)
- [ ] Teste do caso especial (ENTRY → EXIT sem PARKED)
- [ ] Teste de concorrência: dois ENTRY simultâneos com 1 vaga
- [ ] Teste de concorrência: dois PARKED para o mesmo spot
- [ ] Teste de concorrência: EXIT duplicado
- [ ] Testes de idempotência por estado de sessão

### Ferramentas
- **Testcontainers** com MySQL para testes de integração
- **MockMvc** para testes de API
- **ExecutorService** para testes de concorrência

---

## Task 13 - Documentação API com Swagger/OpenAPI ⏳

### Objetivo
Adicionar documentação interativa da API usando springdoc-openapi (Swagger UI).

### Pré-requisito
- Task 10 (Webhook) e Task 11 (Revenue) concluídas

### Entregáveis
- [ ] Dependência `springdoc-openapi-starter-webmvc-ui` no `pom.xml`
- [ ] Configuração OpenAPI (título, descrição, versão) em `shared/config/`
- [ ] Anotações `@Operation` e `@ApiResponse` em `WebhookController`
- [ ] Anotações `@Operation` e `@ApiResponse` em `RevenueController`
- [ ] Documentação dos DTOs de request/response com `@Schema`
- [ ] Swagger UI acessível em `/swagger-ui.html`

### Endpoints a documentar

#### `POST /webhook`
- Descrição dos 3 tipos de evento (ENTRY, PARKED, EXIT)
- Exemplos de payload por tipo de evento
- Códigos de resposta: 200, 400, 404, 409, 500

#### `GET /revenue`
- Query parameters `date` e `sector`
- Exemplo de resposta
- Códigos de resposta: 200, 400, 404

### Nível de documentação
- `@Operation(summary, description)` em cada endpoint
- `@ApiResponse` com código, descrição e schema para cada status HTTP
- `@Schema(description, example)` nos campos dos DTOs
- Exemplos de payload inline via `@ExampleObject`

---

## Ordem de Locks (Deadlock Prevention)

Para reduzir risco de deadlock, os recursos serão bloqueados nesta ordem:

1. `ParkingSession`
2. `ParkingSpot`
3. `GarageOccupancy`

---

## Estrutura de Pacotes Final

```
com.estapar.parking_management
├── ParkingManagementApplication.java
├── shared
│   ├── exception
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ApiErrorResponse.java
│   │   ├── ValidationException.java
│   │   ├── ResourceNotFoundException.java
│   │   ├── ConflictException.java
│   │   ├── GarageFullException.java
│   │   ├── SpotAlreadyOccupiedException.java
│   │   ├── SpotNotFoundException.java
│   │   ├── ActiveSessionNotFoundException.java
│   │   ├── ActiveSessionAlreadyExistsException.java
│   │   └── InvalidSessionTransitionException.java
├── garage
│   ├── application
│   │   ├── port
│   │   │   ├── GarageSectorPort.java
│   │   │   ├── ParkingSpotPort.java
│   │   │   └── GarageOccupancyPort.java
│   │   ├── GarageSynchronizationService.java
│   │   └── GarageInitializationService.java
│   ├── domain
│   │   ├── GarageSector.java
│   │   ├── GarageOccupancy.java
│   │   └── ParkingSpot.java
│   └── infrastructure
│       ├── client
│       │   └── GarageSimulatorClient.java
│       └── persistence
│           ├── entity
│           │   ├── GarageSectorEntity.java
│           │   ├── GarageOccupancyEntity.java
│           │   └── ParkingSpotEntity.java
│           ├── mapper
│           │   ├── GarageSectorMapper.java
│           │   ├── GarageOccupancyMapper.java
│           │   └── ParkingSpotMapper.java
│           └── repository
│               ├── GarageSectorJpaRepository.java
│               ├── GarageOccupancyJpaRepository.java
│               ├── ParkingSpotJpaRepository.java
│               └── adapter
│                   ├── GarageSectorAdapter.java
│                   ├── GarageOccupancyAdapter.java
│                   └── ParkingSpotAdapter.java
├── webhook
│   ├── api
│   │   ├── WebhookController.java
│   │   └── dto
│   │       ├── WebhookEventRequest.java
│   │       └── WebhookEventType.java
│   └── application
│       └── WebhookEventDispatcher.java
├── parking
│   ├── application
│   │   ├── EntryVehicleService.java
│   │   ├── ParkVehicleService.java
│   │   ├── ExitVehicleService.java
│   │   ├── PricingCalculator.java
│   │   └── PricingAdjustmentPolicy.java
│   ├── domain
│   │   ├── ParkingSession.java
│   │   └── ParkingSessionStatus.java
│   └── infrastructure
│       └── persistence
│           ├── entity
│           │   └── ParkingSessionEntity.java
│           ├── mapper
│           │   └── ParkingSessionMapper.java
│           └── repository
│               ├── ParkingSessionJpaRepository.java
│               └── adapter
│                   └── ParkingSessionAdapter.java
└── revenue
    ├── api
    │   ├── RevenueController.java
    │   └── dto
    │       └── RevenueResponse.java
    ├── application
    │   └── RevenueQueryService.java
    └── infrastructure
        └── persistence
            └── RevenueQueryRepository.java
```

### Separação de Camadas (Domínio, Persistência, Ports)

- **domain/**: entidades POJO puras, sem JPA — regras de negócio e invariantes
- **application/port/**: interfaces que definem contratos de persistência
- **infrastructure/persistence/entity/**: entidades JPA para mapeamento
- **infrastructure/persistence/mapper/**: conversão Entity ↔ Domain
- **infrastructure/persistence/repository/adapter/**: implementações dos Ports
