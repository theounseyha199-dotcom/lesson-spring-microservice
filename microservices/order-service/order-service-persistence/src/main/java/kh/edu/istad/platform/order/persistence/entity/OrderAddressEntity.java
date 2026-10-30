package kh.edu.istad.platform.order.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("order_addresses")
@Getter
@Setter
@NoArgsConstructor
public class OrderAddressEntity {

    private UUID orderId;
    private String city;
    private String pastol_code;
    private String street;
}
