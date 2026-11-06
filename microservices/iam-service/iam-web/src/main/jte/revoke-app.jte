@import istad.ha.iamservice.web.view.Page
@import istad.ha.iamservice.application.result.ConnectedAppResult
@param Page p
@param ConnectedAppResult app
@template.shared.layout(p=p,title=p.t("apps.revokeTitle"),account=true,content=@`
<section class="card narrow"><h1>${p.t("apps.revokeTitle")}</h1><p>${p.t("apps.warning",app.name())}</p>
<div class="actions"><a class="button secondary" autofocus href="${p.url("/profile/apps")}">${p.t("common.cancel")}</a><form method="post" action="${p.url("/profile/apps/"+app.id()+"/revoke")}" data-submit>@template.shared.csrf(p=p)<button type="submit" class="danger-button">${p.t("apps.revoke")}</button></form></div></section>
`)
