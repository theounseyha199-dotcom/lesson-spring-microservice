# Local run guide

All four services use MongoDB for persistence and Consul for discovery.

## Start infrastructure

From the repository root:

```bash
docker network create ite-net # once, if missing
docker compose -f deployment/docker-compose.yaml up -d mongodb
docker compose -f deployment/consul/docker-compose.yaml up -d
```

MongoDB defaults to localhost:2026. Override each service connection with
`ORDER_MONGO_URI`, `CUSTOMER_MONGO_URI`, `BUSINESS_MONGO_URI`, or `PAYMENT_MONGO_URI`.
Each service uses its own database. Existing SQL data is not migrated automatically.
Consul defaults to localhost:8500 (`CONSUL_HOST` and `CONSUL_PORT`).
See the [Consul guide](consul/README.md) for host/container networking.

## Build and run

From the repository root, `./gradlew build` builds every service and runs all tests;
`./gradlew test` runs all service tests. Individual builds are also supported:

```bash
./gradlew -p microservices/order-service test :order-service-main:bootJar
./gradlew -p microservices/business-service test :business-service-main:bootJar
./gradlew -p microservices/payment-service test :payment-service-main:bootJar
./gradlew -p microservices/customer-service test :customer-service-main:bootJar
```

All four services register with Consul.

Run each jar in its own terminal:

```bash
java -jar microservices/order-service/order-service-main/build/libs/order-service-main-1.0.0.jar
java -jar microservices/business-service/business-service-main/build/libs/business-service-main-1.0.0.jar
java -jar microservices/payment-service/payment-service-main/build/libs/payment-service-main-1.0.0.jar
java -jar microservices/customer-service/customer-service-main/build/libs/customer-service-main-1.0.0.jar
```

Default HTTP ports are 8081 (order), 8082 (business), 8083 (payment), and
8084 (customer). Each can be overridden with `ORDER_PORT`, `BUSINESS_PORT`,
`PAYMENT_PORT`, or `CUSTOMER_PORT`.

## Create requests

Create a business:

```bash
curl -i -X POST http://localhost:8082/api/businesses \
  -H 'Content-Type: application/json' \
  -d '{"name":"Sokha Market","email":"hello@sokhamarket.example","address":"Phnom Penh"}'
```

Create an order, using the returned business ID and a customer UUID:

```bash
curl -i -X POST http://localhost:8081/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"11111111-1111-1111-1111-111111111111","businessId":"22222222-2222-2222-2222-222222222222","deliveryAddress":"Phnom Penh","items":[{"productId":"33333333-3333-3333-3333-333333333333","quantity":2,"unitPrice":5.00}]}'
```

Register a pending payment, using the returned order ID:

```bash
curl -i -X POST http://localhost:8083/api/payments \
  -H 'Content-Type: application/json' \
  -d '{"orderId":"44444444-4444-4444-4444-444444444444","customerId":"11111111-1111-1111-1111-111111111111","amount":10.00}'
```

The payment endpoint records a `PENDING` payment. It does not charge a card or
complete a payment. The standalone create endpoints accept IDs supplied in requests. Use the new
`POST /api/checkout` endpoint on order-service for cross-service customer and
business validation followed by order persistence and payment registration.
See [the checkout guide](../docs/checkout-workflow.md).
