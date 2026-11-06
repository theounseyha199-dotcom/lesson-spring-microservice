@import istad.ha.iamservice.web.view.Page
@param Page p
@param java.util.Map<String,String> values
@param String errorCode
@param java.util.Map<String,String> errors = java.util.Map.of()
@param boolean saved = false
@template.shared.layout(p=p, title=p.t("profile.edit"), account=true, content=@`
<section class="card"><h1>${p.t("profile.edit")}</h1><p>${p.t("profile.editHint")}</p>
@if(saved)
<p class="notice success" role="status">${p.t("common.saved")}</p>
@endif
@template.shared.notice(p=p,errorCode=errorCode)
@if(errorCode.equals("concurrent_update"))
<a href="${p.url("/profile/edit")}">${p.t("common.reload")}</a>
@endif
<form method="post" action="${p.url("/profile/edit")}" novalidate data-submit data-dirty>@template.shared.csrf(p=p)
<input type="hidden" name="version" value="${values.getOrDefault("version", "-1")}"><div class="form-grid">
@for(String name : java.util.List.of("email", "givenName", "familyName", "middleName", "nickname", "gender", "birthdate", "profileUrl", "pictureUrl", "websiteUrl", "zoneinfo", "phoneNumber", "streetAddress", "locality", "region", "postalCode", "country"))
@if(name.equals("gender"))
<div class="field"><label for="gender">${p.t("field.gender")} <span class="muted">${p.t("common.optional")}</span></label><select id="gender" name="gender" aria-invalid="${Boolean.toString(errors.containsKey("gender"))}" aria-describedby="gender-error"><option value="">${p.t("common.notProvided")}</option>
@for(String gender : java.util.List.of("female","male","other"))
<option value="${gender}" selected="${values.getOrDefault("gender", "").equals(gender)}">${p.t("gender."+gender)}</option>
@endfor
</select><p id="gender-error" class="field-error">${errors.containsKey("gender") ? p.t(errors.get("gender")) : ""}</p></div>
@else
@template.shared.field(p=p, name=name, value=values.getOrDefault(name,""), type=name.equals("email") ? "email" : name.equals("birthdate") ? "date" : name.endsWith("Url") ? "url" : name.equals("phoneNumber") ? "tel" : "text", required=java.util.Set.of("email","givenName","familyName").contains(name), max=name.endsWith("Url") ? 500 : name.equals("email") || name.equals("streetAddress") ? 255 : 100, error=errors.getOrDefault(name,""))
@endif
@endfor
<div class="field"><label for="locale">${p.t("field.locale")}</label><select id="locale" name="locale"><option value="en" selected="${values.getOrDefault("locale","en").equals("en")}">${p.t("common.english")}</option><option value="km" selected="${values.getOrDefault("locale","en").equals("km")}">${p.t("common.khmer")}</option></select></div>
</div><button type="submit">${p.t("common.save")}</button></form></section>
`)
