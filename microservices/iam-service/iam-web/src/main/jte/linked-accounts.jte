@import istad.ha.iamservice.web.view.Page
@import istad.ha.iamservice.application.result.IdentityResult
@param Page p
@param java.util.List<IdentityResult> identities
@param boolean linked
@param boolean failed
@param boolean unlinked = false
@param String errorCode = ""
@template.shared.layout(p=p,title=p.t("profile.linkedAccounts"),account=true,content=@`
<section class="card"><h1>${p.t("profile.linkedAccounts")}</h1><p>${p.t("accounts.intro")}</p>
@if(linked || unlinked)
<p class="notice success" role="status">${p.t(linked ? "accounts.linked" : "accounts.unlinked")}</p>
@endif
@if(failed)
@template.shared.notice(p=p,errorCode=errorCode.isEmpty() ? "generic" : errorCode)
@endif
@if(identities.isEmpty())
<p class="empty">${p.t("accounts.empty")}</p>
@else
<ul class="record-list">
@for(var identity : identities)
<li><div><h2>${p.t("provider."+identity.provider().name().toLowerCase(java.util.Locale.ROOT))}</h2><p>${identity.displayName() == null ? p.t("accounts.connected") : identity.displayName()}</p></div><a class="button secondary" href="${p.url("/profile/accounts/"+identity.uuid()+"/unlink")}">${p.t("accounts.unlink")}</a></li>
@endfor
</ul>
@endif
<div class="provider-grid">
@for(String provider : java.util.List.of("google", "github", "facebook", "telegram"))
<form method="post" action="${p.url("/profile/accounts/link/"+provider)}" data-submit>@template.shared.csrf(p=p)<button class="secondary" type="submit">${p.t("accounts.connect",p.t("provider."+provider))}</button></form>
@endfor
</div></section>
`)
