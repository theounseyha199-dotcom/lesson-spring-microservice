បាន។ Project នេះគឺជា **Java / Spring Boot Microservice Starter** ដែលរៀបចំជា **Gradle composite build** ហើយបច្ចុប្បន្ន Feature ដែលបាន implement ពេញលេញជាងគេគឺ **Customer Management**។

ខ្ញុំនឹងពន្យល់ពី flow និង architecture ឲ្យងាយយល់ ដោយទុក Technical terms សំខាន់ៗជា English។

## 1. Project នេះធ្វើអ្វី?

Project មាន Microservice សំខាន់ៗ 2៖

- `customer-service`
- `business-service`

បច្ចុប្បន្ន `customer-service` អាចធ្វើបាន 3 operations៖

1. **Create Customer**
2. **Update Customer Name**
3. **Deactivate Customer**

ចំណែក `business-service` នៅពេលនេះគ្រាន់តែជា Spring Boot Application ដែលអាច start បាន ប៉ុន្តែមិនទាន់មាន API ឬ Business Logic ទេ។

Customer data ត្រូវបានរក្សាទុកក្នុង **PostgreSQL**។

---

# 2. Architecture របស់ Customer Service

Flow សំខាន់គឺ៖

```text
Client
  ↓
REST Controller
  ↓
Request DTO
  ↓
Application Use Case
  ↓
Domain Service
  ↓
Customer Domain Model
  ↓
CustomerRepository Port
  ↓
PostgreSQL Adapter
  ↓
PostgreSQL Database
```

ន័យសាមញ្ញគឺ Client មិនអាចទៅ Database ដោយផ្ទាល់ទេ។

វាត្រូវឆ្លងកាត់ Layer មួយៗ។

ឧទាហរណ៍ Client ចង់ create customer:

```text
POST /api/customers
        ↓
CustomerController
        ↓
InitiateCustomerUseCase
        ↓
CustomerDomainService
        ↓
Customer
        ↓
CustomerRepository
        ↓
PostgresCustomerRepository
        ↓
customers table
```

Architecture នេះមាន style ជិតនឹង **Hexagonal Architecture / Clean Architecture / DDD-oriented architecture**។

ចំណុចសំខាន់គឺ៖

> Domain មិន depend លើ PostgreSQL ឬ Spring JDBC ដោយផ្ទាល់ទេ។

Domain ដឹងតែ៖

```java
CustomerRepository
```

ដែលជា **interface / port**។

ចំណែក PostgreSQL implementation នៅ Infrastructure/Persistence Layer។

---

# 3. Folder Structure

Project root:

```text
istad-ite-1/
```

នៅក្នុងនេះមាន៖

```text
build-logic/
gradle/
istad-common/
microservices/
```

## `build-logic`

នេះសម្រាប់ **shared Gradle configuration**។

ឧទាហរណ៍ project មាន modules ច្រើន។ ជំនួសឲ្យសរសេរ configuration ដូចគ្នាគ្រប់ module យើងអាចដាក់ common configuration នៅ `build-logic`។

ដូចជា៖

```text
Java version
Spring Boot dependencies
Compiler configuration
Testing configuration
```

Project នេះប្រើ៖

```text
Java 25
Spring Boot 4.1.1
```

---

# 4. `istad-common`

នេះជា code ដែលអាច reuse ដោយ Microservices ផ្សេងៗ។

```text
istad-common/
├── istad-common-domain/
└── istad-common-restapi/
```

## `istad-common-domain`

មាន common Domain concepts ដូចជា៖

```text
BaseEntity
AggregateRoot
CustomerId
DomainEvent
Money
Exceptions
```

ឧទាហរណ៍ `CustomerId` អាចត្រូវ reuse នៅ Customer domain។

## `istad-common-restapi`

បច្ចុប្បន្ន module នេះនៅទទេ។

នៅពេលក្រោយ អាចដាក់ common REST things ដូចជា៖

```text
ErrorResponse
GlobalExceptionHandler
PaginationResponse
API Response
```

---

# 5. `microservices`

មាន៖

```text
microservices/
├── business-service/
└── customer-service/
```

## Business Service

`business-service` បច្ចុប្បន្នមានតែ Spring Boot application។

វារត់លើ៖

```text
Port 9090
```

មិនទាន់មាន៖

```text
Controller
Use Case
Domain
Database
API
```

---

# 6. Customer Service

Customer Service ជា service ដែលបាន implement ច្រើន។

Structure:

```text
customer-service/
├── customer-service-main/
├── customer-service-restapi/
├── customer-service-domain/
├── customer-service-persistence/
└── customer-service-message/
```

មើលវាជា Architecture Layer បានដូចនេះ៖

```text
REST API
      ↓
Application
      ↓
Domain
      ↓
Repository Port
      ↓
Persistence Adapter
```

---

# 7. `customer-service-main`

នេះគឺជា **Application Bootstrap / Composition Root**។

មាន class សំខាន់៖

```text
CustomerApplicationService.java
BeanConfig.java
```

## CustomerApplicationService

វាជា Spring Boot main class។

Concept ប្រហែលជា៖

```java
@SpringBootApplication
public class CustomerApplicationService {

    public static void main(String[] args) {
        SpringApplication.run(
            CustomerApplicationService.class,
            args
        );
    }
}
```

វាជា entry point សម្រាប់ start Customer Service។

Customer service run លើ៖

```text
Port 8282
```

---

# 8. `BeanConfig.java`

`BeanConfig` ប្រើសម្រាប់ configure Spring Bean។

ឧទាហរណ៍៖

```java
@Bean
CustomerDomainService customerDomainService() {
    return new CustomerDomainServiceImpl();
}
```

មូលហេតុដែលធ្វើបែបនេះគឺ Domain Layer មិនចាំបាច់ដាក់ Spring annotation ដូចជា៖

```java
@Service
@Component
```

វាជួយឲ្យ Domain remain independent ពី Spring Framework។

នេះជាគំនិតល្អមួយក្នុង Hexagonal Architecture។

---

# 9. REST API Layer

នៅ៖

```text
customer-service-restapi
```

មាន៖

```text
CustomerController
Request DTO
Response DTO
CustomerWebMapper
```

Responsibility របស់ REST Layer គឺ៖

```text
Receive HTTP Request
Validate Request
Convert DTO → Command
Call Use Case
Convert Result → Response
Return HTTP Response
```

វាមិនគួរដាក់ Business Logic ច្រើននៅ Controller ទេ។

---

# 10. CustomerController

`CustomerController` មាន 3 endpoints សំខាន់។

## Create

```http
POST /api/customers
```

## Update

```http
PUT /api/customers/{customerId}
```

## Deactivate

```http
PUT /api/customers/{customerId}/deactivate
```

Controller ជា **Inbound Adapter** ក្នុង Hexagonal Architecture។

ព្រោះវាទទួល request ពី External World ហើយហៅ Application។

---

# 11. DTO

ឧទាហរណ៍ Create Customer Request៖

```json
{
  "username": "sokha",
  "familyName": "Chan",
  "givenName": "Sokha",
  "email": "sokha@example.com",
  "phoneNumber": "+85512345678"
}
```

Fields ទាំង 5 មាន៖

```java
@NotBlank
```

មានន័យថា Client មិនអាចផ្ញើ៖

```json
{
  "username": ""
}
```

បានទេ។

Spring Validation នឹង reject request មុន Business Logic។

---

# 12. CustomerWebMapper

Project ប្រើ **MapStruct**។

Mapper មាន role៖

```text
HTTP DTO
   ↓
Application Command
```

និង

```text
Application Result
   ↓
HTTP Response DTO
```

ឧទាហរណ៍៖

```text
CustomerInitiateRequest
       ↓
InitiateCustomerCommand
```

បន្ទាប់ពី Use Case complete៖

```text
InitiateCustomerResult
       ↓
CustomerResponse
```

អត្ថប្រយោជន៍គឺ Controller មិនចាំបាច់សរសេរ mapping code ច្រើន។

---

# 13. Application Layer

Application Layer មាន Use Cases៖

```text
InitiateCustomerUseCase
UpdateCustomerUseCase
DeactivateCustomerUseCase
```

Use Case មាន responsibility ក្នុងការរៀបចំ workflow។

Application Layer មិនមែនជាកន្លែងសម្រាប់ core Business Rule ទេ។

វាធ្វើ orchestration ប្រហែល៖

```text
Receive Command
    ↓
Load/Create Domain Object
    ↓
Call Domain Service
    ↓
Save Repository
    ↓
Return Result
```

---

# 14. InitiateCustomerUseCase

Flow៖

```text
InitiateCustomerCommand
       ↓
Create Customer object
       ↓
CustomerDomainService
       ↓
Customer.initiateCustomer()
       ↓
Repository.save()
       ↓
InitiateCustomerResult
```

នេះគឺជា Application Use Case។

---

# 15. Domain Layer

Domain Core មាន៖

```text
Customer
CustomerStatus
CustomerId
Email
PhoneNumber
CustomerDomainService
Domain Events
```

នេះគឺជា **heart/core of the system**។

Business Rules សំខាន់ៗគួរត្រូវនៅទីនេះ។

---

# 16. Customer Entity

Customer មាន fields៖

```text
customerId
username
familyName
givenName
email
phoneNumber
status
```

ឧទាហរណ៍៖

```text
Customer
├── CustomerId
├── username
├── familyName
├── givenName
├── Email
├── PhoneNumber
└── CustomerStatus
```

Customer គឺជា **Entity** ព្រោះវាមាន Identity។

ឧទាហរណ៍ Customer A អាចប្តូរ៖

```text
familyName
email
phoneNumber
status
```

ប៉ុន្តែ UUID របស់វានៅតែដដែល។

---

# 17. Value Objects

មាន៖

```text
CustomerId
Email
PhoneNumber
```

Concept របស់ Value Object គឺ object ដែល focus លើ value ជាង identity។

ឧទាហរណ៍៖

```java
new Email("sokha@example.com")
```

ប៉ុន្តែបច្ចុប្បន្ន `Email` និង `PhoneNumber` មិនទាន់មាន format validation ទេ។

មានន័យថា អាច theoretically បញ្ចូល៖

```text
Email = "abc"
PhoneNumber = "hello"
```

ហើយ Domain អាចនៅតែទទួលបាន ប្រសិនបើ HTTP `@NotBlank` validation pass។

ដូច្នេះនេះជាផ្នែកមួយដែលគួរ improve។

---

# 18. Customer Status

Customer មាន status ឧទាហរណ៍៖

```text
ACTIVE
INACTIVE
```

ពេល create customer៖

```text
status = ACTIVE
```

ពេល deactivate៖

```text
ACTIVE
   ↓
INACTIVE
```

Business rule នៅ Domain Entity។

---

# 19. CustomerDomainService

Domain Service ប្រើសម្រាប់ Business Logic ដែលយើងមិនចង់ដាក់ក្នុង Controller ឬ Repository។

មាន operation ដូចជា៖

```text
initiateCustomer
updateCustomer
deactivateCustomer
```

Domain Service គួរតែ focus លើ business rules។

---

# 20. Repository Port

មាន៖

```java
CustomerRepository
```

វាជា interface។

Concept៖

```java
interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId customerId);
}
```

នេះជា **Outbound Port**។

Domain/Application និយាយថា៖

> ខ្ញុំត្រូវការ save និង find Customer។

ប៉ុន្តែវាមិន care ថា Database ជាអ្វីទេ។

អាចជា៖

```text
PostgreSQL
MySQL
Oracle
MongoDB
REST API
In-memory
```

Application depend លើ interface ប៉ុណ្ណោះ។

---

# 21. Persistence Adapter

Implementation គឺ៖

```text
PostgresCustomerRepository
```

វា implement៖

```text
CustomerRepository
```

Structure៖

```text
Application
     ↓
CustomerRepository
     ↑
PostgresCustomerRepository
     ↓
JdbcTemplate
     ↓
PostgreSQL
```

នេះជា **Dependency Inversion**។

ខាង Core មិន depend លើ Database។

Database Adapter depend លើ Core contract។

---

# 22. JdbcTemplate

Persistence Layer ប្រើ៖

```text
JdbcTemplate
```

មិនប្រើ JPA/Hibernate ទេ។

វាសរសេរ SQL ដោយផ្ទាល់។

ឧទាហរណ៍ concept៖

```sql
INSERT INTO customers (...)
VALUES (...)
ON CONFLICT (...)
DO UPDATE ...
```

`ON CONFLICT` គឺជា PostgreSQL feature សម្រាប់ **Upsert**។

មានន័យថា៖

```text
Row មិនមាន → INSERT
Row មានហើយ → UPDATE
```

---

# 23. Create Customer Flow

Request៖

```http
POST /api/customers
```

Body៖

```json
{
  "username": "sokha",
  "familyName": "Chan",
  "givenName": "Sokha",
  "email": "sokha@example.com",
  "phoneNumber": "+85512345678"
}
```

Flow៖

```text
1. Client
   ↓
2. CustomerController
   ↓
3. @Valid CustomerInitiateRequest
   ↓
4. CustomerWebMapper
   ↓
5. InitiateCustomerCommand
   ↓
6. InitiateCustomerUseCase
   ↓
7. CustomerDomainService
   ↓
8. Customer.initiateCustomer()
   ↓
9. Generate UUID
   ↓
10. status = ACTIVE
   ↓
11. CustomerRepository.save()
   ↓
12. PostgreSQL
   ↓
13. InitiateCustomerResult
   ↓
14. Response DTO
   ↓
15. HTTP 201 Created
```

សំខាន់ណាស់៖

UUID មិនបាន generate នៅ Controller។

វា generate នៅ Domain។

```text
Customer.initiateCustomer()
```

ដែលបង្ហាញថា Customer entity ជាអ្នក control lifecycle របស់ខ្លួន។

---

# 24. CustomerInitiatedEvent

ពេល Create Customer Domain Service បង្កើត៖

```text
CustomerInitiatedEvent
```

នេះគឺជា **Domain Event**។

Meaning៖

> Something important happened in the Domain.

ក្នុងនេះ៖

```text
Customer was initiated.
```

ប៉ុន្តែបច្ចុប្បន្ន Event គ្រាន់តែត្រូវបាន create នៅ memory។

វាមិនបាន publish ទៅ៖

```text
Kafka
RabbitMQ
Event Bus
```

ទេ។

---

# 25. Update Customer Flow

Request៖

```http
PUT /api/customers/{customerId}
```

Body៖

```json
{
  "familyName": "Chan",
  "givenName": "Sokha Updated"
}
```

Flow៖

```text
Controller
   ↓
UpdateCustomerCommand
   ↓
UpdateCustomerUseCase
   ↓
CustomerRepository.findById()
   ↓
CustomerDomainService.updateCustomer()
   ↓
Change Family Name / Given Name
   ↓
CustomerRepository.save()
   ↓
Return Response
```

ប្រសិនបើ Customer មិនមាន៖

```text
CustomerNotFoundException
```

Controller map ទៅ៖

```http
404 Not Found
```

---

# 26. Business Rule បច្ចុប្បន្នរបស់ Update

Domain check ថា៖

```text
familyName != null
givenName != null
```

ប៉ុន្តែមិន check status។

ដូច្នេះ Customer ដែលជា៖

```text
INACTIVE
```

អាចនៅតែ Update Name បាន។

បើ Business Requirement និយាយថា inactive customer មិនអាច update ទេ នោះគួរតែបន្ថែម rule៖

```text
if status != ACTIVE
    throw CustomerDomainException
```

---

# 27. Deactivate Customer Flow

Request៖

```http
PUT /api/customers/{customerId}/deactivate
```

មិនមាន body ទេ។

Flow៖

```text
CustomerController
       ↓
customerId
       ↓
DeactivateCustomerUseCase
       ↓
CustomerRepository.findById()
       ↓
CustomerDomainService.deactivateCustomer()
       ↓
Customer.deactivate()
       ↓
ACTIVE → INACTIVE
       ↓
CustomerRepository.save()
```

Business Rule៖

```text
Only ACTIVE customer can be deactivated
```

ដូច្នេះ៖

```text
ACTIVE → INACTIVE
```

Allowed។

ប៉ុន្តែ៖

```text
INACTIVE → INACTIVE
```

មិន allowed ទេ។

វានឹង throw៖

```text
CustomerDomainException
```

---

# 28. Problem នៅ Deactivate Error Handling

Project មាន handler សម្រាប់៖

```text
CustomerNotFoundException
```

→ `404`

ប៉ុន្តែមិនទាន់មាន handler សម្រាប់៖

```text
CustomerDomainException
```

ដូច្នេះ deactivate Customer ដែល inactive រួច អាចបង្កើត generic `500 Internal Server Error` ឬ default Spring error response។

គួរតែមានដូចជា៖

```text
CustomerDomainException
        ↓
400 Bad Request
```

ឬ៖

```text
409 Conflict
```

`409 Conflict` ក៏សមស្រប ព្រោះ current Customer state conflict ជាមួយ operation ដែល client request។

---

# 29. Database

Database៖

```text
PostgreSQL
```

Database name៖

```text
istad_customer
```

Table៖

```text
customers
```

Columns៖

```text
customer_id
username
family_name
given_name
email
phone_number
status
```

---

# 30. Environment Variables

Configuration៖

```text
CUSTOMER_DB_URL
CUSTOMER_DB_USERNAME
CUSTOMER_DB_PASSWORD
```

Example៖

```bash
export CUSTOMER_DB_URL=jdbc:postgresql://localhost:5432/istad_customer
export CUSTOMER_DB_USERNAME=postgres
export CUSTOMER_DB_PASSWORD=123456
```

ហើយ run៖

```bash
./gradlew -p microservices/customer-service :customer-service-main:bootRun
```

មានន័យថា Gradle ត្រូវ run `customer-service-main` Spring Boot module។

---

# 31. schema.sql

នៅ៖

```text
customer-service-persistence/src/main/resources/schema.sql
```

ពេល Application start Spring នឹង execute SQL នេះ។

វាបង្កើត៖

```text
customers table
```

បើ table មិនទាន់មាន។

Concept៖

```sql
CREATE TABLE IF NOT EXISTS customers (...)
```

ដូច្នេះ developer មិនចាំបាច់ manually create table រាល់ពេល។

---

# 32. Domain Event បច្ចុប្បន្ន

មាន Events ដូចជា៖

```text
CustomerInitiatedEvent
CustomerUpdatedEvent
CustomerDeactivatedEvent
```

ប៉ុន្តែ architecture បច្ចុប្បន្នមានតែ៖

```text
Create Event
```

មិនទាន់មាន៖

```text
Event Publisher
Kafka Producer
RabbitMQ
Message Broker
Outbox
```

ទេ។

`customer-service-message` មាន module រួច ប៉ុន្តែទទេ។

អនាគតវាអាចជា៖

```text
Customer Use Case
       ↓
Domain Event
       ↓
Event Publisher Port
       ↓
Kafka Adapter
       ↓
Kafka Topic
       ↓
Other Microservices
```

នេះអាចជាជំហានបន្ទាប់ល្អសម្រាប់ project Microservices។

---

# 33. ចំណុចដែល Project ធ្វើបានល្អ

Architecture បច្ចុប្បន្នមាន separation ល្អ៖

```text
REST        → HTTP concerns
Application → Use cases
Domain      → Business rules
Repository  → Port / abstraction
Persistence → PostgreSQL implementation
Main        → Wiring
```

នេះមានន័យថា **Separation of Concerns** ល្អ។

ឧទាហរណ៍បើថ្ងៃក្រោយ PostgreSQL ប្តូរទៅ MongoDB៖

```text
CustomerDomainService
InitiateCustomerUseCase
UpdateCustomerUseCase
```

មិនចាំបាច់ដឹងទេ។

យើងគ្រាន់តែ implement adapter ថ្មី៖

```text
MongoCustomerRepository
```

ដែល implement៖

```text
CustomerRepository
```

---

# 34. ចំណុចដែលមិនទាន់ complete

បច្ចុប្បន្ន Project មាន gap មួយចំនួន៖

| Part | Current Status |
|---|---|
| Create Customer | ✅ |
| Update Customer | ✅ |
| Deactivate Customer | ✅ |
| Get Customer | ❌ |
| List Customers | ❌ |
| Activate Customer | ❌ |
| Delete Customer | ❌ |
| Kafka/Event Publisher | ❌ |
| Email Validation | ❌ |
| Phone Validation | ❌ |
| Full Exception Handling | ❌ |
| Business Service Features | ❌ |
| Message Module | ❌ |

មាន unused files ផងដែរ៖

```text
initiateCustomerUseCase.java
initiateCustomerResult.java
```

ដោយសារ Java naming convention គួរតែជា៖

```text
InitiateCustomerUseCase.java
InitiateCustomerResult.java
```

បើ lowercase files មិនប្រើ គួរតែលុបវាចេញ ដើម្បីកុំឲ្យ project confusing។

---

# 35. សង្ខេប Architecture ឲ្យងាយចាំ

អាចចាំជា 5 ជំហាន៖

```text
1. REST Adapter
      ↓
2. Application Use Case
      ↓
3. Domain
      ↓
4. Repository Port
      ↓
5. Persistence Adapter
```

ឧទាហរណ៍៖

```text
CustomerController
      ↓
InitiateCustomerUseCase
      ↓
CustomerDomainService
      ↓
CustomerRepository
      ↓
PostgresCustomerRepository
      ↓
PostgreSQL
```

### REST
ទទួល request ពី Client។

### Application
រៀបចំ workflow / Use Case។

### Domain
មាន Core Business Rules។

### Port
កំណត់ contract ថា Core ត្រូវការអ្វី។

### Adapter
Implement Port ដោយប្រើ Technology ជាក់ស្តែង ដូចជា PostgreSQL។

---

## បើយើងមើលតាម Hexagonal Architecture

Project នេះអាច map បានដូចនេះ៖

```text
          Outside World
               |
        REST Controller
        Inbound Adapter
               |
               v
      Application Use Case
               |
               v
        Customer Domain
               |
               v
      CustomerRepository
         Outbound Port
               |
               v
 PostgresCustomerRepository
        Outbound Adapter
               |
               v
          PostgreSQL
```

ចំណុចសំខាន់បំផុតដែលអ្នកគួរចាំសម្រាប់ project នេះគឺ៖

> **Controller មិនមែនជា Business Logic។ Application រៀបចំ Use Case។ Domain កាន់ Business Rules។ Port ជា Interface/Contract។ Adapter ជា Implementation ដែលភ្ជាប់ Technology ខាងក្រៅ។**

នេះជាមូលដ្ឋានសំខាន់របស់ architecture ដែល project របស់អ្នកកំពុងប្រើ។

---

# 36. Write Create and Update from Zero: Step by Step

This is the practical order to write these features. Each layer has one job:

```text
HTTP JSON -> Controller -> Command -> Use Case -> Domain -> Repository -> PostgreSQL
PostgreSQL -> Repository -> Use Case Result -> Response DTO -> HTTP JSON
```

The code shown here follows the classes already present in this project. Package declarations and imports are omitted from short snippets; see the named files for complete Java source.

## Step 1: Decide the data and database table

A customer needs `customerId`, `username`, `familyName`, `givenName`, `email`, `phoneNumber`, and `status`. Write `customer-service-persistence/src/main/resources/schema.sql` with the `customers` table. Use a UUID primary key. The Java `Customer` class represents one customer; the PostgreSQL table stores it.

## Step 2: Write the domain rules

In `customer-domain-core/.../entity/Customer.java`, put rules that must always be true:

```java
public void initiateCustomer() {
    validateCustomer();
    super.setId(new CustomerId(UUID.randomUUID()));
    status = CustomerStatus.ACTIVE;
}

public void updateCustomer(String familyName, String givenName) {
    if (familyName == null || givenName == null) {
        throw new CustomerDomainException("familyName and givenName must not be null");
    }
    this.familyName = familyName;
    this.givenName = givenName;
}
```

Create generates the ID on the server and sets `ACTIVE`. Update changes only the two names. The existing `CustomerDomainServiceImpl` calls these entity methods and creates timestamped domain events. The current use cases do not publish those events.

## Step 3: Write the repository interface and SQL implementation

Create the application-facing contract in `customer-application-service/.../port/out/CustomerRepository.java`:

```java
public interface CustomerRepository {
    Customer save(Customer customer);
    Optional<Customer> findById(CustomerId id);
}
```

Implement it in `customer-service-persistence/.../PostgresCustomerRepository.java`. `findById` executes `SELECT ... WHERE customer_id = ?` and builds a `Customer` from the row. `save` executes `INSERT ... ON CONFLICT (customer_id) DO UPDATE ...`: it inserts during create and updates the same row during update. Use `JdbcTemplate` for these queries, as the project does.

## Step 4: Write the Create use case

Define `InitiateCustomerCommand` with the five input strings and `InitiateCustomerResult` with the returned customer data. In `InitiateCustomerUseCase.execute(command)`, perform the steps in this order:

```java
Customer customer = Customer.builder()
        .username(command.username())
        .familyName(command.familyName())
        .givenName(command.givenName())
        .email(new Email(command.email()))
        .phoneNumber(new PhoneNumber(command.phoneNumber()))
        .build();

customerDomainService.initiateCustomer(customer); // sets ID and ACTIVE
customerRepository.save(customer);                 // inserts the row

return new InitiateCustomerResult(
        customer.getId().value(),
        customer.getUsername(),
        customer.getFamilyName(),
        customer.getGivenName(),
        customer.getEmail().value(),
        customer.getPhoneNumber().value()
);
```

Why this order? The repository needs an ID and status to save the row, so the domain must initialize the customer before `save`.

## Step 5: Expose Create as an HTTP endpoint

Define `CustomerInitiateRequest` in the REST module with the same five input fields and `@NotBlank` on each. Define `CustomerInitiateResponse` for the output. `CustomerWebMapper` maps the request to `InitiateCustomerCommand` and `InitiateCustomerResult` to the response. Then add this to `CustomerController`:

```java
@ResponseStatus(HttpStatus.CREATED)
@PostMapping
public CustomerInitiateResponse initiateCustomer(
        @Valid @RequestBody CustomerInitiateRequest request) {
    InitiateCustomerResult result = initiateCustomerUseCase.execute(
            customerWebMapper.toCommand(request));
    return customerWebMapper.toResponse(result);
}
```

The controller class already has `@RequestMapping("/api/customers")`, so `@PostMapping` means `POST /api/customers`. A successful call returns HTTP 201 and the generated `customerId`.

## Step 6: Write the Update use case

Define `UpdateCustomerCommand(UUID customerId, String familyName, String givenName)` and an `UpdateCustomerResult`. The update use case must load the existing customer before changing it:

```java
Customer customer = customerRepository
        .findById(new CustomerId(command.customerId()))
        .orElseThrow(() -> new CustomerNotFoundException(command.customerId()));

customerDomainService.updateCustomer(
        customer, command.familyName(), command.givenName());
customerRepository.save(customer);

return new UpdateCustomerResult(
        customer.getId().value(),
        customer.getUsername(),
        customer.getFamilyName(),
        customer.getGivenName(),
        customer.getEmail().value(),
        customer.getPhoneNumber().value()
);
```

The sequence is **find → change → save → return**. If the ID does not exist, stop at `findById` and raise `CustomerNotFoundException`. The controller maps that exception to HTTP 404.

## Step 7: Expose Update as an HTTP endpoint

Define `CustomerUpdateRequest` with `familyName` and `givenName`, each `@NotBlank`. The UUID comes from the URL, so the JSON body does not need an ID. Add this controller method:

```java
@PutMapping("/{customerId}")
public CustomerUpdateResponse updateCustomer(
        @PathVariable("customerId") UUID customerId,
        @Valid @RequestBody CustomerUpdateRequest request) {
    UpdateCustomerResult result = updateCustomerUseCase.execute(
            new UpdateCustomerCommand(
                    customerId, request.familyName(), request.givenName()));
    return customerWebMapper.toResponse(result);
}
```

Add `toResponse(UpdateCustomerResult)` to `CustomerWebMapper`. This becomes `PUT /api/customers/{customerId}` and returns HTTP 200 with the updated customer data.

## Step 8: Run and verify the full flow

Start PostgreSQL and the customer service on port **8282**. First create a customer:

```bash
curl -X POST http://localhost:8282/api/customers \
  -H 'Content-Type: application/json' \
  -d '{"username":"sokha","familyName":"Chan","givenName":"Sokha","email":"sokha@example.com","phoneNumber":"+85512345678"}'
```

Copy `customerId` from the response and update its names:

```bash
curl -X PUT http://localhost:8282/api/customers/<customerId> \
  -H 'Content-Type: application/json' \
  -d '{"familyName":"Chan","givenName":"Sokha Updated"}'
```

Check the `customers` table: the first request should insert a row with status `ACTIVE`; the second should change its name columns. Try an unknown UUID to verify the 404 path.
