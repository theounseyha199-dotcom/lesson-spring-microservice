@param String username
@param String contextPath
@param String csrfParameterName
@param String csrfToken
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Account · ISTAD</title>
    <link rel="stylesheet" href="${contextPath}/css/login.css">
</head>
<body>
<main class="login-shell">
    <section class="form-panel" aria-labelledby="account-title">
        <h1 id="account-title">You’re signed in</h1>
        <p>Welcome, ${username}.</p>
        <form method="post" action="${contextPath}/logout">
            <input type="hidden" name="${csrfParameterName}" value="${csrfToken}">
            <button type="submit" class="btn-submit">Sign out</button>
        </form>
    </section>
</main>
</body>
</html>
