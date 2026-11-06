@import istad.ha.iamservice.web.view.Page
@param Page p
@param String errorCode = ""
@if(!errorCode.isEmpty())
<p class="notice danger" role="alert" tabindex="-1">${p.error(errorCode)}</p>
@endif
