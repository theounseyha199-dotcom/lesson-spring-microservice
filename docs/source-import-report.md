# Missing-file import

Source: `/home/seyha/data/istad-ite-platform`.

Existing source and configuration files were preserved byte for byte, except for adding the required `api 'io.projectreactor:reactor-core'` dependency to the order application-service build. Only absent, compatible files were copied. Existing files with different contents were not merged.

## Copied (27)

- `deployment/mongodb/docker-compose.yaml`
- `istad-common/istad-common-webflux/src/main/resources/application.properties`
- `microservices/customer-service/customer-service-persistence/src/main/java/kh/edu/istad/platform/customer/persistence/config/AuditorAwareConfig.java`
- `microservices/customer-service/customer-service-persistence/src/main/java/kh/edu/istad/platform/customer/persistence/config/MongoConfig.java`
- `microservices/customer-service/customer-service-persistence/src/main/java/kh/edu/istad/platform/customer/persistence/convert/CustomerIdReadConverter.java`
- `microservices/customer-service/customer-service-persistence/src/main/java/kh/edu/istad/platform/customer/persistence/convert/CustomerIdWriteConverter.java`
- `microservices/customer-service/customer-service-persistence/src/main/java/kh/edu/istad/platform/customer/persistence/convert/EmailReadConverter.java`
- `microservices/customer-service/customer-service-persistence/src/main/java/kh/edu/istad/platform/customer/persistence/convert/EmailWriteConverter.java`
- `microservices/customer-service/customer-service-restapi/src/main/java/kh/edu/istad/platform/customer/restapi/dto/CreateCustomerRequest.java`
- `microservices/gradle/wrapper/gradle-wrapper.jar`
- `microservices/gradle/wrapper/gradle-wrapper.properties`
- `microservices/gradlew`
- `microservices/gradlew.bat`
- `microservices/order-service/order-service-client/src/main/java/kh/edu/istad/platform/order/client/httpclient/BusinessHttpClient.java`
- `microservices/order-service/order-service-domain/order-application-service/src/main/java/kh/edu/istad/platform/order/domain/command/CommandOrderAddress.java`
- `microservices/order-service/order-service-domain/order-application-service/src/main/java/kh/edu/istad/platform/order/domain/command/CommandOrderItem.java`
- `microservices/order-service/order-service-domain/order-application-service/src/main/java/kh/edu/istad/platform/order/domain/command/CreateOrderCommand.java`
- `microservices/order-service/order-service-domain/order-application-service/src/main/java/kh/edu/istad/platform/order/domain/command/CreateOrderCommandHandler.java`
- `microservices/order-service/order-service-domain/order-application-service/src/main/java/kh/edu/istad/platform/order/domain/command/CreateOrderResult.java`
- `microservices/order-service/order-service-domain/order-application-service/src/main/java/kh/edu/istad/platform/order/domain/dto/QueryCustomerResult.java`
- `microservices/order-service/order-service-domain/order-application-service/src/main/java/kh/edu/istad/platform/order/domain/port/out/client/CustomerServiceClient.java`
- `microservices/order-service/order-service-domain/order-application-service/src/main/java/kh/edu/istad/platform/order/domain/port/out/repository/BusinessRepository.java`
- `microservices/order-service/order-service-domain/order-domain-core/src/main/java/kh/edu/istad/platform/order/domain/valueobject/OrderItemId.java`
- `microservices/order-service/order-service-main/src/main/java/kh/edu/istad/platform/order/exception/GlobalExceptionHandler.java`
- `microservices/order-service/order-service-main/src/test/java/kh/edu/istad/platform/order/package-info.java`
- `microservices/order-service/order-service-persistence/src/main/java/kh/edu/istad/platform/order/persistence/adapter/BusinessRepositoryAdapter.java`
- `microservices/order-service/order-service-persistence/src/main/java/kh/edu/istad/platform/order/persistence/repository/OrderReactiveRepository.java`

## Skipped (36)

- `.idea/.name`: Local IDE settings.
- `.idea/diff-generator.xml`: Local IDE settings.
- `.idea/doNotAskNotifications.xml`: Local IDE settings.
- `.idea/jpb-settings.xml`: Local IDE settings.
- `istad-common/istad-common-swagger/src/main/java/kh/edu/istad/common/swagger/config/OpenApiConfig.java`: OpenApiConfig already exists under kh/edu/istad/swagger.
- `istad-common/istad-common-webflux/src/test/java/kh/edu/istad/common/webflux/IstadCommonWebfluxApplicationTests.java`: Source context test has no Spring Boot application configuration in this library.
- `microservices/business-service/src/main/java/kh/edu/istad/platform/business/BusinessApplicationService.java`: Business application, controller and configuration already exist in business-service-main.
- `microservices/business-service/src/main/java/kh/edu/istad/platform/business/DemoController.java`: Business application, controller and configuration already exist in business-service-main.
- `microservices/business-service/src/main/resources/application.yaml`: Business application, controller and configuration already exist in business-service-main.
- `microservices/customer-service/customer-service-domain/customer-application-service/src/main/java/kh/edu/istad/platform/customer/domain/usecase/CreateCustomerUseCase.java`: Source requires reactive CustomerRepository methods absent from current synchronous port; creation and listing already exist.
- `microservices/customer-service/customer-service-domain/customer-application-service/src/main/java/kh/edu/istad/platform/customer/domain/usecase/QueryCustomerUseCase.java`: Source requires reactive CustomerRepository methods absent from current synchronous port; creation and listing already exist.
- `microservices/customer-service/customer-service-persistence/src/main/java/kh/edu/istad/platform/customer/persistence/adapter/CustomerReactiveRepositoryAdapter.java`: Source requires a reactive repository contract and mutable document; current project uses a synchronous port and record document.
- `microservices/customer-service/customer-service-persistence/src/main/java/kh/edu/istad/platform/customer/persistence/adapter/CustomerRepositoryAdapter.java`: Source file contains only commented-out obsolete JPA code.
- `microservices/customer-service/customer-service-persistence/src/main/java/kh/edu/istad/platform/customer/persistence/entity/CustomerEntity.java`: Source file contains only commented-out obsolete JPA code.
- `microservices/customer-service/customer-service-persistence/src/main/java/kh/edu/istad/platform/customer/persistence/mapper/CustomerPersistenceMapper.java`: Source requires a reactive repository contract and mutable document; current project uses a synchronous port and record document.
- `microservices/customer-service/customer-service-persistence/src/main/java/kh/edu/istad/platform/customer/persistence/repository/CustomerJpaRepository.java`: Source file contains only commented-out obsolete JPA code.
- `microservices/gateway-server/src/main/java/kh/edu/istad/platform/gateway/GatewayServerApplication.java`: Gateway application and tests already exist under istad/ha/gatewayserver.
- `microservices/gateway-server/src/test/java/kh/edu/istad/platform/gateway/GatewayServerApplicationTests.java`: Gateway application and tests already exist under istad/ha/gatewayserver.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/IamServiceApplication.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/config/PasswordConfig.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/config/SecurityConfig.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/config/TokenConfig.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/config/UserDetailsServiceImpl.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/domain/Permission.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/domain/Role.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/domain/User.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/feature/permission/PermissionJpaRepository.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/feature/role/RoleJpaRepository.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/feature/user/UserJpaRepository.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/init/InitCore.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/java/kh/edu/istad/iam/init/InitRegisteredClient.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/resources/db/changelog/ddl/001-create-authorization-table.sql`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/main/resources/db/changelog/ddl/002-create-authorization-consent-table.sql`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/iam-service/src/test/java/kh/edu/istad/iam/IamServiceApplicationTests.java`: IAM implementation already exists under istad/ha/iamservice; alternate entities and migrations would duplicate it.
- `microservices/order-service/order-service-persistence/src/test/java/kh/edu/istad/platform/order/persistence/OrderR2dbcRepositoryTest.java`: Source integration test requires an external database and reactor-test dependency absent from this module.
- `microservices/order-service/order-service-persistence/src/test/java/kh/edu/istad/platform/order/persistence/PersistenceTestConfig.java`: Source integration test requires an external database and reactor-test dependency absent from this module.

The imported order command handler and business HTTP client are scaffolding from the source, including its unfinished implementation.

## Validation

Customer and order services: `compileJava compileTestJava --offline` passed.

Customer service: `test --offline` passed.

Order service: 7 main-module tests ran; 6 passed, and the existing `BusinessRepositoryTest.findAllBusinesses` failed because PostgreSQL refused the connection during Liquibase initialization. The full order test task did not complete.
