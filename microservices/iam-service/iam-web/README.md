# Browser account pages — Step 9

Section F routes use the required JTE names:

| Route | Template | Behavior |
| --- | --- | --- |
| GET /login | login.jte | Local login, four social providers and language switch |
| GET/POST /register | register.jte | Public registration, field errors, USER only, redirect to sign in |
| GET /oauth2/consent | consent.jte | Trusted client name, requested scopes, Allow/Cancel |
| GET / | index.jte | Internal account claims and current domain roles/permissions |
| GET/POST /profile/edit | profile-edit.jte | Profile/address fields, validation, optimistic form version |
| GET/POST /profile/password | change-password.jte | Current-password check, new-password policy, masked inputs |
| GET /profile/accounts | linked-accounts.jte | Provider list and explicit linking/disconnection |
| GET /profile/apps | connected-apps.jte | Own authorized applications and revocation |

GET /profile/accounts/{identityUuid}/unlink and GET /profile/apps/{id}/revoke show confirmation pages without
mutating state. Matching POST routes perform the action with browser CSRF protection and the authenticated
internal owner. A posted user UUID/username never selects the owner. The domain protects historical provider
ownership and the last sign-in method. Social-only accounts cannot change a password without the current password;
they continue to sign in through linked providers.

BrowserUser resolves the internal actor and rechecks current account flags. Page controllers call application use
cases; the plain ConnectedApplications port and infrastructure JDBC adapter own Authorization Server state. There
is no web-to-infrastructure project dependency. An optional expectedVersion on UpdateUserProfileCommand protects
browser edits while retaining the existing two-argument constructor for API callers. A stale browser form receives
409 and must reload; validation errors retain nonsecret values. Passwords are never repopulated in responses.

Consent stays within Spring Security's authorization protocol. The Authorization Server redirects to the custom
page, and native POST /oauth2/authorize validates its stored consent state and selected scopes. Cancel submits no
scopes and produces access_denied. Client metadata and form values are escaped by JTE. The state parameter is the
protocol's CSRF binding; the authorization endpoint retains the framework's protocol-specific CSRF handling.

Revocation removes only the current user's stored authorization rows and consent for that registered client in a
transaction. Refresh and pending code exchanges stop. Self-contained access JWTs can remain valid at downstream
resource servers until expiry; the confirmation page states this limitation. Other users and applications are retained.

## Localization and shared UI

messages.properties and messages_km.properties contain matching English/Khmer keys, including all eight assignment
keys. All template labels, help, validation, status and accessibility text use the message source; ISTAD/provider
names and stored domain values remain proper names/data. A HttpOnly language cookie persists ?lang=en|km, with
English fallback for unsupported languages. Consent language links preserve client_id/scope/state. Profile locale
is stored account data; the visible language follows the browser language selector.

Canonical layout, CSRF, field and notice templates live in jte/shared. Existing login.css remains the established
runtime token source; account.css adapts those tokens for every account screen. Native date/select popups are
intentionally browser/OS-owned; native date-picker chrome follows the browser locale, while application labels follow
the selected en/km locale. Forms provide labels, associated errors, visible focus, password visibility controls,
duplicate-submit protection and keyboard operation. Actual page unload warns about unsaved profile edits. Account
navigation can scroll at narrow widths; the document does not overflow. User-deleted DESIGN.md/UX-CONTRACT.md
were intentionally not recreated.

## Development and production templates

Default configuration uses precompiled templates. The matching gg.jte.gradle 3.2.5 plugin generates template Java
sources in iam-web, compiles them with Java 17, and packages their classes in the web dependency inside bootJar.
The packaged application requires no source .jte files or runtime template compilation. Run:

```sh
./gradlew check bootJar
java -jar iam-main/build/libs/iam-main-0.0.1-SNAPSHOT.jar
```

For live template editing, activate the dev profile (SPRING_PROFILES_ACTIVE=dev or --spring.profiles.active=dev).
The existing resolver locates iam-web/src/main/jte from supported project launch directories. Reload the Gradle
model in IntelliJ after introducing the JTE plugin. Development and precompiled modes are mutually exclusive.

[JTE build documentation](https://jte.gg/pre-compiling/) explains the generated-source packaging approach.

## Verification

Full IAM checks and bootJar pass: 180 reported tests, 179 passed, one PostgreSQL-only container skipped,
zero failures/errors. BrowserIntegrationTests adds 12 server-side cases covering routes, localized bundles,
registration privilege/secret boundaries, profile ownership/versioning, passwords, consent, disconnection and app
revocation. All prior REST, domain, persistence, Authorization Server and federation cases remain included.

The actual production JAR was launched from /tmp without template sources, with H2 and test changelogs supplied
only to the isolated smoke process through Boot's external loader path. The production JAR itself does not bundle
H2 or test classes. Packaged precompiled template classes were inspected. Chromium passed 48 workflow/layout
checks against that process: English/Khmer, desktop/mobile, keyboard, 200% zoom, error/empty/success states,
registration/login/password changes, stale saves, consent allow/cancel, token exchange and revocation/refresh denial.
Native locale/date popup screenshots were also checked. Browser console errors: zero. Strict UI audit: zero findings.
This does not claim a full independent accessibility or native-language audit.

No live db_oauth2 migration, provider-account acceptance login, provider/client secret rotation, signing-key change
or deployment was performed. Final README/Postman/requirements reconciliation, fresh PostgreSQL validation and live
provider acceptance remain the next delivery audit.
