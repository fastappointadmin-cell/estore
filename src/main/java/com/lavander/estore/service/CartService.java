package com.lavander.estore.service;

import com.lavander.estore.dto.AddCartItemRequest;
import com.lavander.estore.dto.CartDto;
import com.lavander.estore.dto.UpdateCartItemRequest;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.Cart;
import com.lavander.estore.model.CartItem;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.repository.CartRepository;
import com.lavander.estore.repository.ProductVariantRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {

    private static final String CART_COOKIE_NAME = "cart_token";
    private static final int CART_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365;

    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartService(CartRepository cartRepository, ProductVariantRepository productVariantRepository) {
        this.cartRepository = cartRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public Cart resolveCart(String cartToken, HttpServletResponse response) {
        if (cartToken != null) {
            Optional<Cart> existing = cartRepository.findByOwnerToken(cartToken);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        String newToken = UUID.randomUUID().toString();
        Cart cart = cartRepository.save(new Cart(newToken));
        setCartCookie(response, newToken);
        return cart;
    }

    public CartDto getCart(String cartToken, HttpServletResponse response) {
        return CartDto.fromEntity(resolveCart(cartToken, response));
    }

    public CartDto addItem(String cartToken, HttpServletResponse response, AddCartItemRequest request) {
        Cart cart = resolveCart(cartToken, response);
        ProductVariant variant = productVariantRepository.findById(request.variantId())
                .orElseThrow(() -> new NotFoundException("Product variant not found with id: " + request.variantId()));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getVariant().getId().equals(variant.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.quantity());
        } else {
            cart.getItems().add(new CartItem(cart, variant, request.quantity()));
        }
        return CartDto.fromEntity(cartRepository.save(cart));
    }

    public CartDto updateItemQuantity(String cartToken, HttpServletResponse response, Long itemId, UpdateCartItemRequest request) {
        Cart cart = resolveCart(cartToken, response);
        findItemInCart(cart, itemId).setQuantity(request.quantity());
        return CartDto.fromEntity(cartRepository.save(cart));
    }

    public CartDto removeItem(String cartToken, HttpServletResponse response, Long itemId) {
        Cart cart = resolveCart(cartToken, response);
        cart.getItems().remove(findItemInCart(cart, itemId));
        return CartDto.fromEntity(cartRepository.save(cart));
    }

    private CartItem findItemInCart(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Cart item not found with id: " + itemId));
    }

    private void setCartCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(CART_COOKIE_NAME, token)
                .httpOnly(true)
                .path("/")
                .maxAge(CART_COOKIE_MAX_AGE_SECONDS)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
