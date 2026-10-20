package kh.edu.istad.platform.customer.persistence.query;
import kh.edu.istad.platform.customer.domain.dto.FilterRequest;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.regex.Pattern;
import org.bson.Document;
import static org.junit.jupiter.api.Assertions.*;
import static kh.edu.istad.platform.customer.persistence.query.CustomerQueries.*;

class DynamicMongoQueryTest {
    private FilterRequest request(String... filters) {
        return new FilterRequest(List.of(filters), null, 2, 10, "createdAt", "desc");
    }
    @Test void alwaysExcludesDeletedRecordsAndCombinesFiltersWithAnd() {
        var criteria = DynamicMongoQuery.criteria(request("status:eq:ACTIVE", "username:eq:sokha"), FIELDS, SEARCH).getCriteriaObject();
        var terms = criteria.getList("$and", Document.class);
        assertEquals(new Document("status", new Document("$ne", "DELETED")), terms.getFirst());
        assertTrue(terms.contains(new Document("status", "ACTIVE")));
        assertTrue(terms.contains(new Document("username", "sokha")));
    }
    @Test void escapesRegexInputAsLiteralText() {
        var terms = DynamicMongoQuery.criteria(request("username:contains:.*"), FIELDS, SEARCH)
                .getCriteriaObject().getList("$and", Document.class);
        Pattern pattern = (Pattern) terms.get(1).get("username");
        assertTrue(pattern.matcher("text.*text").find());
        assertFalse(pattern.matcher("anything").find());
    }
    @Test void rejectsUnknownFieldsOperatorsAndInvalidTypes() {
        for (String input : List.of("$where:eq:evil", "status:regex:.*", "segmentId:eq:invalid", "status:eq:UNKNOWN", "username:gt:a"))
            assertThrows(IllegalArgumentException.class, () -> DynamicMongoQuery.criteria(request(input), FIELDS, SEARCH));
    }
    @Test void appliesBoundedPaginationAndStableSorting() {
        var query = DynamicMongoQuery.page(request(), FIELDS, SEARCH);
        assertEquals(20, query.getSkip());
        assertEquals(10, query.getLimit());
        assertEquals(new Document("createdAt", -1).append("_id", 1), query.getSortObject());
        assertThrows(IllegalArgumentException.class, () -> new FilterRequest(List.of(), null, -1, 10, "createdAt", "desc"));
        assertThrows(IllegalArgumentException.class, () -> new FilterRequest(List.of(), null, 0, 101, "createdAt", "desc"));
        assertThrows(IllegalArgumentException.class, () -> DynamicMongoQuery.page(
                new FilterRequest(List.of(), null, 0, 10, "$where", "asc"), FIELDS, SEARCH));
    }
    @Test void convertsDatesAndInLists() {
        var terms = DynamicMongoQuery.criteria(request("createdAt:gte:2026-01-01T00:00:00Z", "status:in:ACTIVE,INACTIVE"), FIELDS, SEARCH)
                .getCriteriaObject().getList("$and", Document.class);
        assertInstanceOf(java.util.Date.class, terms.get(1).get("createdAt", Document.class).get("$gte"));
        assertEquals(List.of("ACTIVE", "INACTIVE"), terms.get(2).get("status", Document.class).get("$in"));
    }
}
