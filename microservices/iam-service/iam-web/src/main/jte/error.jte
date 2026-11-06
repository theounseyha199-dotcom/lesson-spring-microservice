@import istad.ha.iamservice.web.view.Page
@param Page p
@param int status
@template.shared.layout(p=p,title=p.t("error.pageTitle"),content=@`
<section class="card"><h1>${p.t("error.pageTitle")}</h1><p>${p.t("error.page",status)}</p><a href="${p.url("/login")}">${p.t("login.title")}</a></section>
`)
