package com.lavander.estore.repository;

import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.PropertyDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private ProductCategoryGroupRepository groupRepository;

    @Autowired
    private PropertyDefinitionRepository propertyDefinitionRepository;

    @Test
    void productBelongsToCategoryAndSharesExtraProperties() {
        ProductCategoryGroup electronics = groupRepository.save(new ProductCategoryGroup("Electronics"));
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setParentGroup(electronics);
        categoryRepository.save(laptops);

        PropertyDefinition chip = propertyDefinitionRepository.save(new PropertyDefinition("Chip"));

        Product apple = new Product("Apple", "Apple laptops", laptops);
        apple.setExtraProperties(Set.of(chip));
        productRepository.save(apple);

        entityManager.flush();
        entityManager.clear();

        Product reloaded = productRepository.findById(apple.getId()).orElseThrow();
        assertThat(reloaded.getProductCategory().getCategoryName()).isEqualTo("Laptops");
        assertThat(reloaded.getExtraProperties()).extracting(PropertyDefinition::getId)
                .containsExactly(chip.getId());
    }
}
