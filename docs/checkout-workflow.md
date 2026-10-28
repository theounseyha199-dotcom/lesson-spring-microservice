# Four-service checkout and code explanation

## Business workflow

`POST /api/checkout` on order-service coordinates a complete local assignment
workflow across Customer, Business, Order, and Payment services:

1. Look up the customer and reject missing or inactive customers.
2. Look up the business and reject missing or inactive businesses.
3. Calculate the order total from validated items and save the PENDING order in MongoDB.
4. Send the saved order ID, customer ID, and calculated total to payment-service.
5. Return HTTP 201 with the order and its PENDING payment registration.

The default client uses logical service names with the existing
`@LoadBalanced WebClient.Builder`. With `hardcoded-demo`, the same workflow uses
plain WebClient and fixed addresses on ports 8084, 8082, and 8083. This isolates
address resolution while retaining the same business sequence.

Create the customer with `POST /api/customers` and business with
`POST /api/businesses` first. Use the returned identifiers in this request:

```json
{
  "customerId": "<created customer UUID>",
  "businessId": "<created business UUID>",
  "deliveryAddress": "Phnom Penh",
  "items": [{
    "productId": "33333333-3333-3333-3333-333333333333",
    "quantity": 2,
    "unitPrice": 5.00
  }]
}
```

Submit this body to `http://localhost:8081/api/checkout`. The calculated total
is 10.00. Payment receives that total from the saved order, not a separate
payment amount supplied by the checkout caller.

## Code responsibilities and best practices

- `CheckoutController` coordinates the workflow and returns explicit outcomes.
- `CheckoutServices` owns typed HTTP contracts, service-name resolution, active-state
  checks, three-second remote-call deadlines, and downstream error translation.
- `CreateOrderUseCase` and `OrderWebMapper` retain order construction and response mapping.
- Customer and Business GET endpoints expose lookup contracts without sharing databases.
- `BusinessRepository.findById` uses the reactive MongoDB adapter; Customer reuses
  its existing repository lookup. Missing records return HTTP 404.
- Bean Validation rejects invalid quantities, prices, missing fields, and null items.
- Order runs Spring MVC because MongoDB persistence and the checkout orchestration are blocking.
  WebFlux remains available for WebClient; the application no longer enables a
  competing WebFlux server configuration.
- The historical hard-coded path is explicit and opt-in. Normal startup uses discovery.
- No automatic write retry is applied: a lost response does not prove that a payment
  record was not committed.

## Failure contract and remaining limitations

Missing customer or business returns 404 before order creation. An inactive record
returns 409. Invalid checkout input returns 400. An unavailable lookup service
returns 503 before order creation.

If payment registration fails after the order is persisted, checkout returns
HTTP 202, the saved order, a null payment field, and
`PAYMENT_RECONCILIATION_REQUIRED`. This outcome does not assert that payment failed:
a remote write could have committed before its response was lost. The caller must
retain the order ID and must not blindly resubmit checkout. The 202 response flags
manual reconciliation; no background reconciliation worker is implemented.

This is an educational checkout-to-pending-payment workflow, not a complete
production commerce system. The standalone `/api/orders` and `/api/payments`
create endpoints remain lower-level APIs and do not enforce the checkout's
cross-service checks. Authentication, authorization, product-catalog validation,
inventory, authoritative pricing, payment-provider authorization/capture, durable
idempotency, order/payment reconciliation, and a saga or transactional outbox are
remaining work. Prices are still supplied by the request. Customer/business status
can change between lookup and persistence. Discovery does not solve these business
consistency problems.

## Reproduce the integration test

Build all four services with JDK 25 using deployment/README.md.
Run `node scripts/verify-checkout.mjs` with `consul`, `mongod`, and
Playwright/Chromium installed. Set PLAYWRIGHT_MODULE, CHROMIUM_PATH, or
JAVA_COMMAND if needed. Ports 8500, 8300–8302, 8600, 8081–8084 and 12027 must be free.

The script launches isolated MongoDB and Consul processes and all four services,
checks checkout and failure responses, and saves evidence under
`docs/screenshots/checkout`. It stops its own child processes afterward.
Existing screenshots are historical and should be regenerated for this setup.
