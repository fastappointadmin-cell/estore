package com.lavander.estore.controller;

import com.lavander.estore.dto.AddCartItemRequest;
import com.lavander.estore.dto.CartDto;
import com.lavander.estore.dto.UpdateCartItemRequest;
import com.lavander.estore.service.CartService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart(
            @CookieValue(value = "cart_token", required = false) String cartToken,
            HttpServletResponse response) {
        return ResponseEntity.ok(cartService.getCart(cartToken, response));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(
            @CookieValue(value = "cart_token", required = false) String cartToken,
            HttpServletResponse response,
            @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(cartToken, response, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateItemQuantity(
            @CookieValue(value = "cart_token", required = false) String cartToken,
            HttpServletResponse response,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(cartToken, response, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(
            @CookieValue(value = "cart_token", required = false) String cartToken,
            HttpServletResponse response,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(cartToken, response, itemId));
    }
}
