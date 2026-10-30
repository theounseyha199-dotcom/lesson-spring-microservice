package kh.edu.istad.platform.order.persistence.entity;


import com.mongodb.lang.Nullable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Table("businesses")
@Getter
@Setter
@NoArgsConstructor
public class BusinessEntity {

    @Id
    @Embedded.Nullable
    private BusinessEntityId businessEntityId;

    private String businessActive;
    private String productName;
    @Column("product_price")
    private BigDecimal productPrice;

}
