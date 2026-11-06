package istad.ha.iamservice.domain.model;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import istad.ha.iamservice.domain.rule.Checks;

public record UserProfile(String givenName, String familyName, String middleName, String nickname,
        String gender, LocalDate birthdate, String profileUrl, String pictureUrl, String websiteUrl,
        String zoneinfo, String locale, String phoneNumber, boolean phoneNumberVerified, Address address) {
    public UserProfile {
        givenName = Checks.text(givenName, "givenName", 100);
        familyName = Checks.text(familyName, "familyName", 100);
        middleName = Checks.optional(middleName, "middleName", 100);
        nickname = Checks.optional(nickname, "nickname", 100);
        if (gender != null) {
            gender = Checks.text(gender, "gender", 20).toLowerCase(Locale.ROOT);
            Checks.require(Set.of("female", "male", "other").contains(gender), "validation", "Invalid gender");
        }
        profileUrl = Checks.url(profileUrl, "profileUrl"); pictureUrl = Checks.url(pictureUrl, "pictureUrl");
        websiteUrl = Checks.url(websiteUrl, "websiteUrl");
        if (zoneinfo != null) {
            zoneinfo = Checks.text(zoneinfo, "zoneinfo", 50);
            Checks.require(ZoneId.getAvailableZoneIds().contains(zoneinfo), "validation", "Invalid IANA time zone");
        }
        locale = Checks.text(locale, "locale", 10);
        Checks.require(Set.of("en", "km").contains(locale), "validation", "Supported locales are en and km");
        if (phoneNumber != null) {
            phoneNumber = Checks.text(phoneNumber, "phoneNumber", 20);
            Checks.require(phoneNumber.matches("\\+[1-9][0-9]{1,14}"), "validation", "Phone number must use E.164 format");
        }
        Checks.require(phoneNumber != null || !phoneNumberVerified, "validation", "Missing phone cannot be verified");
    }
    public static UserProfile basic(String givenName, String familyName, String locale) {
        return new UserProfile(givenName, familyName, null, null, null, null, null, null, null,
                null, locale, null, false, null);
    }
    public String name() { return givenName + " " + familyName; }
}
