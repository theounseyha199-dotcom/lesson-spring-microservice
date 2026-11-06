package istad.ha.iamservice.application.query;

public record RoleFilterQuery(int page, int size, String sort, String search, Boolean active) {
    public RoleFilterQuery { search = PageValidation.search(search, 255); sort = PageValidation.sort(page, size, sort, java.util.Set.of("uuid", "name", "createdAt", "updatedAt")); }
}
