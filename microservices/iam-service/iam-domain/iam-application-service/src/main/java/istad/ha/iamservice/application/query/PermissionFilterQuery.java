package istad.ha.iamservice.application.query;

public record PermissionFilterQuery(int page, int size, String sort, String search, String resource, String action, Boolean active) {
    public PermissionFilterQuery { search = PageValidation.search(search, 255); resource = PageValidation.search(resource, 50); action = PageValidation.search(action, 50); sort = PageValidation.sort(page, size, sort, java.util.Set.of("uuid", "name", "resource", "action", "createdAt", "updatedAt")); }
}
