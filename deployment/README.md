# Microservices assignment: local run guide

The order service uses Spring WebMVC, Spring Data JPA, and PostgreSQL, as required
by the assignment. The business service uses Spring WebFlux and Reactive MongoDB.
Payment registration uses WebMVC and MongoDB.

## Start databases

The Compose network is external. Create it once if it does not already exist:

```bash
docker network create ite-net
docker compose up -d postgres mongodb
```

When the applications run on the host, order connects to PostgreSQL on
`localhost:5433`; business, payment, and customer connect to MongoDB on `localhost:2026`.
Override the order connection with `ORDER_DB_URL`, `ORDER_DB_USERNAME`, and
`ORDER_DB_PASSWORD`. Override MongoDB connections with `BUSINESS_MONGO_URI` or
`PAYMENT_MONGO_URI` or `CUSTOMER_MONGO_URI`. Containers on `ite-net` should use `postgres:5432` and
`mongodb:27017` instead of the host addresses.

## Build and run

From the repository root:

```bash
./gradlew -p microservices/order-service test :order-service-main:bootJar
./gradlew -p microservices/business-service test :business-service-main:bootJar
./gradlew -p microservices/payment-service test :payment-service-main:bootJar
./gradlew -p microservices/customer-service test :customer-service-main:bootJar
```

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
complete a payment. The services currently accept IDs supplied in requests;
they do not yet verify referenced records across service boundaries.
