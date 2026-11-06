package istad.ha.iamservice.restapi.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import istad.ha.iamservice.application.command.*;

public final class ProfileRequest implements Requests.Strict {
    private final Set<String> supplied = new HashSet<>();
    @Email @Size(max=255) private String email;
    @JsonSetter("email") public void setEmail(String value) { supplied.add("email"); email = value; }
    @Size(max=100) private String givenName;
    @JsonSetter("givenName") public void setGivenName(String value) { supplied.add("givenName"); givenName = value; }
    @Size(max=100) private String familyName;
    @JsonSetter("familyName") public void setFamilyName(String value) { supplied.add("familyName"); familyName = value; }
    @Size(max=100) private String middleName;
    @JsonSetter("middleName") public void setMiddleName(String value) { supplied.add("middleName"); middleName = value; }
    @Size(max=100) private String nickname;
    @JsonSetter("nickname") public void setNickname(String value) { supplied.add("nickname"); nickname = value; }
    @Pattern(regexp="male|female|other") private String gender;
    @JsonSetter("gender") public void setGender(String value) { supplied.add("gender"); gender = value; }
     private LocalDate birthdate;
    @JsonSetter("birthdate") public void setBirthdate(LocalDate value) { supplied.add("birthdate"); birthdate = value; }
    @Size(max=500) private String profileUrl;
    @JsonSetter("profileUrl") public void setProfileUrl(String value) { supplied.add("profileUrl"); profileUrl = value; }
    @Size(max=500) private String pictureUrl;
    @JsonSetter("pictureUrl") public void setPictureUrl(String value) { supplied.add("pictureUrl"); pictureUrl = value; }
    @Size(max=500) private String websiteUrl;
    @JsonSetter("websiteUrl") public void setWebsiteUrl(String value) { supplied.add("websiteUrl"); websiteUrl = value; }
    @Size(max=50) private String zoneinfo;
    @JsonSetter("zoneinfo") public void setZoneinfo(String value) { supplied.add("zoneinfo"); zoneinfo = value; }
    @Pattern(regexp="en|km") private String locale;
    @JsonSetter("locale") public void setLocale(String value) { supplied.add("locale"); locale = value; }
    @Pattern(regexp="\\+[1-9][0-9]{1,14}") private String phoneNumber;
    @JsonSetter("phoneNumber") public void setPhoneNumber(String value) { supplied.add("phoneNumber"); phoneNumber = value; }
    @Valid private Requests.AddressInput address;
    @JsonSetter("address") public void setAddress(Requests.AddressInput value) { supplied.add("address"); address = value; }
    public UpdateUserProfileCommand command(UUID uuid) {
        var patch = ProfilePatch.builder();
        if (supplied.contains("email")) patch.email(email);
        if (supplied.contains("givenName")) patch.givenName(givenName);
        if (supplied.contains("familyName")) patch.familyName(familyName);
        if (supplied.contains("middleName")) patch.middleName(middleName);
        if (supplied.contains("nickname")) patch.nickname(nickname);
        if (supplied.contains("gender")) patch.gender(gender);
        if (supplied.contains("birthdate")) patch.birthdate(birthdate);
        if (supplied.contains("profileUrl")) patch.profileUrl(profileUrl);
        if (supplied.contains("pictureUrl")) patch.pictureUrl(pictureUrl);
        if (supplied.contains("websiteUrl")) patch.websiteUrl(websiteUrl);
        if (supplied.contains("zoneinfo")) patch.zoneinfo(zoneinfo);
        if (supplied.contains("locale")) patch.locale(locale);
        if (supplied.contains("phoneNumber")) patch.phoneNumber(phoneNumber);
        if (supplied.contains("address")) patch.address(address == null ? null : address.value());
        return new UpdateUserProfileCommand(uuid, patch.build());
    }
}
