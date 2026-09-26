package com.lavander.estore.controller;

import com.lavander.estore.dto.OrderDto;
import com.lavander.estore.dto.OrderRequest;
import com.lavander.estore.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDto> placeOrder(
            Authentication authentication,
            @RequestHeader(value = "X-Cart-Token", required = false) String cartToken,
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(resolveUserEmail(authentication), cartToken, request));
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getMyOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getMyOrders(resolveUserEmail(authentication)));
    }

    // Placing an order is public (anonymous checkout stays supported), so `authentication`
    // is an AnonymousAuthenticationToken for a visitor with no JWT — only a real Bearer
    // token produces the UsernamePasswordAuthenticationToken JwtAuthenticationFilter sets.
    private String resolveUserEmail(Authentication authentication) {
        if (authentication instanceof UsernamePasswordAuthenticationToken && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
}
