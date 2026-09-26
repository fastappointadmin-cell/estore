package com.lavander.estore.dto;

import com.lavander.estore.model.DeliveryMethod;
import com.lavander.estore.model.Order;
import com.lavander.estore.model.OrderStatus;
import com.lavander.estore.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDto(
        Long id,
        String customerFullName,
        String customerPhone,
        String customerEmail,
        DeliveryMethod deliveryMethod,
        String shippingStreet,
        String shippingCity,
        String shippingCounty,
        String shippingPostalCode,
        PaymentMethod paymentMethod,
        String promoCode,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal discountAmount,
        BigDecimal total,
        OrderStatus status,
        Instant createdAt,
        List<OrderItemDto> items) {

    public static OrderDto fromEntity(Order entity) {
        List<OrderItemDto> items = entity.getItems().stream().map(OrderItemDto::fromEntity).toList();
        return new OrderDto(
                entity.getId(),
                entity.getCustomerFullName(),
                entity.getCustomerPhone(),
                entity.getCustomerEmail(),
                entity.getDeliveryMethod(),
                entity.getShippingStreet(),
                entity.getShippingCity(),
                entity.getShippingCounty(),
                entity.getShippingPostalCode(),
                entity.getPaymentMethod(),
                entity.getPromoCode(),
                entity.getSubtotal(),
                entity.getShippingCost(),
                entity.getDiscountAmount(),
                entity.getTotal(),
                entity.getStatus(),
                entity.getCreatedAt(),
                items);
    }
}
