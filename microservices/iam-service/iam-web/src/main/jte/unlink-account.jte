@import istad.ha.iamservice.web.view.Page
@import istad.ha.iamservice.application.result.IdentityResult
@param Page p
@param IdentityResult identity
@template.shared.layout(p=p,title=p.t("accounts.unlinkTitle"),account=true,content=@`
<section class="card narrow"><h1>${p.t("accounts.unlinkTitle")}</h1><p>${p.t("accounts.unlinkWarning",p.t("provider."+identity.provider().name().toLowerCase(java.util.Locale.ROOT)))}</p><div class="actions"><a class="button secondary" autofocus href="${p.url("/profile/accounts")}">${p.t("common.cancel")}</a><form method="post" action="${p.url("/profile/accounts/"+identity.uuid()+"/unlink")}" data-submit>@template.shared.csrf(p=p)<button class="danger-button" type="submit">${p.t("accounts.unlink")}</button></form></div></section>
`)
