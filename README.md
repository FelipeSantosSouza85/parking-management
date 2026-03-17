# Parking Garage Management System

Backend service for managing parking garage operations, including vehicle entry and exit processing, parking spot allocation, and revenue calculation.

The service integrates with an external garage simulator that emits parking events through a webhook interface. **The garage simulator is required** for the application to function correctly—it provides sector/spot configuration and emits the parking lifecycle events. The system maintains the internal state of the garage, applies pricing and occupancy business rules, and exposes a REST API to query revenue by date and sector.

## Features

- **Event processing**: ENTRY (vehicle entry), PARKED (parking confirmation), and EXIT (vehicle exit)
- **Spot management**: occupancy control by sector and global capacity
- **Dynamic pricing**: price adjustment based on occupancy at entry time
- **Revenue API**: aggregated revenue query by sector and date

## Tech Stack

- Java 21
- Spring Boot 3.5
- Spring Data JPA
- MySQL 8
- SpringDoc OpenAPI (Swagger UI)
- Testcontainers (integration tests)
- Docker Compose (local environment)

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker and Docker Compose (for MySQL and garage simulator)

## How to Run

### Option 1: With Docker Compose

Start MySQL and the garage simulator:

```bash
docker-compose up -d
```

Start the application:

```bash
./mvnw spring-boot:run
```

### Option 2: Local MySQL

If MySQL is already running locally, adjust `src/main/resources/application.yml` as needed and run:

```bash
./mvnw spring-boot:run
```

### Garage Simulator (Required)

The garage simulator must be running for the application to work. When using Docker Compose (Option 1), it is started automatically. To run it standalone:

```bash
docker run -d --network="host" cfontes0estapar/garage-sim:1.0.0
```

> **Note (Docker Desktop):** Host networking may be disabled by default. If the container runs but webhook/garage initialization communication fails, enable host networking in Docker Desktop settings. Without this enabled, the simulator cannot reach the application on the host.

### URLs

| Resource | URL |
|----------|-----|
| API | http://localhost:3003 |
| Swagger UI | http://localhost:3003/swagger-ui.html |
| Garage simulator | http://localhost:3000 |

## API Endpoints

### POST /webhook

Receives events from the external simulator. Unified payload with conditional validation by `event_type`.

**ENTRY**
```json
{
  "license_plate": "ZUL0001",
  "entry_time": "2025-01-01T12:00:00",
  "event_type": "ENTRY"
}
```

**PARKED**
```json
{
  "license_plate": "ZUL0001",
  "lat": -23.561684,
  "lng": -46.655981,
  "event_type": "PARKED"
}
```

**EXIT**
```json
{
  "license_plate": "ZUL0001",
  "exit_time": "2025-01-01T14:00:00",
  "event_type": "EXIT"
}
```

**Responses**: 200 (success), 400 (invalid payload), 404 (resource not found), 409 (business rule violated)

### GET /revenue

Query aggregated revenue by sector and date.

**Parameters**
- `date` (required): date in YYYY-MM-DD format
- `sector` (required): sector code

**Example**
```http
GET /revenue?date=2025-01-01&sector=A
```

**Response**
```json
{
  "amount": 150.00,
  "currency": "BRL",
  "timestamp": "2025-01-01T23:59:59Z"
}
```

## Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | 3003 | Application port |
| `spring.datasource.url` | jdbc:mysql://localhost:3307/parking_management | MySQL URL |
| `garage.simulator.base-url` | http://localhost:3000 | Garage simulator URL |

## Architecture

The project follows a **modular monolith** organized by business domain:

- **garage**: configuration sync, sectors, spots, and occupancy
- **webhook**: event ingestion endpoint and dispatch
- **parking**: parking sessions, pricing rules
- **revenue**: aggregated revenue query
- **shared**: exceptions, configuration, and utilities

Each module separates responsibilities across `api`, `application`, `domain`, and `infrastructure`. The domain remains free of JPA; persistence uses Ports/Adapters.

## Testing

```bash
./mvnw test
```

- **Testcontainers + MySQL**: integration tests with real database
- **MockMvc**: HTTP API tests
