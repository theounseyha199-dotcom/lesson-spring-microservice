# Customer service

Customer and Customer Segment CRUD use reactive Spring Data MongoDB through application ports. Important classes and business rules have comments above their declarations. The HTTP layer uses Spring MVC's asynchronous support for `Mono` responses; database calls never use `.block()` or manual `.subscribe()` in production code.

## Run

From the repository root, start the MongoDB service after creating the external `ite-net` Docker network if necessary:

```bash
docker compose -f deployment/docker-compose.yaml up -d mongodb
./gradlew -p microservices/customer-service :customer-service-main:bootRun
```

The default port is `8084`. Override `CUSTOMER_PORT` or `CUSTOMER_MONGO_URI` as needed. The default database is `istad_customer`; collections are `customers` and `customer_segments`. See [deployment instructions](../../deployment/README.md) and the [frontend guide](../../docs/FRONTEND_INTEGRATION.md).

## Customer API

| Method | Path | Behavior |
| --- | --- | --- |
| POST | `/api/customers` | Create an ACTIVE customer, optionally with `segmentId`; returns 201 |
| GET | `/api/customers` | Filtered, sorted, paginated JSON array of visible customers |
| GET | `/api/customers/{id}` | Read a single visible customer |
| PUT | `/api/customers/{id}` | Update required names and optional username, email, and phone |
| PATCH | `/api/customers/{id}/status` | Set ACTIVE or INACTIVE |
| PUT | `/api/customers/{id}/deactivate` | Legacy deactivation route; rejects already inactive customers |
| PUT | `/api/customers/{id}/segment` | Assign an ACTIVE segment using `{ "segmentId": "UUID" }` |
| DELETE | `/api/customers/{id}/segment` | Remove the segment membership |
| DELETE | `/api/customers/{id}` | Soft delete; returns 204 |
| GET | `/statistics` | Count customers by status, segment, and status plus segment |
| GET | `/api/customers/statistics` | Alias for the same aggregation |

```json
{
  "username": "sokha",
  "familyName": "Chan",
  "givenName": "Sokha",
  "email": "sokha@example.com",
  "phoneNumber": "+85512345678",
  "segmentId": null
}
```

Username and email are normalized to lowercase and are unique among ACTIVE and INACTIVE customers. A database partial unique index enforces this under concurrent writes. Names must contain 1 to 100 characters; email must be valid; phone must contain 8 to 15 digits with an optional `+` prefix. Domain validation protects non-HTTP callers too.

Only ACTIVE and INACTIVE are valid direct status transitions. DELETED is terminal and can only be applied by DELETE. Missing or deleted resources return 404. Repeated DELETE returns 404. Duplicate identities, invalid lifecycle transitions, and concurrent edits return 409. Invalid requests and filters return 400 as Problem Details JSON.

Updates preserve creation audit fields and refresh modification fields. Soft deletion also records `deletedAt` and `deletedBy`. The auditor currently resolves to SYSTEM. Customer updates use a version condition, preventing stale writes and resurrection after deletion. Legacy documents without a version receive one on their first update; unknown legacy creation audit fields remain empty.

## Customer Segment API

| Method | Path | Behavior |
| --- | --- | --- |
| POST | `/api/customer-segments` | Create an ACTIVE segment with name and optional description |
| GET | `/api/customer-segments` | Filtered, sorted, paginated JSON array |
| GET | `/api/customer-segments/{id}` | Read a visible segment |
| PUT | `/api/customer-segments/{id}` | Update name, description, and ACTIVE/INACTIVE status |
| DELETE | `/api/customer-segments/{id}` | Soft delete; returns 204 |

Create body: `{ "name": "VIP", "description": "Priority customers" }`.
Update body: `{ "name": "VIP", "description": "Priority customers", "status": "INACTIVE" }`.

Segment names are case-insensitively unique and remain reserved after deletion. Names are limited to 100 characters and descriptions to 1000. Only ACTIVE segments accept new customer assignments. INACTIVE and deleted segments preserve existing customer memberships, allowing historical statistics to retain the original segment identity. Deleting a segment does not delete its customers. Deleted segments are hidden from segment CRUD reads. Optimistic locking rejects conflicting concurrent segment updates.

## Reusable dynamic filtering

Both list endpoints share `DynamicMongoQuery`. Supply repeated `filter=field:operator:value` parameters. Filters are combined with AND; `search` matches any searchable text field. The soft-delete exclusion is mandatory and cannot be overridden by a filter.

| Parameter | Default | Rules |
| --- | --- | --- |
| `page` | 0 | Zero-based, 0..100000 |
| `size` | 20 | 1..100 |
| `sort` | createdAt | Must be an allowed field |
| `direction` | desc | asc or desc |
| `search` | none | Literal, case-insensitive text; max 100 characters |
| `filter` | none | Up to 20 expressions; max 300 characters each |

Customer fields: `customerId`, `username`, `familyName`, `givenName`, `email`, `phoneNumber`, `status`, `segmentId`, `createdAt`, `updatedAt`.
Segment fields: `segmentId`, `name`, `description`, `status`, `createdAt`, `updatedAt`.

Operators: `eq`, `ne`, `in`; `contains` for text; `gt`, `gte`, `lt`, `lte` for dates. Dates must be ISO UTC instants. `in` takes comma-separated values. `segmentId:eq:null` selects unassigned customers. Text filters and search treat regex metacharacters literally; raw MongoDB JSON, field paths, and arbitrary operators are rejected. Sorting always includes `_id` as a stable tie-breaker.

```bash
curl -G 'http://localhost:8084/api/customers' \
  --data-urlencode 'filter=status:in:ACTIVE,INACTIVE' \
  --data-urlencode 'filter=familyName:contains:Chan' \
  --data-urlencode 'page=0' --data-urlencode 'size=20'

curl -G 'http://localhost:8084/api/customer-segments' \
  --data-urlencode 'filter=status:eq:ACTIVE' --data-urlencode 'search=VIP'

curl -G 'http://localhost:8084/statistics' \
  --data-urlencode 'filter=status:eq:ACTIVE'
```

## Statistics

MongoDB performs one `$match` plus `$facet` pipeline. Counts exclude DELETED customers and include all filter matches, without list pagination. Unassigned customers have `segmentId: null`. Segment counts use permanent IDs, including IDs of retired segments. Empty result sets return total 0 and empty arrays. Only groups with matching customers are returned.

```json
{
  "total": 2,
  "byStatus": [{ "status": "ACTIVE", "total": 2 }],
  "bySegment": [{ "segmentId": null, "total": 2 }],
  "byStatusAndSegment": [{ "status": "ACTIVE", "segmentId": null, "total": 2 }]
}
```

## Verification

```bash
./gradlew -p microservices/customer-service test :customer-service-main:bootJar
```

The real HTTP/MongoDB integration test is opt-in. It starts the application on a random port, creates a uniquely named test database, and removes that database afterward:

```bash
RUN_MONGO_INTEGRATION_TESTS=true ./gradlew -p microservices/customer-service \
  :customer-service-main:test --tests '*CustomerApiIntegrationTest' --rerun-tasks
```

Its default connection is `mongodb://mongod:mongod@localhost:2026/{database}?authSource=admin`. To use another MongoDB server, set `CUSTOMER_TEST_MONGO_URI` with the literal `{database}` placeholder; the test refuses a URI without it to protect existing data.

Unique indexes are created before the first write. Existing duplicate customer identities must be resolved before those indexes can be created. Old JPA source files are reference material and are not active persistence adapters.
