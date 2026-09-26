package com.lavander.estore.service;

import com.lavander.estore.dto.AddCartItemRequest;
import com.lavander.estore.dto.CartDto;
import com.lavander.estore.dto.UpdateCartItemRequest;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.Cart;
import com.lavander.estore.model.CartItem;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.User;
import com.lavander.estore.repository.CartRepository;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            ProductVariantRepository productVariantRepository,
            UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
    }

    /**
     * Frontend and backend live on different origins (different Railway subdomains), so a
     * cart-identifying cookie would be a third-party cookie and gets silently dropped by
     * modern browsers. The cart token is instead handed to the client in the response body
     * and echoed back as a request header on later calls.
     *
     * <p>When the caller is authenticated (userEmail non-null), the cart tied to that user's
     * account — not the anonymous token — is the source of truth, so it's just as
     * accessible from a second device with no local cart token at all. If both exist (an
     * anonymous cart from before login, and the user's own account cart), the anonymous
     * cart's items are merged in and it's discarded — this is the "claim your guest cart on
     * login" behavior, and it happens transparently on any cart request, not a separate step.
     */
    public Cart resolveCart(String userEmail, String cartToken) {
        Long userId = resolveUserId(userEmail);
        if (userId != null) {
            return resolveCartForUser(userId, cartToken);
        }

        if (cartToken != null) {
            Optional<Cart> existing = cartRepository.findByOwnerToken(cartToken);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return cartRepository.save(new Cart(UUID.randomUUID().toString()));
    }

    public CartDto getCart(String userEmail, String cartToken) {
        return CartDto.fromEntity(resolveCart(userEmail, cartToken));
    }

    public CartDto addItem(String userEmail, String cartToken, AddCartItemRequest request) {
        Cart cart = resolveCart(userEmail, cartToken);
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

    public CartDto updateItemQuantity(String userEmail, String cartToken, Long itemId, UpdateCartItemRequest request) {
        Cart cart = resolveCart(userEmail, cartToken);
        findItemInCart(cart, itemId).setQuantity(request.quantity());
        return CartDto.fromEntity(cartRepository.save(cart));
    }

    public CartDto removeItem(String userEmail, String cartToken, Long itemId) {
        Cart cart = resolveCart(userEmail, cartToken);
        cart.getItems().remove(findItemInCart(cart, itemId));
        return CartDto.fromEntity(cartRepository.save(cart));
    }

    private Long resolveUserId(String userEmail) {
        return userEmail != null
                ? userRepository.findByEmail(userEmail).map(User::getId).orElse(null)
                : null;
    }

    private Cart resolveCartForUser(Long userId, String cartToken) {
        Optional<Cart> userCart = cartRepository.findByUserId(userId);
        if (userCart.isPresent()) {
            Cart cart = userCart.get();
            mergeAnonymousCartIfPresent(cart, cartToken);
            return cart;
        }

        // No account cart yet — claim the anonymous one (if the caller has one) rather
        // than abandoning it, so items added before login aren't silently lost.
        if (cartToken != null) {
            Optional<Cart> anonymousCart = cartRepository.findByOwnerToken(cartToken);
            if (anonymousCart.isPresent()) {
                Cart cart = anonymousCart.get();
                cart.setUserId(userId);
                return cartRepository.save(cart);
            }
        }

        Cart cart = new Cart(UUID.randomUUID().toString());
        cart.setUserId(userId);
        return cartRepository.save(cart);
    }

    private void mergeAnonymousCartIfPresent(Cart userCart, String cartToken) {
        if (cartToken == null || cartToken.equals(userCart.getOwnerToken())) {
            return;
        }
        Optional<Cart> anonymousCart = cartRepository.findByOwnerToken(cartToken);
        if (anonymousCart.isEmpty() || anonymousCart.get().getId().equals(userCart.getId())) {
            return;
        }

        Cart cartToMerge = anonymousCart.get();
        for (CartItem item : cartToMerge.getItems()) {
            Optional<CartItem> matching = userCart.getItems().stream()
                    .filter(existing -> existing.getVariant().getId().equals(item.getVariant().getId()))
                    .findFirst();
            if (matching.isPresent()) {
                matching.get().setQuantity(matching.get().getQuantity() + item.getQuantity());
            } else {
                userCart.getItems().add(new CartItem(userCart, item.getVariant(), item.getQuantity()));
            }
        }
        cartRepository.save(userCart);
        cartRepository.delete(cartToMerge);
    }

    private CartItem findItemInCart(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Cart item not found with id: " + itemId));
    }
}
