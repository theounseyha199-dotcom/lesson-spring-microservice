# IAM interface contract

| Capability | Canonical owner | Source of truth | Allowed variants | Verification |
| --- | --- | --- | --- | --- |
| Form | login.html and login.js | Spring SecurityConfig and LoginController | sign-in | IamServiceApplicationTests |
| Scrollbar | login.css root | DESIGN.md | native scroll behavior | static audit |

Authentication policy comes from `src/main/java/istad/ha/iamservice/config/SecurityConfig.java` and the existing user details service. POST /login is owned by Spring Security with CSRF enabled. Thymeleaf inserts the CSRF hidden field. Successful authentication follows the saved request, falling back to /. Failure returns to /login?error and logout to /login?logout. Messages do not expose account existence. Passwords are never persisted in browser storage. Native form POST remains functional without JavaScript; JavaScript adds empty-field feedback, show/hide, and duplicate-submit prevention. No unsaved-change prompt applies to credentials.
