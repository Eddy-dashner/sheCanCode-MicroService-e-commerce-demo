# Microservices E-Commerce Platform (Learning Build)

An e-commerce platform built as independent Spring Boot microservices. Each
service owns its own database, its own deployable JAR, and its own Dockerfile —
**no service ever reads another service's database.**

This repo is built in strict, gated phases 

## Current status: Phase 5 — order-service + checkout saga ✅

The full **choreographed checkout saga** (without payment yet). Placing an order
starts it; services react to each other's events with no central coordinator.

```
POST /orders ──► Order CREATED ──► OrderCreated event
                                         │
                              inventory reserves stock
                       ┌─────────────────┴─────────────────┐
              StockReserved                        StockReservationFailed
                    │                                       │
            Order CONFIRMED                          Order CANCELLED
            + OrderConfirmed ──► inventory              + OrderCancelled ──► inventory
              finalises reservation                       releases stock (compensation)
```

Order placement makes the system's canonical **sync call**: order → product for
the authoritative price (circuit-breaker fallback fails *loud* — no order at an
unknown price).

```bash
# Place an order (needs a token). Returns 201 immediately as CREATED.
curl -X POST http://localhost:8080/orders -H "Authorization: Bearer <token>" \
  -H 'Content-Type: application/json' \
  -d '{"lines":[{"productId":"<uuid>","quantity":2}]}'

# Poll the order to watch the saga drive it to CONFIRMED (or CANCELLED)
curl http://localhost:8080/orders/<id> -H "Authorization: Bearer <token>"
```

## Phase 4 — inventory-service ✅

Stock levels + reservations, driven by **Kafka events** (the async backbone).
Introduces the `common-events` module (event DTOs only). inventory-service
consumes `OrderCreated`/`OrderCancelled` and publishes `StockReserved` /
`StockReservationFailed` / `StockReleased` / `LowStock`. Consumers are
**idempotent** via a `processed_events` ledger.

REST is used only for stock reads/seeding — reservations happen via events.

```bash
# Seed stock (admin)
curl -X POST http://localhost:8080/inventory -H "Authorization: Bearer <token>" \
  -H 'Content-Type: application/json' \
  -d '{"productId":"<uuid>","quantity":10,"lowStockThreshold":2}'

# Read stock
curl http://localhost:8080/inventory/<productId> -H "Authorization: Bearer <token>"
```

## Phase 3 — product-service ✅

Catalogue CRUD + the authoritative price endpoint, plus the platform's **first
synchronous inter-service call**: `product-service → user-service` via OpenFeign,
wrapped in a Resilience4j circuit breaker with timeout, retry and fallback.

### Demonstrate the circuit breaker opening (once running)

```bash
# Create a product (needs a token from login; POST is protected)
curl -X POST http://localhost:8080/products -H "Authorization: Bearer <token>" \
  -H 'Content-Type: application/json' -d '{"name":"Mug","price":12.50}'

# Its creator, fetched live from user-service
curl http://localhost:8080/products/<id>/creator

# Now STOP user-service and hammer the endpoint ~6 times:
docker compose stop user-service
for i in $(seq 1 6); do curl -s http://localhost:8080/products/<id>/creator; echo; done
# First calls fail slowly, then the circuit OPENS and responses become instant
# "unknown" fallbacks. Watch the state flip to OPEN:
curl http://localhost:8082/actuator/circuitbreakers
```

## Phase 2 — user-service ✅

user-service is a full vertical slice: register, login (JWT issuance), and a
gateway-protected `/users/me`. The gateway now validates JWTs at the edge and
forwards a trusted `X-User-Id` header to the service.

### Try the user flow (once running)

```bash
# Register (public)
curl -X POST http://localhost:8080/users -H 'Content-Type: application/json' \
  -d '{"email":"a@b.com","password":"password123","firstName":"A","lastName":"B"}'

# Login -> copy the accessToken
curl -X POST http://localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"a@b.com","password":"password123"}'

# Call a protected endpoint through the gateway
curl http://localhost:8080/users/me -H "Authorization: Bearer <token>"

# Without a token -> 401 from the gateway
curl -i http://localhost:8080/users/me
```

OpenAPI/Swagger UI: http://localhost:8081/swagger-ui.html

## Phase 1 — Skeleton ✅

The shared infrastructure and the data/messaging backbone are wired end to end.
No business services exist yet.

## Tech stack

- Java 21, Spring Boot 3.3, Spring Cloud 2023.0
- Maven multi-module repo
- PostgreSQL (one database per service), Kafka (async events)
- Spring Cloud Config, Netflix Eureka (discovery), Spring Cloud Gateway
- Docker Compose for local orchestration

## Modules

| Module | Port | Role |
|---|---|---|
| `config-server` | 8888 | Centralised configuration for all services |
| `discovery-server` | 8761 | Eureka service registry |
| `api-gateway` | 8080 | Single entry point; discovery-based routing |

Boot order: **config-server → discovery-server → api-gateway** (each waits for
the previous to be healthy).

## Run it

```bash
docker compose up --build
```

Then verify the wiring:

```bash
# 1. Each infra service is healthy
curl http://localhost:8888/actuator/health   # config-server
curl http://localhost:8761/actuator/health   # discovery-server
curl http://localhost:8080/actuator/health   # api-gateway

# 2. Config-server is actually serving centralised config to others
curl http://localhost:8888/api-gateway/default

# 3. END-TO-END proof: hit the gateway, which looks the target up in Eureka
#    (lb://DISCOVERY-SERVER) and proxies to it. A UP response means
#    gateway + discovery + routing all work together.
curl http://localhost:8080/wiring-check/health

# 4. See the registry UI (api-gateway registered here)
open http://localhost:8761
```

## Build / test locally (without Docker)

```bash
./mvnw clean package        # compiles all modules, runs tests
```

## Design docs

Phase 0 design (context map, event catalogue, order state machine) lives in the
conversation history / project notes. The saga is **choreographed** and uses **Kafka**.
