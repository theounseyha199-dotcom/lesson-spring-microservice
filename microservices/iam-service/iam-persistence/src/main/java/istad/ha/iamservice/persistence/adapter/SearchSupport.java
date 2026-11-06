package istad.ha.iamservice.persistence.adapter;

import org.springframework.data.domain.*;
import jakarta.persistence.criteria.*;
import java.util.*;

final class SearchSupport {
    private SearchSupport() { }
    static Pageable page(int page, int size, String value) {
        var parts = value.split(",");
        var sort = Sort.by(parts.length == 2 && "desc".equals(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC, parts[0]);
        return PageRequest.of(page, size, sort.and(Sort.by("id")));
    }
    static void text(List<Predicate> predicates, Root<?> root, CriteriaBuilder cb, String search, String... fields) {
        if (search == null) return;
        String pattern = "%" + search.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        predicates.add(cb.or(Arrays.stream(fields).map(field -> cb.like(cb.lower(root.get(field)), pattern, '!')).toArray(Predicate[]::new)));
    }
}
