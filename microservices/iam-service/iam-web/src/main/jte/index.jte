@import istad.ha.iamservice.web.view.Page
@import istad.ha.iamservice.application.result.UserResult
@import istad.ha.iamservice.application.result.UserAuthoritiesResult
@param Page p
@param UserResult user
@param UserAuthoritiesResult authorities
@param java.util.Map<String,String> claims
@template.shared.layout(p=p, title=p.t("profile.title"), account=true, content=@`
<div class="page-heading"><p class="eyebrow">ISTAD</p><h1>${p.t("profile.title")}</h1><p>${p.t("profile.intro")}</p></div>
<div class="two-columns"><section class="card"><h2>${p.t("profile.details")}</h2><dl>
@for(var claim : claims.entrySet())
<div><dt>${p.t("field."+claim.getKey())}</dt><dd>${claim.getValue().isBlank() ? p.t("common.notProvided") : claim.getValue()}</dd></div>
@endfor
<div><dt>${p.t("field.emailVerified")}</dt><dd>${p.t(user.emailVerified() ? "common.yes" : "common.no")}</dd></div>
<div><dt>${p.t("field.phoneVerified")}</dt><dd>${p.t(user.profile().phoneNumberVerified() ? "common.yes" : "common.no")}</dd></div>
</dl></section><section class="card"><h2>${p.t("profile.access")}</h2><h3>${p.t("profile.roles")}</h3><ul>
@for(var role : authorities.roles())
<li>${role.name()}</li>
@endfor
</ul><h3>${p.t("profile.permissions")}</h3>
@if(authorities.effectivePermissions().isEmpty())
<p>${p.t("profile.noPermissions")}</p>
@else
<ul>
@for(String permission : new java.util.TreeSet<>(authorities.effectivePermissions()))
<li>${permission}</li>
@endfor
</ul>
@endif
</section></div>
`)
