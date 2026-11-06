@import istad.ha.iamservice.web.view.Page
@param Page p
@param String name
@param String value = ""
@param String type = "text"
@param String autocomplete = "off"
@param int max = 255
@param boolean required = false
@param String error = ""
<div class="field"><label for="${name}">${p.t("field." + name)} <span class="muted">${p.t(required ? "common.required" : "common.optional")}</span></label>
<div class="input-row"><input id="${name}" name="${name}" type="${type}" value="${value}" autocomplete="${autocomplete}" maxlength="${max}" required="${required}" aria-invalid="${Boolean.toString(!error.isEmpty())}" aria-describedby="${name}-error">
@if(type.equals("password"))
<button type="button" class="secondary password-toggle" data-password="${name}" data-show-text="${p.t("common.show")}" data-hide-text="${p.t("common.hide")}" data-show="${p.t("common.showPassword")}" data-hide="${p.t("common.hidePassword")}" aria-label="${p.t("common.showPassword")}" aria-pressed="false" hidden>${p.t("common.show")}</button>
@endif
</div><p id="${name}-error" class="field-error">${error.isEmpty() ? "" : p.t(error)}</p></div>
