package istad.ha.iamservice.web.view;
import java.util.Locale;
import org.springframework.context.MessageSource;
public record Page(String contextPath, String csrfName, String csrfToken, Locale locale, MessageSource messages, String languagePath) {
    public String t(String key, Object... arguments) { return messages.getMessage(key, arguments, locale); }
    public String language() { return locale.getLanguage().equals("km") ? "km" : "en"; }
    public String languageLink(String language) { return languagePath + (languagePath.contains("?") ? "&" : "?") + "lang=" + language; }
    public String error(String code) { return messages.getMessage("error."+code, null, t("error.generic"), locale); }
    public String scope(String scope) { return messages.getMessage("scope."+scope, null, scope, locale); }
    public String url(String path) { return contextPath + path; }
}
