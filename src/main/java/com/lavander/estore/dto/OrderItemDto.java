package com.lavander.estore.dto;

import com.lavander.estore.model.OrderItem;

import java.math.BigDecimal;

public record OrderItemDto(Long id, Long variantId, String variantName, BigDecimal unitPrice, Integer quantity) {
    public static OrderItemDto fromEntity(OrderItem entity) {
        return new OrderItemDto(entity.getId(), entity.getVariantId(), entity.getVariantName(), entity.getUnitPrice(), entity.getQuantity());
    }
}
