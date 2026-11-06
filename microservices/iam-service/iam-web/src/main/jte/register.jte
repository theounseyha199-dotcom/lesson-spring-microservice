@import istad.ha.iamservice.web.view.Page
@import java.util.Map
@param Page p
@param Map<String,String> values
@param Map<String,String> errors
@param String errorCode
@template.shared.layout(p=p, title=p.t("register.title"), content=@`
<section class="card"><h1>${p.t("register.title")}</h1><p class="intro">${p.t("register.intro")}</p>
@template.shared.notice(p=p, errorCode=errorCode)
<form method="post" action="${p.url("/register")}" novalidate data-submit>@template.shared.csrf(p=p)
<input type="hidden" name="locale" value="${p.language()}">
@for(String name : java.util.List.of("username", "email", "givenName", "familyName"))
@template.shared.field(p=p, name=name, value=values.getOrDefault(name,""), type=name.equals("email") ? "email" : "text", autocomplete=name.equals("givenName") ? "given-name" : name.equals("familyName") ? "family-name" : name, required=true, max=name.equals("email") ? 255 : 100, error=errors.getOrDefault(name,""))
@endfor
<p class="hint">${p.t("password.hint")}</p>
@template.shared.field(p=p, name="password", type="password", autocomplete="new-password", required=true, max=1000, error=errors.getOrDefault("password",""))
@template.shared.field(p=p, name="confirmPassword", type="password", autocomplete="new-password", required=true, max=1000, error=errors.getOrDefault("confirmPassword",""))
<button type="submit">${p.t("register.submit")}</button></form>
<p>${p.t("register.haveAccount")} <a href="${p.url("/login")}">${p.t("login.title")}</a></p></section>
`)
