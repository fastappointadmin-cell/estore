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
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartService(CartRepository cartRepository, ProductVariantRepository productVariantRepository) {
        this.cartRepository = cartRepository;
        this.productVariantRepository = productVariantRepository;
    }

    /**
     * Frontend and backend live on different origins (different Railway subdomains), so a
     * cart-identifying cookie would be a third-party cookie and gets silently dropped by
     * modern browsers. The cart token is instead handed to the client in the response body
     * and echoed back as a request header on later calls.
     */
    public Cart resolveCart(String cartToken) {
        if (cartToken != null) {
            Optional<Cart> existing = cartRepository.findByOwnerToken(cartToken);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return cartRepository.save(new Cart(UUID.randomUUID().toString()));
    }

    public CartDto getCart(String cartToken) {
        return CartDto.fromEntity(resolveCart(cartToken));
    }

    public CartDto addItem(String cartToken, AddCartItemRequest request) {
        Cart cart = resolveCart(cartToken);
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

    public CartDto updateItemQuantity(String cartToken, Long itemId, UpdateCartItemRequest request) {
        Cart cart = resolveCart(cartToken);
        findItemInCart(cart, itemId).setQuantity(request.quantity());
        return CartDto.fromEntity(cartRepository.save(cart));
    }

    public CartDto removeItem(String cartToken, Long itemId) {
        Cart cart = resolveCart(cartToken);
        cart.getItems().remove(findItemInCart(cart, itemId));
        return CartDto.fromEntity(cartRepository.save(cart));
    }

    private CartItem findItemInCart(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Cart item not found with id: " + itemId));
    }
}
