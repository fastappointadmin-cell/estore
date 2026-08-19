package com.lavander.estore.service;

import com.lavander.estore.dto.AddCartItemRequest;
import com.lavander.estore.dto.CartDto;
import com.lavander.estore.dto.UpdateCartItemRequest;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.Cart;
import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.repository.CartItemRepository;
import com.lavander.estore.repository.CartRepository;
import com.lavander.estore.repository.ProductCategoryGroupRepository;
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.PropertyDefinitionRepository;
import com.lavander.estore.repository.ReviewRepository;
import com.lavander.estore.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CartServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductCategoryGroupRepository productCategoryGroupRepository;

    @Autowired
    private PropertyDefinitionRepository propertyDefinitionRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private ProductVariant createVariant() {
        ProductCategoryGroup electronics = productCategoryGroupRepository.save(new ProductCategoryGroup("Electronics"));
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setParentGroup(electronics);
        productCategoryRepository.save(laptops);
        Product dell = productRepository.save(new Product("Dell", "Dell laptops", laptops));

        ProductVariant xps13 = new ProductVariant("Dell XPS 13", "13-inch laptop", dell,
                new BigDecimal("4999.00"));
        productVariantRepository.save(xps13);
        entityManager.flush();
        return xps13;
    }

    @Test
    void resolveCartCreatesANewCartWhenNoTokenGiven() {
        CartService cartService = new CartService(cartRepository, productVariantRepository);

        Cart cart = cartService.resolveCart(null);

        assertThat(cart.getId()).isNotNull();
        assertThat(cart.getOwnerToken()).isNotBlank();
    }

    @Test
    void resolveCartReturnsTheSameCartWhenTokenMatchesAnExistingOne() {
        CartService cartService = new CartService(cartRepository, productVariantRepository);
        Cart created = cartService.resolveCart(null);
        entityManager.flush();
        entityManager.clear();

        Cart resolved = cartService.resolveCart(created.getOwnerToken());

        assertThat(resolved.getId()).isEqualTo(created.getId());
    }

    @Test
    void addItemIncrementsQuantityWhenVariantAlreadyInCart() {
        CartService cartService = new CartService(cartRepository, productVariantRepository);
        ProductVariant xps13 = createVariant();

        CartDto afterFirst = cartService.addItem(null, new AddCartItemRequest(xps13.getId(), 1));
        String token = afterFirst.ownerToken();
        CartDto afterSecond = cartService.addItem(token, new AddCartItemRequest(xps13.getId(), 2));

        assertThat(afterSecond.items()).hasSize(1);
        assertThat(afterSecond.items().get(0).quantity()).isEqualTo(3);
    }

    @Test
    void deletingVariantWithACartItemRemovesTheCartItemAndDoesNotThrow() {
        ProductService productService = new ProductService(
                productRepository,
                productVariantRepository,
                productCategoryRepository,
                propertyDefinitionRepository,
                tagRepository,
                reviewRepository,
                cartItemRepository);
        CartService cartService = new CartService(cartRepository, productVariantRepository);
        ProductVariant xps13 = createVariant();
        CartDto afterAdd = cartService.addItem(null, new AddCartItemRequest(xps13.getId(), 1));
        Long cartItemId = afterAdd.items().get(0).id();
        entityManager.flush();
        entityManager.clear();

        productService.deleteVariant(xps13.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(productVariantRepository.findById(xps13.getId())).isEmpty();
        assertThat(cartItemRepository.findById(cartItemId)).isEmpty();
    }

    @Test
    void updateItemQuantityPersistsTheNewQuantity() {
        CartService cartService = new CartService(cartRepository, productVariantRepository);
        ProductVariant xps13 = createVariant();
        CartDto afterAdd = cartService.addItem(null, new AddCartItemRequest(xps13.getId(), 1));
        String token = afterAdd.ownerToken();
        Long itemId = afterAdd.items().get(0).id();
        entityManager.flush();
        entityManager.clear();

        cartService.updateItemQuantity(token, itemId, new UpdateCartItemRequest(7));
        entityManager.flush();
        entityManager.clear();

        Cart reloaded = cartRepository.findByOwnerToken(token).orElseThrow();
        assertThat(reloaded.getItems()).hasSize(1);
        assertThat(reloaded.getItems().get(0).getQuantity()).isEqualTo(7);
    }

    @Test
    void removeItemDeletesItAndPersists() {
        CartService cartService = new CartService(cartRepository, productVariantRepository);
        ProductVariant xps13 = createVariant();
        CartDto afterAdd = cartService.addItem(null, new AddCartItemRequest(xps13.getId(), 1));
        String token = afterAdd.ownerToken();
        Long itemId = afterAdd.items().get(0).id();
        entityManager.flush();
        entityManager.clear();

        cartService.removeItem(token, itemId);
        entityManager.flush();
        entityManager.clear();

        Cart reloaded = cartRepository.findByOwnerToken(token).orElseThrow();
        assertThat(reloaded.getItems()).isEmpty();
    }

    @Test
    void updatingAnItemIdFromAnotherCartThrowsNotFound() {
        CartService cartService = new CartService(cartRepository, productVariantRepository);
        ProductVariant xps13 = createVariant();
        CartDto firstCart = cartService.addItem(null, new AddCartItemRequest(xps13.getId(), 1));
        Long itemIdInFirstCart = firstCart.items().get(0).id();

        Cart secondCart = cartService.resolveCart(null);

        assertThatThrownBy(() -> cartService.updateItemQuantity(secondCart.getOwnerToken(), itemIdInFirstCart, new UpdateCartItemRequest(5)))
                .isInstanceOf(NotFoundException.class);
    }
}
