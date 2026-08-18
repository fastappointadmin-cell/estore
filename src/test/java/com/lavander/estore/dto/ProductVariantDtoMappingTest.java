package com.lavander.estore.dto;

import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.model.PropertyValue;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductVariantDtoMappingTest {

    @Test
    void productMapsCategoryRefAndExtraProperties() {
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setId(10L);

        PropertyDefinition chip = new PropertyDefinition("Chip");
        chip.setId(3L);

        Product apple = new Product("Apple", "Apple laptops", laptops);
        apple.setId(2L);
        apple.setExtraProperties(List.of(chip));

        ProductDto dto = ProductDto.fromEntity(apple);

        assertThat(dto.productName()).isEqualTo("Apple");
        assertThat(dto.category().id()).isEqualTo(10L);
        assertThat(dto.category().categoryName()).isEqualTo("Laptops");
        assertThat(dto.extraProperties()).extracting(PropertyDefinitionDto::propertyName)
                .containsExactly("Chip");
    }

    @Test
    void variantMapsProductRefAndVariantProperties() {
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setId(10L);
        Product dell = new Product("Dell", "Dell laptops", laptops);
        dell.setId(1L);

        PropertyDefinition ram = new PropertyDefinition("RAM");
        ram.setId(1L);

        ProductVariant xps13 = new ProductVariant("Dell XPS 13", "13-inch laptop", dell,
                new BigDecimal("4999.00"), 4);
        xps13.setId(100L);
        xps13.addVariantProperty(new PropertyValue(ram, "16GB"));

        ProductVariantDto dto = ProductVariantDto.fromEntity(xps13);

        assertThat(dto.product().id()).isEqualTo(1L);
        assertThat(dto.product().productName()).isEqualTo("Dell");
        assertThat(dto.variantProperties()).hasSize(1);
        assertThat(dto.variantProperties().get(0).propertyValue()).isEqualTo("16GB");
        assertThat(dto.price()).isEqualByComparingTo("4999.00");
        assertThat(dto.starRating()).isEqualTo(4);
    }
}
