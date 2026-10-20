package kh.edu.istad.platform.customer.domain.dto;
import java.util.List;

// Shared query contract: adapters own field allowlists; callers cannot supply MongoDB expressions.
public record FilterRequest(List<String> filters, String search, int page, int size, String sort, String direction) {
    public FilterRequest {
        filters = filters == null ? List.of() : List.copyOf(filters);
        if (filters.size() > 20) throw new IllegalArgumentException("At most 20 filters are allowed");
        if (page < 0 || page > 100000 || size < 1 || size > 100)
            throw new IllegalArgumentException("page must be 0..100000 and size must be 1..100");
        if (search != null && search.length() > 100) throw new IllegalArgumentException("Search is too long");
        if (sort == null || sort.isBlank()) sort = "createdAt";
        if (direction == null) direction = "desc";
        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc"))
            throw new IllegalArgumentException("direction must be asc or desc");
    }
}
