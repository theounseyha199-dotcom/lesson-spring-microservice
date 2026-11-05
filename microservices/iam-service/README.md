# IAM service

The repository's authorization server uses PostgreSQL on port 9543 (`db_oauth2`).
Run the full project with `./run-all.sh` at the repository root; see
[the run guide](../../README.md).

Login: `http://localhost:9000/login`, local user `user` / `password`.
OIDC client: `oidc-client` / `secret`, Authorization Code with PKCE (SHA-256),
scopes `openid profile`. Postman callback:
`https://oauth.pstmn.io/v1/browser-callback`.

The startup runner inserts missing OIDC, Order and Postman clients after database
migrations. Existing client IDs and secrets are preserved. Set the same
`ORDER_CLIENT_SECRET` for IAM and Order (the startup script generates it once).
Order tokens target Customer; Postman API tokens target Order.

The signing key persists at `IAM_SIGNING_KEY_FILE`, default
`.local/iam-signing-key.json`, with owner-only permissions on Linux. Back it up
and keep it outside Git. The startup script uses an absolute path.

Database overrides: `IAM_DB_URL`, `IAM_DB_USER`, `IAM_DB_PASSWORD`.
Server overrides: `IAM_PORT`, `IAM_ISSUER`. The OIDC secret can be supplied with
`app.iam.client-secret` (`APP_IAM_CLIENTSECRET` using Spring's environment binding).
Changing a secret property does not rotate an existing registered client.

Tests use H2 and temporary signing keys. The test changelog adapts PostgreSQL's
multiple ADD COLUMN syntax for H2; production migrations remain unchanged.

For direct IntelliJ runs, IAM and Order also load the ignored
`.local/runtime.properties` file when the working directory is the repository
root or the service directory. Put `ORDER_CLIENT_SECRET=<shared secret>` in this
file, using the same value as `.local/runtime.env` and the existing registered
Order client. Environment variables override this file. Keep its permissions
restricted (`chmod 600 .local/runtime.properties`).
