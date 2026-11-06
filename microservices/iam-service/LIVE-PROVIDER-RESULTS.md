# Live-provider readiness results — 6 November 2026

These checks used the running IAM service and real provider authorization pages in fresh, isolated browser contexts.
No provider account password, OTP or MFA response was entered. No new live user/identity was created by the checks.
End-to-end provider-account acceptance remains incomplete.

## Results

| Provider | Start origin | Observed result |
| --- | --- | --- |
| Google | http://localhost:9000 | Google account sign-in form reached; no redirect mismatch |
| Google | https://lagging-grouped-bobsled.ngrok-free.dev | Google error 400: redirect_uri_mismatch |
| GitHub | https://lagging-grouped-bobsled.ngrok-free.dev | GitHub sign-in form for OAuthServer reached |
| Facebook | https://lagging-grouped-bobsled.ngrok-free.dev | Facebook account sign-in form reached |
| Telegram | https://lagging-grouped-bobsled.ngrok-free.dev | Telegram authorization page with app/phone sign-in choices reached |

The public IAM login page returned 200. All four authorization starts returned 302 and included OAuth state. After the
proxy fix below, all four public redirect_uri values use HTTPS. Reaching an account sign-in page verifies only the
pre-authentication portion; additional provider restrictions can still appear after account authentication/consent.

## Fixed application issue

Google/GitHub initially generated http callbacks when sign-in began on the HTTPS ngrok origin. Enabled
server.forward-headers-strategy=native, using Tomcat's trusted-proxy handling, then restarted the local IAM run
configuration. Public callback checks now produce HTTPS; direct localhost requests retain HTTP.

An embedded-server regression covers X-Forwarded-Proto/Host/Port and direct HTTP behavior. The full H2 checks/packaging
pass: 182 reported, 181 passed, one PostgreSQL-only container skipped; zero failures/errors. PostgreSQL follow-up also passes: 184 tests, zero failures/errors/skips; its disposable database was removed. In deployments, configure the actual trusted reverse-proxy addresses and keep
untrusted forwarded headers outside that trust boundary.

Reference: [Spring Security forwarded-header handling](https://www.springframework.org/spring-security/reference/features/exploits/http.html).

## Google public-origin correction required

In the Google Cloud Console, open the Web OAuth client matching the configured IAM_GOOGLE_CLIENT_ID. Add this exact
entry to **Authorized redirect URIs**, retaining any existing localhost URI:

```text
https://lagging-grouped-bobsled.ngrok-free.dev/login/oauth2/code/google
```

No client secret rotation is needed for this callback-list change. Then restart login from the public IAM login page.
Google requires the requested redirect URI to match an authorized URI for that client; see the
[Google OAuth web-server documentation](https://developers.google.com/identity/protocols/oauth2/web-server).
Until the public URI is registered, use http://localhost:9000/login for Google testing on this workstation.

The provider console is owned by the account holder and was not modified during this check. No Google-console access
was available through the connected tools.

## Existing database evidence

Read-only baseline from db_oauth2, before these checks:

| Provider | Identity rows / distinct owners | Rows with login history | Latest recorded login (UTC) |
| --- | --- | --- | --- |
| GitHub | 1 / 1 | 1 | 2026-11-06 11:28:46.812975 |
| Google | 1 / 1 | 0 | None |
| Telegram | 1 / 1 | 1 | 2026-11-06 11:31:05.614679 |
| Facebook | 0 / 0 | 0 | None |

The counts did not change during the authorization-page checks. Existing timestamps are historical database evidence;
they do not establish that this run observed a new end-to-end login. Usernames, emails, provider subjects and tokens
are intentionally omitted. Complete the account-owner steps in LIVE-PROVIDER-ACCEPTANCE.md and verify the corresponding
identity/login-history changes afterward.
