package com.lavander.estore.dto;

import com.lavander.estore.model.Cart;
import com.lavander.estore.model.CartItem;
import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductVariant;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartDtoMappingTest {

    @Test
    void cartMapsItemsWithFullVariantDetail() {
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setId(10L);
        Product dell = new Product("Dell", "Dell laptops", laptops);
        dell.setId(1L);

        ProductVariant xps13 = new ProductVariant("Dell XPS 13", "13-inch laptop", dell,
                new BigDecimal("4999.00"));
        xps13.setId(100L);

        Cart cart = new Cart("token-abc");
        cart.setId(5L);
        CartItem item = new CartItem(cart, xps13, 3);
        item.setId(50L);
        cart.setItems(List.of(item));

        CartDto dto = CartDto.fromEntity(cart);

        assertThat(dto.id()).isEqualTo(5L);
        assertThat(dto.ownerToken()).isEqualTo("token-abc");
        assertThat(dto.items()).hasSize(1);
        assertThat(dto.items().get(0).id()).isEqualTo(50L);
        assertThat(dto.items().get(0).quantity()).isEqualTo(3);
        assertThat(dto.items().get(0).variant().id()).isEqualTo(100L);
        assertThat(dto.items().get(0).variant().variantName()).isEqualTo("Dell XPS 13");
    }

    @Test
    void emptyCartMapsToEmptyItemsList() {
        Cart cart = new Cart("token-empty");
        cart.setId(6L);

        CartDto dto = CartDto.fromEntity(cart);

        assertThat(dto.items()).isEmpty();
    }
}
