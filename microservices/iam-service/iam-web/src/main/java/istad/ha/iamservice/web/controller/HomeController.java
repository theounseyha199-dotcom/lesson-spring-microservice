package istad.ha.iamservice.web.controller;
import istad.ha.iamservice.web.view.BrowserUser;
import istad.ha.iamservice.application.usecase.GetUserAuthoritiesUseCase;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class HomeController {
    private final BrowserUser current; private final GetUserAuthoritiesUseCase authorities;
    public HomeController(BrowserUser current, GetUserAuthoritiesUseCase authorities) { this.current = current; this.authorities = authorities; }
    @GetMapping("/") public String home(Model model) {
        var user = current.current(); model.addAttribute("user", user);
        var claims = new java.util.LinkedHashMap<String,String>();
        claims.put("subject",user.uuid().toString()); claims.put("username",user.username()); claims.put("email",user.email());
        var profile = user.profile();
        claims.put("givenName",profile.givenName()); claims.put("familyName",profile.familyName());
        claims.put("middleName",text(profile.middleName()));
        claims.put("nickname",text(profile.nickname()));
        claims.put("gender",text(profile.gender()));
        claims.put("birthdate",text(profile.birthdate()));
        claims.put("profileUrl",text(profile.profileUrl()));
        claims.put("pictureUrl",text(profile.pictureUrl()));
        claims.put("websiteUrl",text(profile.websiteUrl()));
        claims.put("zoneinfo",text(profile.zoneinfo()));
        claims.put("locale",text(profile.locale()));
        claims.put("phoneNumber",text(profile.phoneNumber()));
        var address = profile.address();
        if (address != null) { claims.put("streetAddress",text(address.streetAddress())); claims.put("locality",text(address.locality())); claims.put("region",text(address.region())); claims.put("postalCode",text(address.postalCode())); claims.put("country",text(address.country())); }
        model.addAttribute("claims", claims);
        model.addAttribute("authorities", authorities.execute(user.uuid())); return "index";
    }
    private String text(Object value) { return value == null ? "" : value.toString(); }
}
