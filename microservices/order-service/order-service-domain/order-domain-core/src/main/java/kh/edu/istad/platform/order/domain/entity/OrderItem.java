package kh.edu.istad.platform.order.domain.entity;

import kh.edu.istad.common.domain.entity.BaseEntity;
import kh.edu.istad.common.domain.valueobject.Money;
import kh.edu.istad.common.domain.valueobject.OrderId;
import kh.edu.istad.common.domain.valueobject.ProductId;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class OrderItem extends BaseEntity<UUID> {
    private OrderId orderId;
    private final Product product;
    private final int quantity;
    private final Money price;
    private final Money subTotal;

    private OrderItem(Builder builder) {
        setId(builder.id == null ? UUID.randomUUID() : builder.id);
        orderId = builder.orderId;
        product = Objects.requireNonNull(builder.product, "product is required");
        if (builder.quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        quantity = builder.quantity;
        price = Objects.requireNonNull(builder.price, "price is required");
        if (price.amount() == null || price.amount().signum() <= 0) {
            throw new IllegalArgumentException("Unit price must be positive");
        }
        Money calculatedSubTotal = price.multiply(quantity);
        if (builder.subTotal != null && builder.subTotal.amount().compareTo(calculatedSubTotal.amount()) != 0) {
            throw new IllegalArgumentException("Subtotal must equal price times quantity");
        }
        subTotal = calculatedSubTotal;
    }

    public OrderId getOrderId() {
        return orderId;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getPrice() {
        return price;
    }

    public Money getSubTotal() {
        return subTotal;
    }

    public OrderItem(UUID productId, int quantity, BigDecimal unitPrice) {
        this(productId, quantity, unitPrice, null, null);
    }

    public OrderItem(UUID productId, int quantity, BigDecimal unitPrice, Money price, Money subTotal) {
        Objects.requireNonNull(productId, "productId is required");
        Objects.requireNonNull(unitPrice, "unitPrice is required");
        if (quantity <= 0 || unitPrice.signum() <= 0) {
            throw new IllegalArgumentException("Quantity and unit price must be positive");
        }
        setId(UUID.randomUUID());
        this.product = new Product(new ProductId(productId), null, new Money(unitPrice));
        this.quantity = quantity;
        this.price = price == null ? new Money(unitPrice) : price;
        if (this.price.amount() == null || this.price.amount().compareTo(unitPrice) != 0) {
            throw new IllegalArgumentException("Price must match unit price");
        }
        Money calculatedSubTotal = this.price.multiply(quantity);
        if (subTotal != null && subTotal.amount().compareTo(calculatedSubTotal.amount()) != 0) {
            throw new IllegalArgumentException("Subtotal must equal price times quantity");
        }
        this.subTotal = calculatedSubTotal;
    }

    public UUID productId() { return product.getId().id(); }
    public int quantity() { return quantity; }
    public BigDecimal unitPrice() { return price.amount(); }
    public BigDecimal subtotal() { return subTotal.amount(); }


    public static final class Builder {
        private UUID id;
        private OrderId orderId;
        private Product product;
        private int quantity;
        private Money price;
        private Money subTotal;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder id(UUID val) {
            id = val;
            return this;
        }

        public Builder orderId(OrderId val) {
            orderId = val;
            return this;
        }

        public Builder product(Product val) {
            product = val;
            return this;
        }

        public Builder quantity(int val) {
            quantity = val;
            return this;
        }

        public Builder price(Money val) {
            price = val;
            return this;
        }

        public Builder subTotal(Money val) {
            subTotal = val;
            return this;
        }

        public OrderItem build() {
            return new OrderItem(this);
        }
    }
}
