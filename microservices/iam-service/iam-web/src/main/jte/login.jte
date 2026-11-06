@import istad.ha.iamservice.web.view.Page
@import java.util.List
@param Page p
@param boolean loginError
@param boolean loggedOut
@param boolean oauth2Error
@param boolean registered = false
@template.shared.layout(p=p, title=p.t("login.title"), content=@`
<section class="card"><p class="eyebrow">ISTAD</p><h1>${p.t("login.title")}</h1><p class="intro">${p.t("login.intro")}</p>
@if(loginError)
<p class="notice danger" role="alert" tabindex="-1">${p.t("login.error")}</p>
@endif
@if(loggedOut)
<p class="notice success" role="status">${p.t("login.logout")}</p>
@endif
@if(oauth2Error)
<p class="notice danger" role="alert" tabindex="-1">${p.t("login.providerError")}</p>
@endif
@if(registered)
<p class="notice success" role="status">${p.t("login.registered")}</p>
@endif
<form method="post" action="${p.url("/login")}" novalidate data-submit>@template.shared.csrf(p=p)
<div class="field"><label for="username">${p.t("login.username")}</label><input id="username" name="username" autocomplete="username" required></div>
@template.shared.field(p=p, name="password", type="password", autocomplete="current-password", required=true, max=1000)
<button type="submit">${p.t("login.title")}</button></form>
<p class="divider">${p.t("login.alternative")}</p><div class="provider-grid">
@for(String provider : List.of("google", "github", "facebook", "telegram"))
<a class="button secondary" href="${p.url("/oauth2/authorization/"+provider)}">${p.t("login.continueWith", p.t("provider."+provider))}</a>
@endfor
</div><p>${p.t("login.noAccount")} <a href="${p.url("/register")}">${p.t("register.title")}</a></p></section>
`)
