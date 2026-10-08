# Customer service

Create a PostgreSQL database named `istad_customer`. The database user must be able to create tables.

Set these variables before running the service:

```bash
export CUSTOMER_DB_URL=jdbc:postgresql://localhost:5432/istad_customer
export CUSTOMER_DB_USERNAME=your_user
export CUSTOMER_DB_PASSWORD=your_password
./gradlew -p microservices/customer-service :customer-service-main:bootRun
```

For IntelliJ runs from the project root, the service can also read `microservices/customer-service/.env.local`. Put the same three `KEY=value` lines in that file. It is ignored by Git.

The service creates the `customers` table from `schema.sql` on startup. `POST /api/customers` creates a customer; `PUT /api/customers/{customerId}` updates the customer's names.
