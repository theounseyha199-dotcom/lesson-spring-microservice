@import istad.ha.iamservice.web.view.Page
@import istad.ha.iamservice.application.result.ConnectedAppResult
@param Page p
@param java.util.List<ConnectedAppResult> apps
@param String errorCode
@param boolean revoked = false
@template.shared.layout(p=p,title=p.t("profile.apps"),account=true,content=@`
<section class="card"><h1>${p.t("profile.apps")}</h1><p>${p.t("apps.intro")}</p>
@if(revoked)
<p class="notice success" role="status">${p.t("apps.revoked")}</p>
@endif
@template.shared.notice(p=p,errorCode=errorCode)
@if(apps.isEmpty())
<p class="empty">${p.t("apps.empty")}</p>
@else
<ul class="record-list">
@for(var app : apps)
<li><div><h2>${app.name()}</h2><ul>
@for(String scope : new java.util.TreeSet<>(app.scopes()))
<li>${p.scope(scope)}</li>
@endfor
</ul></div><a class="button secondary" href="${p.url("/profile/apps/"+app.id()+"/revoke")}">${p.t("apps.revoke")}</a></li>
@endfor
</ul>
@endif
</section>
`)
