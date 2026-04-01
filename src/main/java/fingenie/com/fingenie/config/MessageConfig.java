package fingenie.com.fingenie.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Configuration for internationalization (i18n) support.
 * 
 * Configures:
 * - MessageSource for loading error messages from properties files
 * - LocaleResolver for determining user locale from Accept-Language header
 * - Validator integration with message source for constraint validation
 */
@Configuration
public class MessageConfig {

    /**
     * Configures the message source for i18n error messages.
     * 
     * Properties files:
     * - messages.properties (default/fallback)
     * - messages_en.properties (English)
     * - messages_vi.properties (Vietnamese)
     * 
     * Features:
     * - UTF-8 encoding for proper character support
     * - 1 hour cache for production performance
     * - Fallback to system locale if message not found
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasenames("classpath:messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setCacheSeconds(3600); // 1 hour cache in production
        messageSource.setFallbackToSystemLocale(true);
        messageSource.setUseCodeAsDefaultMessage(true); // Return code if message not found
        return messageSource;
    }

    /**
     * Configures locale resolution based on Accept-Language header.
     * 
     * Supported locales:
     * - en (English) - default
     * - vi (Vietnamese)
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(List.of(
            Locale.ENGLISH,
            new Locale("vi")
        ));
        return resolver;
    }

    /**
     * Integrates the message source with Bean Validation.
     * This allows @Valid annotations to use i18n messages.
     */
    @Bean
    public LocalValidatorFactoryBean localValidatorFactoryBean(MessageSource messageSource) {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource);
        return bean;
    }
}
