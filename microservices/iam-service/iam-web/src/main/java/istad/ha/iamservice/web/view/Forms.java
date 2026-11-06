package istad.ha.iamservice.web.view;
import istad.ha.iamservice.application.exception.ApplicationException;
import istad.ha.iamservice.domain.exception.DomainException;
import java.util.*;
public final class Forms {
    private Forms() {}
    public static String optional(String value) { return value == null || value.isBlank() ? null : value.strip(); }
    public static String code(RuntimeException ex) {
        if (ex instanceof ApplicationException app) return app.code();
        if (ex instanceof DomainException domain) return domain.code();
        if (ex instanceof java.time.DateTimeException || ex instanceof NumberFormatException) return "validation";
        throw ex;
    }
    public static Map<String, String> values(Map<String, String> input) {
        var result = new LinkedHashMap<String, String>();
        input.forEach((key,value) -> { if (!key.toLowerCase(Locale.ROOT).contains("password") && !key.equals("_csrf")) result.put(key,value); });
        return result;
    }
    public static Map<String,String> profileErrors(Map<String,String> input) {
        var errors = new LinkedHashMap<String,String>();
        for (String key : List.of("email","givenName","familyName","locale")) if (optional(input.get(key)) == null) errors.put(key,"validation.required");
        try { istad.ha.iamservice.domain.rule.Checks.email(input.get("email")); } catch (DomainException ex) { errors.put("email","validation.email"); }
        for (String key : List.of("givenName","familyName","middleName","nickname","locality","region"))
            if (input.getOrDefault(key,"").strip().length() > 100) errors.put(key,"validation.name");
        for (String key : List.of("profileUrl","pictureUrl","websiteUrl")) try { istad.ha.iamservice.domain.rule.Checks.url(optional(input.get(key)),key); }
            catch (DomainException ex) { errors.put(key,"validation.value"); }
        String gender=optional(input.get("gender")); if (gender != null && !Set.of("female","male","other").contains(gender.toLowerCase(Locale.ROOT))) errors.put("gender","validation.value");
        String birth=optional(input.get("birthdate")); if (birth != null) try { java.time.LocalDate.parse(birth); } catch (java.time.DateTimeException ex) { errors.put("birthdate","validation.value"); }
        String zone=optional(input.get("zoneinfo")); if (zone != null && !java.time.ZoneId.getAvailableZoneIds().contains(zone)) errors.put("zoneinfo","validation.value");
        if (!Set.of("en","km").contains(input.getOrDefault("locale",""))) errors.put("locale","validation.value");
        String phone=optional(input.get("phoneNumber")); if (phone != null && !phone.matches("\\+[1-9][0-9]{1,14}")) errors.put("phoneNumber","validation.value");
        String country=optional(input.get("country")); if (country != null && !Set.of(Locale.getISOCountries()).contains(country.toUpperCase(Locale.ROOT))) errors.put("country","validation.value");
        if (input.getOrDefault("streetAddress","").length()>255) errors.put("streetAddress","validation.value");
        if (input.getOrDefault("postalCode","").length()>20) errors.put("postalCode","validation.value");
        return errors;
    }

}
