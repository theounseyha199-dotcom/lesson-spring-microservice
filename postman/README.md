# Postman collections

## IAM assignment

Import `IAM-Assignment.postman_collection.json` and `IAM-Assignment.postman_environment.json`, then select
**IAM Assignment - Local (secrets empty)**. Every exported password, secret and token is intentionally empty.

1. Start IAM after supplying the external configuration in the [service guide](../README.md#iam-service).
2. Set `iam_url`, `oidc_client_id` and `oidc_client_secret` to an existing authorization-code client. Configure the OAuth
   helper in folder 01 with Authorization Code (With PKCE), SHA-256, **Authorize using browser**, the registered callback,
   and Basic-header client authentication. Sign in with an internal user; use an admin with exact permissions for admin APIs.
3. Copy the resulting access token to the environment's **user_access_token** and its refresh token to **user_refresh_token**.
   An ID token or a client-credentials token cannot substitute for a user access token at /api/me.
4. Set `new_user_password` to a disposable test password (at least 12 characters, at most 72 UTF-8 bytes). Start with
   metadata, registration, then permission/role/user CRUD folders. Created UUIDs save automatically. Clear `run_id` and
   target UUIDs when deliberately starting a new disposable run. Entity flags are restored before assignment requests.
5. To test DCR, set `registration_client_secret` to IAM_REGISTRATION_CLIENT_SECRET. Request a fresh single-use initial
   token, register, then read that client with the returned registration token. The generated secret is returned once.
   Registration/read tokens are separate from the user access token. For older OIDC clients with narrower stored scopes,
   request their original scopes or use the newly registered client with its returned secret; startup does not widen them.
6. Password and identity requests in folder 08 are manual: fill current/new passwords or a real linked identity UUID.
   The API never accepts an unvalidated provider identity payload. Link through the browser first. Run folder 09 cleanup
   last and verify the selected target UUIDs; do not run the collection indiscriminately against application data.
7. Never share populated token/secret values or export them back into Git. No collection request logs those values.

All 39 Section D routes have requests and response assertions. Administrative requests require exact resource:action
permissions, not merely ROLE_ADMIN or OAuth scopes. Revocation DELETE payloads contain a reason; normal entity deletion
and identity disconnection do not. Protected reads use bearer tokens; public registration/metadata are explicitly no-auth.

Runtime verification passed 44 collection requests and 66 assertions against an isolated packaged app. Interactive OAuth-helper and manual identity/password folders
were excluded from the collection run; all REST routes are additionally covered by server integration tests.

[Postman's OAuth documentation](https://learning.postman.com/docs/use/send-requests/authorization/oauth-20/) describes
its interactive authorization-code/PKCE helper. Automated collection runners need an already obtained user token.

## Other microservices

`Service-OAuth-Full.postman_collection.json` and `Service-OAuth-Local.postman_environment.json` retain the existing
Order/Customer/Business/Payment/Gateway examples. These have different token variables and are separate from the IAM
assignment collection. Set `order_client_secret` from ORDER_CLIENT_SECRET and `client_secret` from the existing
Postman service client's IAM_POSTMAN_CLIENT_SECRET. Those values are no longer exported in plaintext.

On this workstation, the previous filled legacy environment was preserved only in the Git-ignored
`.local/postman-service.environment.json`. Do not commit or share that private file.

Start the authorization server (9000), order (9092), customer (8084), business (8082), payment (8083), gateway (9999),
databases and Consul. Consul may override the order port. The two folder-01 token requests save separate bearer tokens.
Create the required customer/business records, then choose individual requests. Checkout creates another order and
attempts payment; folder 08 deactivates customers. The existing Order security configuration enables CSRF, so Order
writes may return 403 despite a valid bearer token. Direct customer writes use stateless bearer-token security.
These older cross-service workflows were not rerun as part of the IAM delivery audit.
