package com.lavander.estore.service;

import com.lavander.estore.dto.OrderDto;
import com.lavander.estore.dto.OrderRequest;
import com.lavander.estore.model.Cart;
import com.lavander.estore.model.DeliveryMethod;
import com.lavander.estore.model.Order;
import com.lavander.estore.model.OrderItem;
import com.lavander.estore.model.OrderStatus;
import com.lavander.estore.repository.CartRepository;
import com.lavander.estore.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
    }

    @Transactional
    public OrderDto placeOrder(String cartToken, OrderRequest request) {
        Cart cart = (cartToken != null ? cartRepository.findByOwnerToken(cartToken) : Optional.<Cart>empty())
                .orElseThrow(() -> new IllegalArgumentException("Cannot place an order without a cart"));
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot place an order with an empty cart");
        }

        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingCost = calculateShippingCost(subtotal, request.deliveryMethod());
        BigDecimal discountAmount = BigDecimal.ZERO; // Promo codes aren't validated/applied yet.
        BigDecimal total = subtotal.add(shippingCost).subtract(discountAmount);

        Order order = new Order();
        order.setCustomerFullName(request.customerFullName());
        order.setCustomerPhone(request.customerPhone());
        order.setCustomerEmail(request.customerEmail());
        order.setDeliveryMethod(request.deliveryMethod());
        order.setShippingStreet(request.shippingStreet());
        order.setShippingCity(request.shippingCity());
        order.setShippingCounty(request.shippingCounty());
        order.setShippingPostalCode(request.shippingPostalCode());
        order.setPaymentMethod(request.paymentMethod());
        order.setPromoCode(request.promoCode());
        order.setSubtotal(subtotal);
        order.setShippingCost(shippingCost);
        order.setDiscountAmount(discountAmount);
        order.setTotal(total);
        order.setStatus(OrderStatus.PLACED);

        List<OrderItem> orderItems = cart.getItems().stream()
                .map(item -> new OrderItem(
                        order,
                        item.getVariant().getId(),
                        item.getVariant().getVariantName(),
                        item.getVariant().getPrice(),
                        item.getQuantity()))
                .toList();
        order.setItems(orderItems);

        Order saved = orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return OrderDto.fromEntity(saved);
    }

    // Placeholder: free shipping regardless of subtotal or delivery method. This is the
    // one place to add real logic later (e.g. a flat courier fee, a free-shipping
    // threshold, or a different fee per DeliveryMethod).
    private BigDecimal calculateShippingCost(BigDecimal subtotal, DeliveryMethod deliveryMethod) {
        return BigDecimal.ZERO;
    }
}
