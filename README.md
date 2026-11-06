# Spring microservices project

Gradle composite builds for IAM, Order, Customer, Business, Payment and Gateway services, with shared libraries and
local infrastructure definitions. The completed IAM assignment is documented in its [service guide](microservices/iam-service/README.md).

## IAM entry points

- [Setup, required external configuration and run commands](microservices/iam-service/README.md)
- [Final delivery audit and verification evidence](microservices/iam-service/DELIVERY-AUDIT.md)
- [Manual live-provider acceptance](microservices/iam-service/LIVE-PROVIDER-ACCEPTANCE.md)
- [Assignment checklist and original PDF transcription](microservices/iam-service/ASSIGNMENT-CHECKLIST.md)
- [Postman collections and usage](postman/README.md)
- [Local dependencies](deployment/README.md)

From this repository root:

```sh
./gradlew -p microservices/iam-service check bootJar
python3 microservices/iam-service/scripts/audit-delivery.py
```

Use Java 17. Supply IAM runtime configuration externally before starting the application; a key-only template is at
microservices/iam-service/runtime.properties.example. Existing protected .local files are Git-ignored and must not be
overwritten or committed. The IAM server defaults to port 9000 and uses PostgreSQL on port 9543.

The final IAM checks passed on H2 and a disposable PostgreSQL database, and its Postman requests were exercised against
an isolated packaged app. The other services have their separate collection; this IAM audit does not claim a fresh
end-to-end run of every microservice. No run-all.sh script is present; start the needed services/dependencies explicitly.
