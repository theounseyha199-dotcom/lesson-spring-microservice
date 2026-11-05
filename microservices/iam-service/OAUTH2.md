# Form and social login

Open http://localhost:9000/login to use the username/password form or GitHub/Google buttons. Both methods preserve Spring Security saved authorization requests.

Set these values in the repository root `.local/runtime.properties` (ignored by Git), or export them as environment variables:

```properties
IAM_GITHUB_CLIENT_ID=your-github-client-id
IAM_GITHUB_CLIENT_SECRET=your-github-client-secret
IAM_GOOGLE_CLIENT_ID=your-google-client-id
IAM_GOOGLE_CLIENT_SECRET=your-google-client-secret
```

Register these exact callback URLs with the providers for local development:

- GitHub: `http://localhost:9000/login/oauth2/code/github`
- Google: `http://localhost:9000/login/oauth2/code/google`

Use matching scheme, host, and port when opening the app; replace these callbacks for other environments. GitHub uses `read:user` and `user:email`; Google uses `openid`, `profile`, and `email`. Provider credentials are separate from the IAM server's own registered client credentials. Rotate any credentials shared in chat before further use.

Social login authenticates the external principal; it does not link it to a local account or grant local user roles.
