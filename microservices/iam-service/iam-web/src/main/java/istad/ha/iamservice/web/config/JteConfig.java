package istad.ha.iamservice.web.config;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import gg.jte.springframework.boot.autoconfigure.JteProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Configuration
@EnableConfigurationProperties(JteProperties.class)
public class JteConfig {
    @Bean
    public org.springframework.boot.webmvc.autoconfigure.error.ErrorViewResolver jteErrorViewResolver() {
        return (request, status, model) -> {
            var view = new org.springframework.web.servlet.ModelAndView("error");
            view.addObject("status", status.value());
            view.addObject("contextPath", request.getContextPath());
            view.setStatus(status);
            return view;
        };
    }

    @Bean
    public TemplateEngine jteTemplateEngine(JteProperties properties) {
        if (properties.isDevelopmentMode() && properties.isUsePrecompiledTemplates()) {
            throw new IllegalStateException("JTE development mode and precompiled templates cannot both be enabled");
        }
        if (properties.isUsePrecompiledTemplates()) {
            return TemplateEngine.createPrecompiled(ContentType.Html);
        }
        if (!properties.isDevelopmentMode()) {
            throw new IllegalStateException("Enable JTE development mode or precompiled templates");
        }

        Path configured = Path.of(properties.getTemplateLocation());
        Path templates = configured;
        // IntelliJ may launch this module from either composite-build root.
        if (!configured.isAbsolute() && configured.equals(Path.of("src/main/jte"))) {
            templates = List.of(configured,
                            Path.of("iam-web").resolve(configured),
                            Path.of("../iam-web").resolve(configured),
                            Path.of("iam-service/iam-web").resolve(configured),
                            Path.of("microservices/iam-service/iam-web").resolve(configured))
                    .stream().filter(path -> Files.isRegularFile(path.resolve("login.jte")))
                    .findFirst().orElse(configured);
        }
        if (!Files.isRegularFile(templates.resolve("login" + properties.getTemplateSuffix()))) {
            throw new IllegalStateException("JTE login template not found at " + templates.toAbsolutePath()
                    + ". Set gg.jte.templateLocation to the IAM template directory.");
        }
        var engine = TemplateEngine.create(new DirectoryCodeResolver(templates),
                Path.of(properties.getClassDirectory()), ContentType.Html, getClass().getClassLoader());
        engine.setTrimControlStructures(properties.isTrimControlStructures());
        return engine;
    }
}
