package com.lavander.estore.controller;

import com.lavander.estore.dto.AddCartItemRequest;
import com.lavander.estore.dto.CartDto;
import com.lavander.estore.dto.UpdateCartItemRequest;
import com.lavander.estore.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
            Authentication authentication,
            @RequestHeader(value = "X-Cart-Token", required = false) String cartToken) {
        return ResponseEntity.ok(cartService.getCart(resolveUserEmail(authentication), cartToken));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(
            Authentication authentication,
            @RequestHeader(value = "X-Cart-Token", required = false) String cartToken,
            @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(resolveUserEmail(authentication), cartToken, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateItemQuantity(
            Authentication authentication,
            @RequestHeader(value = "X-Cart-Token", required = false) String cartToken,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(resolveUserEmail(authentication), cartToken, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(
            Authentication authentication,
            @RequestHeader(value = "X-Cart-Token", required = false) String cartToken,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(resolveUserEmail(authentication), cartToken, itemId));
    }

    // Cart endpoints are public, so `authentication` is an AnonymousAuthenticationToken for
    // a visitor with no JWT — only a real Bearer token produces the UsernamePasswordAuthenticationToken
    // JwtAuthenticationFilter sets, so that's what distinguishes "actually logged in" here.
    private String resolveUserEmail(Authentication authentication) {
        if (authentication instanceof UsernamePasswordAuthenticationToken && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
}
