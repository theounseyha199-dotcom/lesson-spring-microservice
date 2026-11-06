package istad.ha.iamservice.application.query;

public final class PageValidation {
    private PageValidation() { }
    public static String search(String value, int max) {
        return value == null || value.isBlank() ? null : istad.ha.iamservice.domain.rule.Checks.text(value, "filter", max);
    }
    public static String sort(int page, int size, String sort, java.util.Set<String> allowed) {
        istad.ha.iamservice.domain.rule.Checks.require(page >= 0 && size >= 1 && size <= 100,
                "validation", "Page must be nonnegative and size must be between 1 and 100");
        sort = sort == null || sort.isBlank() ? "createdAt,desc" : sort.strip();
        var parts = sort.split(",", -1);
        istad.ha.iamservice.domain.rule.Checks.require(parts.length <= 2 && allowed.contains(parts[0])
                && (parts.length == 1 || java.util.Set.of("asc", "desc").contains(parts[1])), "validation", "Unsupported sort");
        return sort;
    }
}
