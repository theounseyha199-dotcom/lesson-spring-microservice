package istad.ha.iamservice.web.config;
import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.i18n.*;
@Configuration
public class BrowserConfig implements WebMvcConfigurer {
    @Bean public LocaleResolver localeResolver() { var resolver = new CookieLocaleResolver("iam-language"); resolver.setDefaultLocale(Locale.ENGLISH); resolver.setCookieHttpOnly(true); return resolver; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        var interceptor = new LocaleChangeInterceptor(); interceptor.setParamName("lang");
        interceptor.setIgnoreInvalidLocale(true); registry.addInterceptor(interceptor);
    }
}
