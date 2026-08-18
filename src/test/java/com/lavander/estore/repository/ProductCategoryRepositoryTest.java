package com.lavander.estore.repository;

import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductSubCategoryGroup;
import com.lavander.estore.model.PropertyDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductCategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private ProductCategoryGroupRepository groupRepository;

    @Autowired
    private ProductSubCategoryGroupRepository subGroupRepository;

    @Autowired
    private PropertyDefinitionRepository propertyDefinitionRepository;

    @Test
    void categoryCanAttachDirectlyToGroup() {
        ProductCategoryGroup cleaning = groupRepository.save(new ProductCategoryGroup("Curatenie"));

        ProductCategory detergents = new ProductCategory("Detergenti");
        detergents.setParentGroup(cleaning);

        ProductCategory saved = categoryRepository.save(detergents);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void categoryCanAttachToSubGroup() {
        ProductCategoryGroup electronics = groupRepository.save(new ProductCategoryGroup("Electronics"));
        ProductSubCategoryGroup computers = subGroupRepository.save(new ProductSubCategoryGroup("Computers", electronics));

        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setParentSubGroup(computers);

        ProductCategory saved = categoryRepository.save(laptops);

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void categoryWithNoParentIsRejected() {
        ProductCategory orphan = new ProductCategory("Orphan");

        assertThatThrownBy(() -> {
            categoryRepository.save(orphan);
            entityManager.flush();
        }).isInstanceOf(InvalidDataAccessApiUsageException.class)
         .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void categoryWithBothParentsIsRejected() {
        ProductCategoryGroup group = groupRepository.save(new ProductCategoryGroup("Electronics"));
        ProductSubCategoryGroup subGroup = subGroupRepository.save(new ProductSubCategoryGroup("Computers", group));

        ProductCategory invalid = new ProductCategory("Invalid");
        invalid.setParentGroup(group);
        invalid.setParentSubGroup(subGroup);

        assertThatThrownBy(() -> {
            categoryRepository.save(invalid);
            entityManager.flush();
        }).isInstanceOf(InvalidDataAccessApiUsageException.class)
         .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void categoryPropertiesAreSharedAcrossCategories() {
        ProductCategoryGroup electronics = groupRepository.save(new ProductCategoryGroup("Electronics"));
        PropertyDefinition ram = propertyDefinitionRepository.save(new PropertyDefinition("RAM"));

        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setParentGroup(electronics);
        laptops.setCategoryProperties(List.of(ram));
        categoryRepository.save(laptops);

        ProductCategory desktops = new ProductCategory("Desktops");
        desktops.setParentGroup(electronics);
        desktops.setCategoryProperties(List.of(ram));
        categoryRepository.save(desktops);

        entityManager.flush();
        entityManager.clear();

        ProductCategory reloadedLaptops = categoryRepository.findById(laptops.getId()).orElseThrow();
        ProductCategory reloadedDesktops = categoryRepository.findById(desktops.getId()).orElseThrow();

        assertThat(reloadedLaptops.getCategoryProperties()).extracting(PropertyDefinition::getId)
                .containsExactly(ram.getId());
        assertThat(reloadedDesktops.getCategoryProperties()).extracting(PropertyDefinition::getId)
                .containsExactly(ram.getId());
    }
}
