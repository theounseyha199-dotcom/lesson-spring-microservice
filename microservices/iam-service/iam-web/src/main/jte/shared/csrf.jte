@import istad.ha.iamservice.web.view.Page
@param Page p
<input type="hidden" name="${p.csrfName()}" value="${p.csrfToken()}">
