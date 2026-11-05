package kh.edu.istad.platform.order.persistence;

import kh.edu.istad.platform.order.persistence.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.R2dbcSimpleTypeHolder;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SqlEntityMappingTest {
    private final RelationalMappingContext context = new RelationalMappingContext();
    private final MappingR2dbcConverter converter = new MappingR2dbcConverter(context);

    SqlEntityMappingTest() {
        context.setSimpleTypeHolder(R2dbcSimpleTypeHolder.HOLDER);
    }

    private Object value(OutboundRow row, String column) {
        return row.entrySet().stream()
                .filter(entry -> entry.getKey().getReference().equals(column))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing column " + column + " in " + row.keySet()))
                .getValue().getValue();
    }

    @Test
    void writesBusinessCompositeKeyAndBooleanUsingExistingSchemaColumns() {
        UUID businessId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        BusinessEntity business = new BusinessEntity();
        business.setBusinessEntityId(new BusinessEntityId(businessId, productId));
        business.setBusinessActive(true);

        OutboundRow row = new OutboundRow();
        converter.write(business, row);

        assertEquals(businessId, value(row, "business_id"));
        assertEquals(productId, value(row, "product_id"));
        assertEquals(true, value(row, "business_active"));
    }

    @Test
    void writesCustomerAddressAndFailureMessagesUsingExistingSchemaColumns() {
        CustomerEntity customer = new CustomerEntity();
        customer.setUsername("customer");
        OutboundRow customerRow = new OutboundRow();
        converter.write(customer, customerRow);
        assertEquals("customers", context.getRequiredPersistentEntity(CustomerEntity.class)
                .getTableName().getReference());
        assertEquals("customer", value(customerRow, "username"));

        UUID addressId = UUID.randomUUID();
        OrderAddressEntity address = new OrderAddressEntity();
        address.setId(addressId);
        address.setPostalCode("12000");
        OutboundRow addressRow = new OutboundRow();
        converter.write(address, addressRow);
        assertEquals(addressId, value(addressRow, "id"));
        assertEquals("12000", value(addressRow, "postal_code"));

        OrderEntity order = new OrderEntity();
        order.setFailureMessages("Declined");
        OutboundRow orderRow = new OutboundRow();
        converter.write(order, orderRow);
        assertEquals("Declined", value(orderRow, "failure_messages"));
    }
}
