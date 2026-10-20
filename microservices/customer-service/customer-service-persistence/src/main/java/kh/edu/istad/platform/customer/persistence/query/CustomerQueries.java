package kh.edu.istad.platform.customer.persistence.query;
import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;
import java.util.*;
import static kh.edu.istad.platform.customer.persistence.query.DynamicMongoQuery.*;
public final class CustomerQueries {
    private CustomerQueries() {}
    public static final Map<String, Field> FIELDS = Map.ofEntries(
            Map.entry("customerId", uuid("_id")), Map.entry("username", text("username")),
            Map.entry("familyName", text("familyName")), Map.entry("givenName", text("givenName")),
            Map.entry("email", text("email")), Map.entry("phoneNumber", text("phoneNumber")),
            Map.entry("status", enumeration("status", CustomerStatus.class)), Map.entry("segmentId", uuid("segmentId")),
            Map.entry("createdAt", instant("createdAt")), Map.entry("updatedAt", instant("updatedAt")));
    public static final List<String> SEARCH = List.of("username", "familyName", "givenName", "email", "phoneNumber");
}
