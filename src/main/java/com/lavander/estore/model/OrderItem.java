package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Snapshots the variant's name and price at order time, rather than referencing
// ProductVariant directly, so a later price change or deletion doesn't alter history.
@Entity
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private Long variantId;
    private String variantName;
    private BigDecimal unitPrice;
    private Integer quantity;

    public OrderItem(Order order, Long variantId, String variantName, BigDecimal unitPrice, Integer quantity) {
        this.order = order;
        this.variantId = variantId;
        this.variantName = variantName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }
}
