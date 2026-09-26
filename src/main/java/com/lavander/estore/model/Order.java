package com.lavander.estore.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// Table explicitly named "orders" — "order" is a reserved SQL keyword (ORDER BY).
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Set only when the customer was logged in at checkout — anonymous checkout stays supported. */
    private Long userId;

    private String customerFullName;
    private String customerPhone;
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod;

    private String shippingStreet;
    private String shippingCity;
    private String shippingCounty;
    private String shippingPostalCode;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    /** Not validated or applied to the total yet — captured for when promo codes are wired up. */
    private String promoCode;

    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal discountAmount;
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private Instant createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
