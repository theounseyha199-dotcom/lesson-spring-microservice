@import istad.ha.iamservice.web.view.Page
@import gg.jte.Content
@param Page p
@param String title
@param boolean account = false
@param Content content
<!DOCTYPE html>
<html lang="${p.language()}"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>${title} · ISTAD</title><link rel="stylesheet" href="${p.url("/css/account.css")}"><script defer src="${p.url("/js/account.js")}"></script></head>
<body><header class="site-header"><a class="brand" href="${p.url(account ? "/" : "/login")}">ISTAD</a>
<nav aria-label="${p.t("common.language")}"><a href="${p.languageLink("en")}" lang="en" aria-current="${p.language().equals("en") ? "true" : "false"}">${p.t("common.english")}</a><a href="${p.languageLink("km")}" lang="km" aria-current="${p.language().equals("km") ? "true" : "false"}">${p.t("common.khmer")}</a></nav></header>
@if(account)
<nav class="account-nav" aria-label="${p.t("common.home")}"><a href="${p.url("/")}">${p.t("profile.title")}</a><a href="${p.url("/profile/edit")}">${p.t("profile.edit")}</a><a href="${p.url("/profile/password")}">${p.t("profile.password")}</a><a href="${p.url("/profile/accounts")}">${p.t("profile.linkedAccounts")}</a><a href="${p.url("/profile/apps")}">${p.t("profile.apps")}</a><form method="post" action="${p.url("/logout")}">@template.shared.csrf(p=p)<button class="link-button" type="submit">${p.t("common.logout")}</button></form></nav>
@endif
<main class="page ${account ? "account-page" : "auth-page"}">${content}</main>
</body></html>
