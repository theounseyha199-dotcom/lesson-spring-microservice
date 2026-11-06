package istad.ha.iamservice.application.command;

import java.time.*;
import istad.ha.iamservice.domain.model.*;

public record ProfilePatch(FieldUpdate<String> email, FieldUpdate<String> givenName, FieldUpdate<String> familyName, FieldUpdate<String> middleName, FieldUpdate<String> nickname, FieldUpdate<String> gender, FieldUpdate<LocalDate> birthdate, FieldUpdate<String> profileUrl, FieldUpdate<String> pictureUrl, FieldUpdate<String> websiteUrl, FieldUpdate<String> zoneinfo, FieldUpdate<String> locale, FieldUpdate<String> phoneNumber, FieldUpdate<Address> address) {
    public ProfilePatch {
        email = FieldUpdate.normalize(email);
        givenName = FieldUpdate.normalize(givenName);
        familyName = FieldUpdate.normalize(familyName);
        middleName = FieldUpdate.normalize(middleName);
        nickname = FieldUpdate.normalize(nickname);
        gender = FieldUpdate.normalize(gender);
        birthdate = FieldUpdate.normalize(birthdate);
        profileUrl = FieldUpdate.normalize(profileUrl);
        pictureUrl = FieldUpdate.normalize(pictureUrl);
        websiteUrl = FieldUpdate.normalize(websiteUrl);
        zoneinfo = FieldUpdate.normalize(zoneinfo);
        locale = FieldUpdate.normalize(locale);
        phoneNumber = FieldUpdate.normalize(phoneNumber);
        address = FieldUpdate.normalize(address);
    }
    public static Builder builder() { return new Builder(); }
    public User apply(User user, String actor, Instant now) {
        var previous = user.profile();
        String phone = phoneNumber.orElse(previous.phoneNumber());
        boolean verified = java.util.Objects.equals(phone, previous.phoneNumber()) && previous.phoneNumberVerified();
        var changed = new UserProfile(givenName.orElse(previous.givenName()), familyName.orElse(previous.familyName()), middleName.orElse(previous.middleName()), nickname.orElse(previous.nickname()), gender.orElse(previous.gender()), birthdate.orElse(previous.birthdate()), profileUrl.orElse(previous.profileUrl()), pictureUrl.orElse(previous.pictureUrl()), websiteUrl.orElse(previous.websiteUrl()), zoneinfo.orElse(previous.zoneinfo()), locale.orElse(previous.locale()), phone, verified, address.orElse(previous.address()));
        var result = user.updateProfile(changed, actor, now);
        return email.supplied() ? result.changeEmail(email.value(), actor, now) : result;
    }
    public static final class Builder {
        private FieldUpdate<String> email = FieldUpdate.unchanged();
        public Builder email(String value) { email = FieldUpdate.set(value); return this; }
        private FieldUpdate<String> givenName = FieldUpdate.unchanged();
        public Builder givenName(String value) { givenName = FieldUpdate.set(value); return this; }
        private FieldUpdate<String> familyName = FieldUpdate.unchanged();
        public Builder familyName(String value) { familyName = FieldUpdate.set(value); return this; }
        private FieldUpdate<String> middleName = FieldUpdate.unchanged();
        public Builder middleName(String value) { middleName = FieldUpdate.set(value); return this; }
        private FieldUpdate<String> nickname = FieldUpdate.unchanged();
        public Builder nickname(String value) { nickname = FieldUpdate.set(value); return this; }
        private FieldUpdate<String> gender = FieldUpdate.unchanged();
        public Builder gender(String value) { gender = FieldUpdate.set(value); return this; }
        private FieldUpdate<LocalDate> birthdate = FieldUpdate.unchanged();
        public Builder birthdate(LocalDate value) { birthdate = FieldUpdate.set(value); return this; }
        private FieldUpdate<String> profileUrl = FieldUpdate.unchanged();
        public Builder profileUrl(String value) { profileUrl = FieldUpdate.set(value); return this; }
        private FieldUpdate<String> pictureUrl = FieldUpdate.unchanged();
        public Builder pictureUrl(String value) { pictureUrl = FieldUpdate.set(value); return this; }
        private FieldUpdate<String> websiteUrl = FieldUpdate.unchanged();
        public Builder websiteUrl(String value) { websiteUrl = FieldUpdate.set(value); return this; }
        private FieldUpdate<String> zoneinfo = FieldUpdate.unchanged();
        public Builder zoneinfo(String value) { zoneinfo = FieldUpdate.set(value); return this; }
        private FieldUpdate<String> locale = FieldUpdate.unchanged();
        public Builder locale(String value) { locale = FieldUpdate.set(value); return this; }
        private FieldUpdate<String> phoneNumber = FieldUpdate.unchanged();
        public Builder phoneNumber(String value) { phoneNumber = FieldUpdate.set(value); return this; }
        private FieldUpdate<Address> address = FieldUpdate.unchanged();
        public Builder address(Address value) { address = FieldUpdate.set(value); return this; }
        public ProfilePatch build() { return new ProfilePatch(email, givenName, familyName, middleName, nickname, gender, birthdate, profileUrl, pictureUrl, websiteUrl, zoneinfo, locale, phoneNumber, address); }
    }
}
