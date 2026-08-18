package com.lavander.estore.dto;

import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductSubCategoryGroup;
import com.lavander.estore.model.PropertyDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogTreeDtoMappingTest {

    @Test
    void propertyDefinitionMapsIdAndName() {
        PropertyDefinition entity = new PropertyDefinition("RAM");
        entity.setId(1L);

        PropertyDefinitionDto dto = PropertyDefinitionDto.fromEntity(entity);

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.propertyName()).isEqualTo("RAM");
    }

    @Test
    void categoryMapsItsProperties() {
        PropertyDefinition ram = new PropertyDefinition("RAM");
        ram.setId(1L);
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setId(10L);
        laptops.setCategoryProperties(Set.of(ram));

        ProductCategoryDto dto = ProductCategoryDto.fromEntity(laptops);

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.categoryName()).isEqualTo("Laptops");
        assertThat(dto.categoryProperties()).extracting(PropertyDefinitionDto::propertyName)
                .containsExactly("RAM");
    }

    @Test
    void subGroupMapsItsCategories() {
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setId(10L);
        ProductSubCategoryGroup computers = new ProductSubCategoryGroup();
        computers.setId(2L);
        computers.setGroupName("Computers");
        computers.setCategories(List.of(laptops));

        ProductSubCategoryGroupDto dto = ProductSubCategoryGroupDto.fromEntity(computers);

        assertThat(dto.groupName()).isEqualTo("Computers");
        assertThat(dto.categories()).extracting(ProductCategoryDto::categoryName)
                .containsExactly("Laptops");
    }

    @Test
    void groupMapsSubGroupsAndDirectCategories() {
        ProductSubCategoryGroup computers = new ProductSubCategoryGroup();
        computers.setId(2L);
        computers.setGroupName("Computers");
        computers.setCategories(List.of());

        ProductCategory detergents = new ProductCategory("Detergenti");
        detergents.setId(11L);

        ProductCategoryGroup electronics = new ProductCategoryGroup("Electronics");
        electronics.setId(1L);
        electronics.setSubGroups(List.of(computers));
        electronics.setCategories(List.of(detergents));

        ProductCategoryGroupDto dto = ProductCategoryGroupDto.fromEntity(electronics);

        assertThat(dto.subGroups()).extracting(ProductSubCategoryGroupDto::groupName)
                .containsExactly("Computers");
        assertThat(dto.categories()).extracting(ProductCategoryDto::categoryName)
                .containsExactly("Detergenti");
    }
}
