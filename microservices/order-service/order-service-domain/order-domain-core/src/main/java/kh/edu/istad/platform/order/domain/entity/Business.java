package kh.edu.istad.platform.order.domain.entity;

import kh.edu.istad.common.domain.entity.BaseEntity;
import kh.edu.istad.common.domain.valueobject.BusinessId;

import java.util.List;
import java.util.Objects;

public class Business extends BaseEntity<BusinessId> {
    private  final List<Product> products;
    private Boolean active;

    private Business(Builder builder) {
        setId(Objects.requireNonNull(builder.id, "businessId is required"));
        products = builder.products == null ? List.of() : List.copyOf(builder.products);
        active = builder.active;
    }

    public static final class Builder {
        private BusinessId id;
        private List<Product> products;
        private Boolean active;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder id(BusinessId val) {
            id = val;
            return this;
        }

        public Builder products(List<Product> val) {
            products = val;
            return this;
        }

        public Builder active(Boolean val) {
            active = val;
            return this;
        }

        public Business build() {
            return new Business(this);
        }
    }
}
