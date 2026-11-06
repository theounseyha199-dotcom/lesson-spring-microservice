package istad.ha.iamservice.domain.rule;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.math.BigInteger;

/** Defensive deep copy of JSON data without any serialization-framework dependency. */
public final class JsonAttributes {
    private JsonAttributes() { }
    public static Map<String, Object> copy(Map<String, Object> attributes) {
        if (attributes == null) return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        attributes.forEach((key, value) -> {
            Checks.required(key, "attribute key"); result.put(key, copyValue(value));
        });
        return Collections.unmodifiableMap(result);
    }
    private static Object copyValue(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Byte
                || value instanceof Short || value instanceof Integer || value instanceof Long
                || value instanceof BigDecimal || value instanceof BigInteger) return value;
        if (value instanceof Double number && Double.isFinite(number)) return value;
        if (value instanceof Float number && Float.isFinite(number)) return value;
        if (value instanceof List<?> list) return Collections.unmodifiableList(list.stream().map(JsonAttributes::copyValue).toList());
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                Checks.require(key instanceof String, "validation", "JSON object keys must be strings");
                result.put((String) key, copyValue(item));
            });
            return Collections.unmodifiableMap(result);
        }
        throw new istad.ha.iamservice.domain.exception.DomainException("validation", "Provider attributes must be JSON values");
    }
}
