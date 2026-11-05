# Postman local API collection

Import `Service-OAuth-Full.postman_collection.json` and
`Service-OAuth-Local.postman_environment.json`, then select **Service OAuth - Local**.

1. Set `order_client_secret` to the authorization server's `ORDER_CLIENT_SECRET`.
   The other client credentials use source-code development defaults; adjust them
   if your running server uses environment overrides or previously seeded clients.
2. Start the authorization server (9000), order (9092), customer (8084),
   business (8082), payment (8083), and gateway (9999), plus their databases and Consul.
   Consul configuration can override the order port; update `order_url` if needed.
3. Run both **Get … token** requests in folder 01. They save separate bearer tokens.
4. Create a customer in folder 03 and business in folder 04. Their IDs save automatically.
5. Run desired order, checkout, payment, or gateway requests individually.

Do not run the whole collection indiscriminately: create requests write records,
checkout creates another order and attempts payment, and folder 08 deactivates
customers. Run lifecycle and negative checks individually. Token requests must be
rerun after tokens expire or temporary authorization signing keys change.

The source order security configuration enables CSRF. Order POST requests,
including checkout and customer creation through order, may return 403 despite
valid bearer tokens. Direct customer writes use the existing stateless bearer-token
configuration. Server security has not been changed by this export.

Requests reflect local controllers and OAuth registrations. JSON structure and
variable references were checked locally; no API writes or live end-to-end checks
were performed. The intentionally failing customer `/flux` demo is omitted.
