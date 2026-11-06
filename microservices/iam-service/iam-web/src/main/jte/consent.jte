@import istad.ha.iamservice.web.view.Page
@import istad.ha.iamservice.application.result.ConnectedAppResult
@param Page p
@param ConnectedAppResult client
@param String clientId
@param String state
@template.shared.layout(p=p,title=p.t("consent.title"),account=true,content=@`
<section class="card narrow"><h1>${p.t("consent.title")}</h1><p>${p.t("consent.intro",client.name())}</p><p>${p.t("consent.hint")}</p>
<form method="post" action="${p.url("/oauth2/authorize")}" data-submit>@template.shared.csrf(p=p)<input type="hidden" name="client_id" value="${clientId}"><input type="hidden" name="state" value="${state}">
@for(String scope : new java.util.TreeSet<>(client.scopes()))
@if(!scope.equals("openid"))
<label class="check"><input type="checkbox" name="scope" value="${scope}" checked>${p.scope(scope)}</label>
@else
<p>${p.scope(scope)}</p>
@endif
@endfor
<button type="submit">${p.t("consent.allow")}</button></form>
<form method="post" action="${p.url("/oauth2/authorize")}" data-submit>@template.shared.csrf(p=p)<input type="hidden" name="client_id" value="${clientId}"><input type="hidden" name="state" value="${state}"><button class="secondary" type="submit">${p.t("consent.cancel")}</button></form></section>
`)
