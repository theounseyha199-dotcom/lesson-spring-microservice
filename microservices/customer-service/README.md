# Customer service

Uses MongoDB and Consul. See [the run guide](../../deployment/README.md).
The HTTP port defaults to 8084 (`CUSTOMER_PORT`). Set `CUSTOMER_MONGO_URI`
to override the connection to the `istad_customer` database.

```bash
./gradlew -p microservices/customer-service test :customer-service-main:bootJar
java -jar microservices/customer-service/customer-service-main/build/libs/customer-service-main-1.0.0.jar
```
