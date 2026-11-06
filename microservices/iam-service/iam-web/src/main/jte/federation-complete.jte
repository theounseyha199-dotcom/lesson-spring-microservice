@import istad.ha.iamservice.web.view.Page
@param Page p
@param String flow
@param String error
@param String email
@param String givenName
@param String familyName
@param boolean needEmail
@param boolean needGivenName
@param boolean needFamilyName
@param java.util.Map<String,String> fieldErrors
@template.shared.layout(p=p,title=p.t("completion.title"),content=@`
<section class="card"><h1>${p.t("completion.title")}</h1><p>${p.t("completion.intro")}</p>
@template.shared.notice(p=p,errorCode=error)
<form method="post" action="${p.url("/federation/complete")}" novalidate data-submit>@template.shared.csrf(p=p)<input type="hidden" name="flow" value="${flow}">
@if(needEmail)
@template.shared.field(p=p,name="email",value=email,type="email",autocomplete="email",required=true,error=fieldErrors.getOrDefault("email",""))
<p class="hint">${p.t("completion.emailHint")}</p>
@endif
@if(needGivenName)
@template.shared.field(p=p,name="givenName",value=givenName,autocomplete="given-name",required=true,max=100,error=fieldErrors.getOrDefault("givenName",""))
@endif
@if(needFamilyName)
@template.shared.field(p=p,name="familyName",value=familyName,autocomplete="family-name",required=true,max=100,error=fieldErrors.getOrDefault("familyName",""))
@endif
<button type="submit">${p.t("completion.submit")}</button></form><form method="post" action="${p.url("/federation/cancel")}">@template.shared.csrf(p=p)<button class="secondary" type="submit">${p.t("completion.cancel")}</button></form></section>
`)
