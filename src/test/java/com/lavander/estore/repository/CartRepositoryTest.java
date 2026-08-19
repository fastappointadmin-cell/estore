package com.lavander.estore.repository;

import com.lavander.estore.model.Cart;
import com.lavander.estore.model.CartItem;
import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductVariant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CartRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private ProductCategoryGroupRepository groupRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    private ProductVariant createVariant(String name) {
        ProductCategoryGroup electronics = groupRepository.save(new ProductCategoryGroup("Electronics"));
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setParentGroup(electronics);
        categoryRepository.save(laptops);
        Product dell = productRepository.save(new Product("Dell", "Dell laptops", laptops));
        return variantRepository.save(new ProductVariant(name, "A laptop", dell, new BigDecimal("4999.00")));
    }

    @Test
    void findByOwnerTokenReturnsTheMatchingCart() {
        Cart cart = cartRepository.save(new Cart("token-123"));
        entityManager.flush();
        entityManager.clear();

        assertThat(cartRepository.findByOwnerToken("token-123")).isPresent();
        assertThat(cartRepository.findByOwnerToken("token-123").orElseThrow().getId()).isEqualTo(cart.getId());
        assertThat(cartRepository.findByOwnerToken("missing-token")).isEmpty();
    }

    @Test
    void savingCartCascadesToCartItems() {
        ProductVariant xps13 = createVariant("Dell XPS 13");
        Cart cart = new Cart("token-456");
        cart.getItems().add(new CartItem(cart, xps13, 2));

        cartRepository.save(cart);
        entityManager.flush();
        entityManager.clear();

        Cart reloaded = cartRepository.findById(cart.getId()).orElseThrow();
        assertThat(reloaded.getItems()).hasSize(1);
        assertThat(reloaded.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void removingItemFromCartDeletesItOnFlush() {
        ProductVariant xps13 = createVariant("Dell XPS 13");
        Cart cart = new Cart("token-789");
        CartItem item = new CartItem(cart, xps13, 1);
        cart.getItems().add(item);
        cartRepository.save(cart);
        entityManager.flush();

        cart.getItems().remove(item);
        cartRepository.save(cart);
        entityManager.flush();
        entityManager.clear();

        Cart reloaded = cartRepository.findById(cart.getId()).orElseThrow();
        assertThat(reloaded.getItems()).isEmpty();
    }
}
