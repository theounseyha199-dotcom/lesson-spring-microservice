@param String contextPath
@param String csrfParameterName
@param String csrfToken
@param boolean loginError
@param boolean loggedOut
@param boolean oauth2Error

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sign in · ISTAD</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Playfair+Display:ital,wght@0,400;0,600;1,400&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${contextPath}/css/login.css">
    <script defer src="${contextPath}/js/login.js"></script>
</head>
<body>
<main class="login-shell">
    <!-- Animated background shapes -->
    <div class="bg-shapes" aria-hidden="true">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
    </div>

    <aside class="identity" aria-label="ISTAD account">
        <a class="brand" href="${contextPath}/login" aria-label="ISTAD sign in">
            <span class="brand-mark" aria-hidden="true">
                <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
                    <path d="M14 2L26 8V20L14 26L2 20V8L14 2Z" fill="currentColor" opacity="0.9"/>
                    <path d="M14 6L22 10V18L14 22L6 18V10L14 6Z" fill="white" opacity="0.3"/>
                </svg>
            </span>
            ISTAD
        </a>
        <div class="identity-copy">
            <p class="eyebrow">YOUR ISTAD ACCOUNT</p>
            <h1>One account.<br>Endless<br><em>possibilities.</em></h1>
            <p class="tagline">A single place to sign in and access all your ISTAD services securely.</p>
        </div>
        <div class="identity-stats" aria-hidden="true">
            <div class="stat">
                <span class="stat-number">10+</span>
                <span class="stat-label">Services</span>
            </div>
            <div class="stat">
                <span class="stat-number">99.9%</span>
                <span class="stat-label">Uptime</span>
            </div>
            <div class="stat">
                <span class="stat-number">256</span>
                <span class="stat-label">bit Encryption</span>
            </div>
        </div>
        <p class="identity-footer">
            <span class="footer-dot" aria-hidden="true"></span>
            Institute of Science and Technology Advanced Development
        </p>
    </aside>

    <section class="form-panel" aria-labelledby="form-title">
        <div class="form-content">
            <div class="form-header">
                <p class="eyebrow">WELCOME BACK</p>
                <h2 id="form-title">Sign in</h2>
                <p class="intro">Enter your credentials to access your account.</p>
            </div>

            @if(loginError)
            <div class="notice error" role="alert">
                <svg class="notice-icon" width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                    <circle cx="10" cy="10" r="9" stroke="currentColor" stroke-width="1.5"/>
                    <path d="M10 6v5M10 13.5v.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
                <span>Unable to sign in. Check your username and password and try again.</span>
            </div>
            @endif
            @if(loggedOut)
            <div class="notice success" role="status">
                <svg class="notice-icon" width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                    <circle cx="10" cy="10" r="9" stroke="currentColor" stroke-width="1.5"/>
                    <path d="M6 10l3 3 5-6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>You have been signed out successfully.</span>
            </div>
            @endif

            @if(oauth2Error)
            <div class="notice error" role="alert">
                <span>Unable to sign in with your provider. Try again or use your username and password.</span>
            </div>
            @endif

            <form id="login-form" action="${contextPath}/login" method="post" novalidate>
                <input type="hidden" name="${csrfParameterName}" value="${csrfToken}">
                <div class="field">
                    <label for="username">Username</label>
                    <div class="input-wrap">
                        <svg class="input-icon" width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                            <circle cx="10" cy="7" r="4" stroke="currentColor" stroke-width="1.5"/>
                            <path d="M3 18c0-3.3 3.1-6 7-6s7 2.7 7 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                        </svg>
                        <input id="username" name="username" type="text" placeholder="Enter your username" autocomplete="username" autocapitalize="none" spellcheck="false" required aria-describedby="username-error" autofocus>
                    </div>
                    <p class="field-error" id="username-error" aria-live="polite"></p>
                </div>
                <div class="field">
                    <label for="password">Password</label>
                    <div class="input-wrap password-wrap">
                        <svg class="input-icon" width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                            <rect x="4" y="9" width="12" height="9" rx="2" stroke="currentColor" stroke-width="1.5"/>
                            <path d="M7 9V6a3 3 0 016 0v3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                            <circle cx="10" cy="13.5" r="1.5" fill="currentColor"/>
                        </svg>
                        <input id="password" name="password" type="password" placeholder="Enter your password" autocomplete="current-password" required aria-describedby="password-error">
                        <button class="password-toggle" type="button" aria-controls="password" aria-label="Show password" aria-pressed="false" hidden disabled>
                            <svg class="eye-open" width="20" height="20" viewBox="0 0 20 20" fill="none">
                                <path d="M2 10s3-6 8-6 8 6 8 6-3 6-8 6-8-6-8-6z" stroke="currentColor" stroke-width="1.5"/>
                                <circle cx="10" cy="10" r="3" stroke="currentColor" stroke-width="1.5"/>
                            </svg>
                            <svg class="eye-closed" width="20" height="20" viewBox="0 0 20 20" fill="none" style="display:none">
                                <path d="M2 10s3-6 8-6 8 6 8 6-3 6-8 6-8-6-8-6z" stroke="currentColor" stroke-width="1.5"/>
                                <line x1="3" y1="3" x2="17" y2="17" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                            </svg>
                        </button>
                    </div>
                    <p class="field-error" id="password-error" aria-live="polite"></p>
                </div>
                <button class="submit" type="submit">
                    <span class="submit-text">Sign in</span>
                    <svg class="submit-arrow" width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                        <path d="M4 10h12M12 6l4 4-4 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    <svg class="submit-spinner" width="20" height="20" viewBox="0 0 20 20" fill="none" aria-hidden="true" style="display:none">
                        <circle cx="10" cy="10" r="8" stroke="currentColor" stroke-width="2" stroke-dasharray="40 20" stroke-linecap="round"/>
                    </svg>
                </button>
                <p id="submit-status" class="submit-status" role="status"></p>
            </form>

            <div class="divider" aria-hidden="true">
                <span>or</span>
            </div>

            <nav class="social-login" aria-label="Other sign-in options">
                <a class="social-button" href="${contextPath}/oauth2/authorization/github">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false">
                        <path d="M12 .75a11.25 11.25 0 0 0-3.56 21.92c.56.1.77-.24.77-.54v-2.1c-3.13.68-3.79-1.33-3.79-1.33-.51-1.3-1.25-1.65-1.25-1.65-1.02-.7.08-.69.08-.69 1.13.08 1.72 1.16 1.72 1.16 1 .1.77 2.02 3.28 1.43.1-.73.39-1.23.71-1.51-2.5-.28-5.13-1.25-5.13-5.56 0-1.23.44-2.23 1.16-3.02-.12-.28-.5-1.43.11-2.98 0 0 .95-.3 3.09 1.15a10.8 10.8 0 0 1 5.62 0c2.15-1.45 3.09-1.15 3.09-1.15.61 1.55.23 2.7.11 2.98.72.79 1.16 1.79 1.16 3.02 0 4.32-2.64 5.27-5.15 5.55.4.35.76 1.03.76 2.08v2.62c0 .3.2.65.78.54A11.25 11.25 0 0 0 12 .75Z"/>
                    </svg>
                    <span>Continue with GitHub</span>
                </a>
                <a class="social-button" href="${contextPath}/oauth2/authorization/google">
                    <svg width="20" height="20" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                        <path fill="#4285F4" d="M21.6 12.23c0-.71-.06-1.39-.18-2.05H12v3.88h5.38a4.6 4.6 0 0 1-2 3.02v2.51h3.24c1.9-1.75 2.98-4.33 2.98-7.36Z"/>
                        <path fill="#34A853" d="M12 22c2.7 0 4.96-.9 6.62-2.41l-3.24-2.51c-.9.6-2.05.97-3.38.97-2.6 0-4.81-1.76-5.6-4.13H3.06v2.59A10 10 0 0 0 12 22Z"/>
                        <path fill="#FBBC05" d="M6.4 13.92a6 6 0 0 1 0-3.84V7.49H3.06a10 10 0 0 0 0 9.02l3.34-2.59Z"/>
                        <path fill="#EA4335" d="M12 5.95c1.47 0 2.79.51 3.83 1.52l2.87-2.87A9.6 9.6 0 0 0 12 2a10 10 0 0 0-8.94 5.49l3.34 2.59A5.99 5.99 0 0 1 12 5.95Z"/>
                    </svg>
                    <span>Continue with Google</span>
                </a>
                <a class="social-button" href="${contextPath}/oauth2/authorization/facebook">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false">
                        <path d="M14 22v-9h3l.5-4H14V7c0-1.2.4-2 2-2h2V1.4A24 24 0 0 0 15 1c-3 0-5 1.8-5 5v3H7v4h3v9z"/>
                    </svg>
                    <span>Continue with Facebook</span>
                </a>
                <a class="social-button" href="${contextPath}/oauth2/authorization/telegram">
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false">
                        <path d="M21.5 3.5 18 20l-6-4.5-3 3 .7-5.1L19 5.5 7.5 12.4 2 10.5z"/>
                    </svg>
                    <span>Continue with Telegram</span>
                </a>
            </nav>

            <p class="help">Need help signing in? Contact your administrator.</p>
        </div>
        <footer>
            <span class="footer-secured">
                <svg width="14" height="14" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                    <path d="M10 2l7 3v5c0 4.4-3 7.5-7 9-4-1.5-7-4.6-7-9V5l7-3z" stroke="currentColor" stroke-width="1.5"/>
                    <path d="M7 10l2 2 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                Secured connection
            </span>
            <span aria-hidden="true">·</span>
            <span>ISTAD Platform</span>
        </footer>
    </section>
</main>
</body>
</html>
