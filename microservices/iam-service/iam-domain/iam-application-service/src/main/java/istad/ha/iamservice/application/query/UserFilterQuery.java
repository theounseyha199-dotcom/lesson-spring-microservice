package istad.ha.iamservice.application.query;

public record UserFilterQuery(int page, int size, String sort, String search, Boolean enabled, Boolean locked) {
    public UserFilterQuery { search = PageValidation.search(search, 255); sort = PageValidation.sort(page, size, sort, java.util.Set.of("uuid", "username", "email", "givenName", "familyName", "createdAt", "updatedAt")); }
}
