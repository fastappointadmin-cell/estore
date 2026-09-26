package com.lavander.estore.service;

import com.lavander.estore.dto.OrderDto;
import com.lavander.estore.dto.OrderRequest;
import com.lavander.estore.exception.UnauthorizedException;
import com.lavander.estore.model.Cart;
import com.lavander.estore.model.CartItem;
import com.lavander.estore.model.DeliveryMethod;
import com.lavander.estore.model.PaymentMethod;
import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.Role;
import com.lavander.estore.model.User;
import com.lavander.estore.repository.CartRepository;
import com.lavander.estore.repository.OrderRepository;
import com.lavander.estore.repository.ProductCategoryGroupRepository;
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.ProductVariantRepository;
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
class OrderServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductCategoryGroupRepository productCategoryGroupRepository;

    @Autowired
    private UserRepository userRepository;

    private OrderService newOrderService() {
        return new OrderService(orderRepository, cartRepository, userRepository);
    }

    private ProductVariant createVariant(String name, String price) {
        ProductCategoryGroup electronics = productCategoryGroupRepository.save(new ProductCategoryGroup("Electronics"));
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setParentGroup(electronics);
        productCategoryRepository.save(laptops);
        Product dell = productRepository.save(new Product("Dell", "Dell laptops", laptops));

        ProductVariant variant = new ProductVariant(name, "A laptop", dell, new BigDecimal(price));
        productVariantRepository.save(variant);
        entityManager.flush();
        return variant;
    }

    private Cart createCartWithItem(ProductVariant variant, int quantity) {
        Cart cart = cartRepository.save(new Cart(UUID.randomUUID().toString()));
        cart.getItems().add(new CartItem(cart, variant, quantity));
        cartRepository.save(cart);
        entityManager.flush();
        entityManager.clear();
        return cartRepository.findById(cart.getId()).orElseThrow();
    }

    private String createUser() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        userRepository.save(new User(email, "hash", "Test User", Role.USER));
        entityManager.flush();
        return email;
    }

    private OrderRequest sampleRequest() {
        return new OrderRequest(
                "Ion Popescu",
                "0722000000",
                "ion@example.com",
                DeliveryMethod.COURIER,
                "Str. Exemplu 1",
                "Bucuresti",
                "Bucuresti",
                "010101",
                PaymentMethod.CASH_ON_DELIVERY,
                null);
    }

    @Test
    void placeOrderComputesSubtotalFromCartItemsAndSnapshotsThem() {
        ProductVariant xps13 = createVariant("Dell XPS 13", "4999.00");
        Cart cart = createCartWithItem(xps13, 2);
        OrderService orderService = newOrderService();

        OrderDto order = orderService.placeOrder(null, cart.getOwnerToken(), sampleRequest());

        assertThat(order.subtotal()).isEqualByComparingTo("9998.00");
        assertThat(order.shippingCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(order.total()).isEqualByComparingTo("9998.00");
        assertThat(order.items()).hasSize(1);
        assertThat(order.items().get(0).variantName()).isEqualTo("Dell XPS 13");
        assertThat(order.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void placeOrderClearsTheCartAfterwards() {
        ProductVariant xps13 = createVariant("Dell XPS 13", "4999.00");
        Cart cart = createCartWithItem(xps13, 1);
        OrderService orderService = newOrderService();

        orderService.placeOrder(null, cart.getOwnerToken(), sampleRequest());
        entityManager.flush();
        entityManager.clear();

        Cart reloaded = cartRepository.findByOwnerToken(cart.getOwnerToken()).orElseThrow();
        assertThat(reloaded.getItems()).isEmpty();
    }

    @Test
    void placeOrderWithEmptyCartThrows() {
        Cart cart = cartRepository.save(new Cart(UUID.randomUUID().toString()));
        OrderService orderService = newOrderService();

        assertThatThrownBy(() -> orderService.placeOrder(null, cart.getOwnerToken(), sampleRequest()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrderWithUnknownCartTokenThrows() {
        OrderService orderService = newOrderService();

        assertThatThrownBy(() -> orderService.placeOrder(null, "does-not-exist", sampleRequest()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrderLinksTheOrderToTheLoggedInUser() {
        ProductVariant xps13 = createVariant("Dell XPS 13", "4999.00");
        Cart cart = createCartWithItem(xps13, 1);
        String email = createUser();
        OrderService orderService = newOrderService();

        orderService.placeOrder(email, cart.getOwnerToken(), sampleRequest());

        var myOrders = orderService.getMyOrders(email);
        assertThat(myOrders).hasSize(1);
    }

    @Test
    void getMyOrdersOnlyReturnsThatUsersOrdersMostRecentFirst() {
        ProductVariant xps13 = createVariant("Dell XPS 13", "4999.00");
        String emailA = createUser();
        String emailB = createUser();
        OrderService orderService = newOrderService();

        Cart cartA1 = createCartWithItem(xps13, 1);
        orderService.placeOrder(emailA, cartA1.getOwnerToken(), sampleRequest());
        Cart cartA2 = createCartWithItem(xps13, 1);
        OrderDto secondOrderForA = orderService.placeOrder(emailA, cartA2.getOwnerToken(), sampleRequest());
        Cart cartB = createCartWithItem(xps13, 1);
        orderService.placeOrder(emailB, cartB.getOwnerToken(), sampleRequest());

        var ordersForA = orderService.getMyOrders(emailA);

        assertThat(ordersForA).hasSize(2);
        assertThat(ordersForA.get(0).id()).isEqualTo(secondOrderForA.id());
    }

    @Test
    void getMyOrdersWithNoLoggedInUserThrowsUnauthorized() {
        OrderService orderService = newOrderService();

        assertThatThrownBy(() -> orderService.getMyOrders(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void placeOrderWithoutLoginLeavesTheOrderUnlinked() {
        ProductVariant xps13 = createVariant("Dell XPS 13", "4999.00");
        Cart cart = createCartWithItem(xps13, 1);
        OrderService orderService = newOrderService();

        OrderDto order = orderService.placeOrder(null, cart.getOwnerToken(), sampleRequest());

        assertThat(orderRepository.findById(order.id()).orElseThrow().getUserId()).isNull();
    }
}
