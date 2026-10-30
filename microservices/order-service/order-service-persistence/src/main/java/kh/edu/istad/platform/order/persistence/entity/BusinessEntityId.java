package kh.edu.istad.platform.order.persistence.entity;

import java.util.UUID;


public record BusinessEntityId(
        UUID businesId,
        UUID productId
) {
}
