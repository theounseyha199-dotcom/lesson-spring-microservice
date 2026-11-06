# Live-provider acceptance — human sign-in required

Automated callback proof/provisioning/linking tests pass for all four providers. A configured client ID or a successful
authorization-start redirect is not proof that the provider console accepts the current callback or account credentials.
No live provider-account login was performed during this delivery audit. Real authorization-page checks and the Google public-callback blocker are now recorded in [LIVE-PROVIDER-RESULTS.md](LIVE-PROVIDER-RESULTS.md).

Before acceptance, configure the real environment variables, start the current IAM build, and ensure each provider
console lists the exact redirect URI. Use HTTPS for the public deployment. Facebook/Telegram currently use an ngrok
callback; start login on that same origin so the OAuth session/state cookie returns. Provider account login and MFA
must be completed by the account owner. Do not put provider passwords, codes, tokens or screenshots containing secrets
in the evidence log.

| Provider | Registration ID | Callback | Live result |
| --- | --- | --- | --- |
| Google | google | /login/oauth2/code/google | Local sign-in page reached; public redirect mismatch; account sign-in pending |
| GitHub | github | /login/oauth2/code/github | Sign-in page reached; account sign-in pending |
| Facebook | facebook | /login/oauth2/code/facebook | Sign-in page reached; account sign-in pending |
| Telegram | telegram | /login/oauth2/code/telegram | Authorization page reached; account sign-in pending |

For each provider:

1. Start from the IAM login page's provider button. Complete provider authentication and consent.
2. Confirm that a complete new profile creates an internal USER account with a UUID subject and no provider-derived
   administrative privileges. If required attributes are absent (common with GitHub/Telegram), complete only the missing
   fields. A manually entered email must remain unverified. The browser must remain unauthenticated until completion.
3. Sign out and repeat provider login: it must find the same internal account/identity, update login history, and retain
   the internal profile rather than silently replacing it from provider data.
4. Test a provider email matching an existing local account: no auto-merge. Sign in to that existing account and use
   Linked accounts to explicitly connect the provider. Confirm callbacks keep the same internal owner and cancellation
   returns safely to the account page.
5. Verify another user's linked provider cannot be transferred, a changed/expired owner session cannot complete linking,
   and disconnect/relink preserves identity history. Keep another sign-in method before disconnection; the last one is blocked.
6. Request an IAM authorization-code + PKCE token, inspect only nonsecret subject/authority metadata locally, and confirm
   /api/me returns the same internal account. Never attach a token value to the acceptance report.
7. Record date, provider registration ID, callback origin, outcome and a brief nonsecret observation. Include failures
   such as console redirect mismatch or account restrictions explicitly; do not mark a redirect-only check as login success.

These manual checks are the remaining acceptance item, not an unimplemented callback feature. Provider credentials
were externalized with current local values retained; existing registered-client secrets and signing keys were not rotated.
