@import istad.ha.iamservice.web.view.Page
@param Page p
@param String errorCode
@param java.util.Map<String,String> errors = java.util.Map.of()
@param boolean saved = false
@template.shared.layout(p=p, title=p.t("profile.password"), account=true, content=@`
<section class="card narrow"><h1>${p.t("profile.password")}</h1><p>${p.t("profile.passwordHint")}</p><p class="hint">${p.t("password.hint")}</p>
@if(saved)
<p class="notice success" role="status">${p.t("common.saved")}</p>
@endif
@template.shared.notice(p=p, errorCode=errorCode)
<form method="post" action="${p.url("/profile/password")}" novalidate data-submit>@template.shared.csrf(p=p)
@template.shared.field(p=p,name="currentPassword",error=errors.getOrDefault("currentPassword",""),type="password",autocomplete="current-password",required=true,max=1000)
@template.shared.field(p=p,name="newPassword",error=errors.getOrDefault("newPassword",""),type="password",autocomplete="new-password",required=true,max=1000)
@template.shared.field(p=p,name="confirmPassword",error=errors.getOrDefault("confirmPassword",""),type="password",autocomplete="new-password",required=true,max=1000)
<button type="submit">${p.t("profile.password")}</button></form></section>
`)
