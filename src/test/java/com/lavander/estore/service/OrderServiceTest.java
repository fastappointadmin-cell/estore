package com.lavander.estore.service;

import com.lavander.estore.dto.OrderDto;
import com.lavander.estore.dto.OrderRequest;
import com.lavander.estore.model.Cart;
import com.lavander.estore.model.CartItem;
import com.lavander.estore.model.DeliveryMethod;
import com.lavander.estore.model.PaymentMethod;
import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.repository.CartRepository;
import com.lavander.estore.repository.OrderRepository;
import com.lavander.estore.repository.ProductCategoryGroupRepository;
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.ProductVariantRepository;
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
        OrderService orderService = new OrderService(orderRepository, cartRepository);

        OrderDto order = orderService.placeOrder(cart.getOwnerToken(), sampleRequest());

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
        OrderService orderService = new OrderService(orderRepository, cartRepository);

        orderService.placeOrder(cart.getOwnerToken(), sampleRequest());
        entityManager.flush();
        entityManager.clear();

        Cart reloaded = cartRepository.findByOwnerToken(cart.getOwnerToken()).orElseThrow();
        assertThat(reloaded.getItems()).isEmpty();
    }

    @Test
    void placeOrderWithEmptyCartThrows() {
        Cart cart = cartRepository.save(new Cart(UUID.randomUUID().toString()));
        OrderService orderService = new OrderService(orderRepository, cartRepository);

        assertThatThrownBy(() -> orderService.placeOrder(cart.getOwnerToken(), sampleRequest()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrderWithUnknownCartTokenThrows() {
        OrderService orderService = new OrderService(orderRepository, cartRepository);

        assertThatThrownBy(() -> orderService.placeOrder("does-not-exist", sampleRequest()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
