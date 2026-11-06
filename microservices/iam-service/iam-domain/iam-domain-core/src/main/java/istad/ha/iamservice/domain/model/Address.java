package istad.ha.iamservice.domain.model;

import java.util.Locale;
import java.util.Set;
import istad.ha.iamservice.domain.rule.Checks;

public record Address(String streetAddress, String locality, String region, String postalCode, String country) {
    private static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());
    public Address {
        streetAddress = Checks.optional(streetAddress, "streetAddress", 255);
        locality = Checks.optional(locality, "locality", 100);
        region = Checks.optional(region, "region", 100);
        postalCode = Checks.optional(postalCode, "postalCode", 20);
        if (country != null) {
            country = Checks.text(country, "country", 2).toUpperCase(Locale.ROOT);
            Checks.require(COUNTRIES.contains(country), "validation", "Invalid ISO country code");
        }
    }
}
