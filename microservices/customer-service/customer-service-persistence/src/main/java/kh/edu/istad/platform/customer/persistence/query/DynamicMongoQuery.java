package kh.edu.istad.platform.customer.persistence.query;
import kh.edu.istad.platform.customer.domain.dto.FilterRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

// Reusable allowlisted filtering: never interpret raw client JSON, field paths, or regular expressions.
public final class DynamicMongoQuery {
    private DynamicMongoQuery() {}
    public record Field(String storedName, Function<String, Object> parse, boolean text) {}
    public static Field text(String name) { return new Field(name, value -> value, true); }
    public static Field uuid(String name) {
        return new Field(name, value -> UUID.fromString(value).toString(), false);
    }
    public static <E extends Enum<E>> Field enumeration(String name, Class<E> type) {
        return new Field(name, value -> Enum.valueOf(type, value.toUpperCase(Locale.ROOT)).name(), false);
    }
    public static Field instant(String name) {
        return new Field(name, value -> java.util.Date.from(java.time.Instant.parse(value)), false);
    }
    public static Criteria criteria(FilterRequest request, Map<String, Field> allowed, List<String> searchFields) {
        List<Criteria> terms = new ArrayList<>();
        // This mandatory condition cannot be overridden by a user-supplied status filter.
        terms.add(Criteria.where("status").ne("DELETED"));
        for (String filter : request.filters()) {
            if (filter.length() > 300) throw new IllegalArgumentException("Filter is too long");
            String[] parts = filter.split(":", 3);
            if (parts.length != 3 || !allowed.containsKey(parts[0]))
                throw new IllegalArgumentException("Filter must be allowedField:operator:value");
            Field field = allowed.get(parts[0]);
            Criteria criterion = Criteria.where(field.storedName());
            String value = parts[2];
            try {
                terms.add(switch (parts[1]) {
                    case "eq" -> criterion.is(value.equals("null") ? null : field.parse().apply(value));
                    case "ne" -> criterion.ne(value.equals("null") ? null : field.parse().apply(value));
                    case "in" -> criterion.in(Arrays.stream(value.split(",", -1)).map(field.parse()).toList());
                    case "contains" -> {
                        if (!field.text()) throw new IllegalArgumentException("contains requires a text field");
                        yield criterion.regex(Pattern.compile(Pattern.quote(value), Pattern.CASE_INSENSITIVE));
                    }
                    case "gt", "gte", "lt", "lte" -> {
                        if (!field.storedName().equals("createdAt") && !field.storedName().equals("updatedAt"))
                            throw new IllegalArgumentException("Range comparisons require a date field");
                        Object parsed = field.parse().apply(value);
                        yield switch(parts[1]) {
                            case "gt" -> criterion.gt(parsed);
                            case "gte" -> criterion.gte(parsed);
                            case "lt" -> criterion.lt(parsed);
                            default -> criterion.lte(parsed);
                        };
                    }
                    default -> throw new IllegalArgumentException("Unsupported filter operator");
                });
            } catch (RuntimeException error) {
                throw new IllegalArgumentException("Invalid filter: " + parts[0] + ":" + parts[1], error);
            }
        }
        if (request.search() != null && !request.search().isBlank() && !searchFields.isEmpty()) {
            Pattern literal = Pattern.compile(Pattern.quote(request.search().trim()), Pattern.CASE_INSENSITIVE);
            terms.add(new Criteria().orOperator(searchFields.stream()
                    .map(field -> Criteria.where(field).regex(literal)).toArray(Criteria[]::new)));
        }
        return new Criteria().andOperator(terms.toArray(Criteria[]::new));
    }
    public static Query page(FilterRequest request, Map<String, Field> fields, List<String> searchFields) {
        if (!fields.containsKey(request.sort())) throw new IllegalArgumentException("Unsupported sort field");
        Sort sort = Sort.by(Sort.Direction.fromString(request.direction()), fields.get(request.sort()).storedName());
        if (!fields.get(request.sort()).storedName().equals("_id")) sort = sort.and(Sort.by("_id"));
        return Query.query(criteria(request, fields, searchFields)).with(sort)
                .skip((long) request.page() * request.size()).limit(request.size());
    }
}
