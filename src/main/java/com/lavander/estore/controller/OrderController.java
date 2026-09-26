package com.lavander.estore.controller;

import com.lavander.estore.dto.OrderDto;
import com.lavander.estore.dto.OrderRequest;
import com.lavander.estore.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDto> placeOrder(
            @RequestHeader(value = "X-Cart-Token", required = false) String cartToken,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(cartToken, request));
    }
}
