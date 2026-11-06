@param int status
@param String contextPath
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Unable to open page · ISTAD</title>
    <link rel="stylesheet" href="${contextPath}/css/login.css">
</head>
<body>
<main class="login-shell">
    <section class="form-panel" aria-labelledby="error-title">
        <h1 id="error-title">Unable to open this page</h1>
        <p>Error ${status}. Please try again.</p>
        <a href="${contextPath}/login">Return to sign in</a>
    </section>
</main>
</body>
</html>
