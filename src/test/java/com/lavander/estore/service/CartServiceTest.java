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
import com.lavander.estore.model.Role;
import com.lavander.estore.model.User;
import com.lavander.estore.repository.CartItemRepository;
import com.lavander.estore.repository.CartRepository;
import com.lavander.estore.repository.ProductCategoryGroupRepository;
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.PropertyDefinitionRepository;
import com.lavander.estore.repository.ReviewRepository;
import com.lavander.estore.repository.TagRepository;
import com.lavander.estore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.UUID;

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

    @Autowired
    private UserRepository userRepository;

    private CartService newCartService() {
        return new CartService(cartRepository, productVariantRepository, userRepository);
    }

    private ProductVariant createVariant(String name, String price) {
        ProductCategoryGroup electronics = productCategoryGroupRepository.save(new ProductCategoryGroup("Electronics"));
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setParentGroup(electronics);
        productCategoryRepository.save(laptops);
        Product dell = productRepository.save(new Product("Dell", "Dell laptops", laptops));

        ProductVariant variant = new ProductVariant(name, "13-inch laptop", dell, new BigDecimal(price));
        productVariantRepository.save(variant);
        entityManager.flush();
        return variant;
    }

    private ProductVariant createVariant() {
        return createVariant("Dell XPS 13", "4999.00");
    }

    private String createUser() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        userRepository.save(new User(email, "hash", "Test User", Role.USER));
        entityManager.flush();
        return email;
    }

    @Test
    void resolveCartCreatesANewCartWhenNoTokenGiven() {
        CartService cartService = newCartService();

        Cart cart = cartService.resolveCart(null, null);

        assertThat(cart.getId()).isNotNull();
        assertThat(cart.getOwnerToken()).isNotBlank();
    }

    @Test
    void resolveCartReturnsTheSameCartWhenTokenMatchesAnExistingOne() {
        CartService cartService = newCartService();
        Cart created = cartService.resolveCart(null, null);
        entityManager.flush();
        entityManager.clear();

        Cart resolved = cartService.resolveCart(null, created.getOwnerToken());

        assertThat(resolved.getId()).isEqualTo(created.getId());
    }

    @Test
    void addItemIncrementsQuantityWhenVariantAlreadyInCart() {
        CartService cartService = newCartService();
        ProductVariant xps13 = createVariant();

        CartDto afterFirst = cartService.addItem(null, null, new AddCartItemRequest(xps13.getId(), 1));
        String token = afterFirst.ownerToken();
        CartDto afterSecond = cartService.addItem(null, token, new AddCartItemRequest(xps13.getId(), 2));

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
        CartService cartService = newCartService();
        ProductVariant xps13 = createVariant();
        CartDto afterAdd = cartService.addItem(null, null, new AddCartItemRequest(xps13.getId(), 1));
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
        CartService cartService = newCartService();
        ProductVariant xps13 = createVariant();
        CartDto afterAdd = cartService.addItem(null, null, new AddCartItemRequest(xps13.getId(), 1));
        String token = afterAdd.ownerToken();
        Long itemId = afterAdd.items().get(0).id();
        entityManager.flush();
        entityManager.clear();

        cartService.updateItemQuantity(null, token, itemId, new UpdateCartItemRequest(7));
        entityManager.flush();
        entityManager.clear();

        Cart reloaded = cartRepository.findByOwnerToken(token).orElseThrow();
        assertThat(reloaded.getItems()).hasSize(1);
        assertThat(reloaded.getItems().get(0).getQuantity()).isEqualTo(7);
    }

    @Test
    void removeItemDeletesItAndPersists() {
        CartService cartService = newCartService();
        ProductVariant xps13 = createVariant();
        CartDto afterAdd = cartService.addItem(null, null, new AddCartItemRequest(xps13.getId(), 1));
        String token = afterAdd.ownerToken();
        Long itemId = afterAdd.items().get(0).id();
        entityManager.flush();
        entityManager.clear();

        cartService.removeItem(null, token, itemId);
        entityManager.flush();
        entityManager.clear();

        Cart reloaded = cartRepository.findByOwnerToken(token).orElseThrow();
        assertThat(reloaded.getItems()).isEmpty();
    }

    @Test
    void updatingAnItemIdFromAnotherCartThrowsNotFound() {
        CartService cartService = newCartService();
        ProductVariant xps13 = createVariant();
        CartDto firstCart = cartService.addItem(null, null, new AddCartItemRequest(xps13.getId(), 1));
        Long itemIdInFirstCart = firstCart.items().get(0).id();

        Cart secondCart = cartService.resolveCart(null, null);

        assertThatThrownBy(() -> cartService.updateItemQuantity(null, secondCart.getOwnerToken(), itemIdInFirstCart, new UpdateCartItemRequest(5)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void resolveCartCreatesAndLinksANewCartForAnAuthenticatedUserWithNoPriorCartOrToken() {
        CartService cartService = newCartService();
        String email = createUser();

        Cart cart = cartService.resolveCart(email, null);

        assertThat(cart.getUserId()).isNotNull();
        assertThat(cartRepository.findByUserId(cart.getUserId()).orElseThrow().getId()).isEqualTo(cart.getId());
    }

    @Test
    void resolveCartClaimsAnAnonymousCartForAFirstTimeLoggedInUser() {
        CartService cartService = newCartService();
        ProductVariant xps13 = createVariant();
        CartDto anonymousCart = cartService.addItem(null, null, new AddCartItemRequest(xps13.getId(), 2));
        String anonymousToken = anonymousCart.ownerToken();
        String email = createUser();

        Cart claimed = cartService.resolveCart(email, anonymousToken);

        assertThat(claimed.getOwnerToken()).isEqualTo(anonymousToken);
        assertThat(claimed.getItems()).hasSize(1);
        assertThat(claimed.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void resolveCartReturnsTheExistingAccountCartWhenNoLocalTokenIsGiven() {
        CartService cartService = newCartService();
        String email = createUser();
        Cart firstDeviceCart = cartService.resolveCart(email, null);
        entityManager.flush();
        entityManager.clear();

        Cart secondDeviceCart = cartService.resolveCart(email, null);

        assertThat(secondDeviceCart.getId()).isEqualTo(firstDeviceCart.getId());
    }

    @Test
    void resolveCartMergesAnonymousCartItemsIntoTheAccountCartAndDeletesTheAnonymousCart() {
        CartService cartService = newCartService();
        ProductVariant xps13 = createVariant("Dell XPS 13", "4999.00");
        ProductVariant macbook = createVariant("MacBook Pro", "9999.00");
        String email = createUser();

        // The account already has a cart (e.g. from a previous device) with one item.
        Cart accountCart = cartService.resolveCart(email, null);
        cartService.addItem(email, accountCart.getOwnerToken(), new AddCartItemRequest(xps13.getId(), 1));

        // A fresh, anonymous session on this device adds a matching item plus a new one.
        CartDto anonymousCart = cartService.addItem(null, null, new AddCartItemRequest(xps13.getId(), 2));
        anonymousCart = cartService.addItem(null, anonymousCart.ownerToken(), new AddCartItemRequest(macbook.getId(), 1));
        String anonymousToken = anonymousCart.ownerToken();
        Long anonymousCartId = anonymousCart.id();

        CartDto merged = cartService.getCart(email, anonymousToken);

        assertThat(merged.ownerToken()).isEqualTo(accountCart.getOwnerToken());
        assertThat(merged.items()).hasSize(2);
        assertThat(merged.items().stream().filter(i -> i.variant().id().equals(xps13.getId())).findFirst().orElseThrow().quantity())
                .isEqualTo(3);
        assertThat(merged.items().stream().filter(i -> i.variant().id().equals(macbook.getId())).findFirst().orElseThrow().quantity())
                .isEqualTo(1);
        assertThat(cartRepository.findById(anonymousCartId)).isEmpty();
    }
}
