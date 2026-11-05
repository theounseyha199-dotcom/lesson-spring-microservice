package kh.edu.istad.platform.order.domain.entity;

import kh.edu.istad.common.domain.entity.BaseEntity;
import kh.edu.istad.common.domain.valueobject.Money;
import kh.edu.istad.common.domain.valueobject.ProductId;

import java.util.Objects;

public class Product extends BaseEntity<ProductId> {
    private String name;
    private Money price;

    private Product(Builder builder) {
        this(builder.id, builder.name, builder.price);
    }

    public Product(ProductId id, String name, Money price) {
        setId(Objects.requireNonNull(id, "productId is required"));
        this.name = name;
        this.price = Objects.requireNonNull(price, "price is required");
    }

    public void updateConfirmedNameAndPrice (String name, Money price){

        this.name = name;
        this.price = price;

    }

    public String getName() {
        return name;
    }

    public Money getPrice() {
        return price;
    }

    public Money price() {
        return price;
    }


    public static final class Builder {
        private ProductId id;
        private String name;
        private Money price;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder id(ProductId val) {
            id = val;
            return this;
        }

        public Builder name(String val) {
            name = val;
            return this;
        }

        public Builder price(Money val) {
            price = val;
            return this;
        }

        public Product build() {
            return new Product(this);
        }
    }
}
