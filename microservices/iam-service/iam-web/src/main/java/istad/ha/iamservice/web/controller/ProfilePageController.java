package istad.ha.iamservice.web.controller;
import istad.ha.iamservice.application.command.*;
import istad.ha.iamservice.application.usecase.*;
import istad.ha.iamservice.application.result.UserResult;
import istad.ha.iamservice.domain.model.Address;
import istad.ha.iamservice.web.view.*;
import java.time.LocalDate;
import java.util.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
public class ProfilePageController {
    private final BrowserUser current; private final UpdateUserProfileUseCase profiles; private final ChangePasswordUseCase passwords;
    public ProfilePageController(BrowserUser current, UpdateUserProfileUseCase profiles, ChangePasswordUseCase passwords) { this.current = current; this.profiles = profiles; this.passwords = passwords; }
    @GetMapping("/profile/edit") public String edit(jakarta.servlet.http.HttpServletRequest request, Model model) { var user = current.current(); model.addAttribute("saved", request.getParameterMap().containsKey("saved")); model.addAttribute("values", values(user)); model.addAttribute("errorCode", ""); return "profile-edit"; }
    @PostMapping("/profile/edit") public String edit(@RequestParam Map<String,String> input, Model model, HttpServletResponse response) {
        var user = current.current();
        var errors=Forms.profileErrors(input);
        if (!errors.isEmpty()) { response.setStatus(400); model.addAttribute("values",Forms.values(input)); model.addAttribute("errors",errors); model.addAttribute("errorCode","validation"); return "profile-edit"; }
        try {
            String birth = Forms.optional(input.get("birthdate"));
            Address address = new Address(Forms.optional(input.get("streetAddress")), Forms.optional(input.get("locality")), Forms.optional(input.get("region")), Forms.optional(input.get("postalCode")), Forms.optional(input.get("country")));
            if (List.of("streetAddress", "locality", "region", "postalCode", "country").stream().allMatch(k -> Forms.optional(input.get(k)) == null)) address = null;
            var patch = ProfilePatch.builder().email(input.get("email")).givenName(input.get("givenName")).familyName(input.get("familyName"))
                    .middleName(Forms.optional(input.get("middleName"))).nickname(Forms.optional(input.get("nickname"))).gender(Forms.optional(input.get("gender")))
                    .birthdate(birth == null ? null : LocalDate.parse(birth)).profileUrl(Forms.optional(input.get("profileUrl"))).pictureUrl(Forms.optional(input.get("pictureUrl")))
                    .websiteUrl(Forms.optional(input.get("websiteUrl"))).zoneinfo(Forms.optional(input.get("zoneinfo"))).locale(input.get("locale"))
                    .phoneNumber(Forms.optional(input.get("phoneNumber"))).address(address).build();
            long version = Long.parseLong(input.getOrDefault("version", "-1"));
            profiles.execute(new UpdateUserProfileCommand(user.uuid(), patch, version)); return "redirect:/profile/edit?saved";
        } catch (RuntimeException ex) {
            String code = Forms.code(ex); response.setStatus(code.equals("concurrent_update") ? 409 : 400);
            model.addAttribute("values", Forms.values(input)); model.addAttribute("errorCode", code); return "profile-edit";
        }
    }
    @GetMapping("/profile/password") public String password(jakarta.servlet.http.HttpServletRequest request, Model model) { current.current(); model.addAttribute("saved", request.getParameterMap().containsKey("saved")); model.addAttribute("errorCode", ""); return "change-password"; }
    @PostMapping("/profile/password") public String password(@RequestParam(name="currentPassword", defaultValue="") String old,
            @RequestParam(name="newPassword", defaultValue="") String password, @RequestParam(name="confirmPassword", defaultValue="") String confirmation,
            Model model, HttpServletResponse response) {
        var user = current.current();
        try { passwords.execute(new ChangePasswordCommand(user.uuid(), old, password, confirmation)); return "redirect:/profile/password?saved"; }
        catch (RuntimeException ex) {
            String code=Forms.code(ex); var errors=new LinkedHashMap<String,String>();
            if (code.equals("invalid_current_password")) errors.put("currentPassword","validation.currentPassword");
            if (ex instanceof istad.ha.iamservice.application.exception.ApplicationException app) {
                if (app.errors().containsKey("password")) errors.put("newPassword","validation.password");
                if (app.errors().containsKey("confirmPassword")) errors.put("confirmPassword","validation.confirm");
            }
            response.setStatus(400); model.addAttribute("errors",errors); model.addAttribute("errorCode", code); return "change-password";
        }
    }
    private Map<String,String> values(UserResult user) {
        var result = new LinkedHashMap<String,String>(); var profile = user.profile();
        result.put("version", Long.toString(user.version())); result.put("email", user.email()); result.put("givenName", profile.givenName()); result.put("familyName", profile.familyName());
        result.put("middleName", text(profile.middleName())); result.put("nickname", text(profile.nickname())); result.put("gender", text(profile.gender()));
        result.put("birthdate", text(profile.birthdate())); result.put("profileUrl", text(profile.profileUrl())); result.put("pictureUrl", text(profile.pictureUrl())); result.put("websiteUrl", text(profile.websiteUrl()));
        result.put("zoneinfo", text(profile.zoneinfo())); result.put("locale", profile.locale()); result.put("phoneNumber", text(profile.phoneNumber()));
        var address = profile.address();
        result.put("streetAddress", address == null ? "" : text(address.streetAddress())); result.put("locality", address == null ? "" : text(address.locality()));
        result.put("region", address == null ? "" : text(address.region())); result.put("postalCode", address == null ? "" : text(address.postalCode())); result.put("country", address == null ? "" : text(address.country())); return result;
    }
    private String text(Object value) { return value == null ? "" : value.toString(); }
}
