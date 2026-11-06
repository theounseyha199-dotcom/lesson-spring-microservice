package istad.ha.iamservice.web.view;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
@ControllerAdvice
public class BrowserPageAdvice {
    private final MessageSource messages;
    public BrowserPageAdvice(MessageSource messages) { this.messages = messages; }
    @ModelAttribute("p") public Page page(HttpServletRequest request, Locale locale) {
        var csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        Locale supported = locale.getLanguage().equals("km") ? Locale.forLanguageTag("km") : Locale.ENGLISH;
        var languagePath = org.springframework.web.util.UriComponentsBuilder.fromPath(request.getRequestURI());
        if (request.getRequestURI().endsWith("/oauth2/consent")) {
            for (String key : java.util.List.of("client_id", "scope", "state")) {
                if (request.getParameter(key) != null) languagePath.queryParam(key, request.getParameter(key));
            }
        }
        return new Page(request.getContextPath(), csrf == null ? "_csrf" : csrf.getParameterName(),
                csrf == null ? "" : csrf.getToken(), supported, messages, languagePath.build().encode().toUriString());
    }
}
