package com.lavander.estore.repository;

import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.model.PropertyValue;
import com.lavander.estore.model.Review;
import com.lavander.estore.model.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductVariantRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private ProductCategoryGroupRepository groupRepository;

    @Autowired
    private PropertyDefinitionRepository propertyDefinitionRepository;

    @Autowired
    private PropertyValueRepository propertyValueRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private Product createProduct() {
        ProductCategoryGroup electronics = groupRepository.save(new ProductCategoryGroup("Electronics"));
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setParentGroup(electronics);
        categoryRepository.save(laptops);
        return productRepository.save(new Product("Dell", "Dell laptops", laptops));
    }

    @Test
    void savingVariantCascadesToVariantProperties() {
        Product dell = createProduct();
        PropertyDefinition ram = propertyDefinitionRepository.save(new PropertyDefinition("RAM"));

        ProductVariant xps13 = new ProductVariant("Dell XPS 13", "13-inch laptop", dell,
                new BigDecimal("4999.00"));
        xps13.addVariantProperty(new PropertyValue(ram, "16GB"));

        variantRepository.save(xps13);
        entityManager.flush();
        entityManager.clear();

        ProductVariant reloaded = variantRepository.findById(xps13.getId()).orElseThrow();
        assertThat(reloaded.getVariantProperties()).hasSize(1);
        assertThat(reloaded.getVariantProperties().get(0).getPropertyValue()).isEqualTo("16GB");
    }

    @Test
    void removingVariantPropertyFromListDeletesItOnFlush() {
        Product dell = createProduct();
        PropertyDefinition ram = propertyDefinitionRepository.save(new PropertyDefinition("RAM"));

        ProductVariant xps13 = new ProductVariant("Dell XPS 13", "13-inch laptop", dell,
                new BigDecimal("4999.00"));
        PropertyValue ramValue = new PropertyValue(ram, "16GB");
        xps13.addVariantProperty(ramValue);
        variantRepository.save(xps13);
        entityManager.flush();

        xps13.removeVariantProperty(ramValue);
        entityManager.flush();
        entityManager.clear();

        ProductVariant reloaded = variantRepository.findById(xps13.getId()).orElseThrow();
        assertThat(reloaded.getVariantProperties()).isEmpty();
        assertThat(propertyValueRepository.findById(ramValue.getId())).isEmpty();
    }

    @Test
    void findDistinctByTagsInReturnsEachMatchingVariantOnce() {
        Product dell = createProduct();
        Tag springSale = tagRepository.save(new Tag("Promotie Primavara"));
        Tag under20 = tagRepository.save(new Tag("Produs sub 20 Lei"));

        ProductVariant xps13 = new ProductVariant("Dell XPS 13", "13-inch laptop", dell,
                new BigDecimal("4999.00"));
        xps13.setTags(Set.of(springSale, under20));
        variantRepository.save(xps13);
        entityManager.flush();
        entityManager.clear();

        List<ProductVariant> matches = variantRepository.findDistinctByTagsIn(List.of(springSale, under20));

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getId()).isEqualTo(xps13.getId());
    }

    @Test
    void reviewsSavedForAVariantAreVisibleAfterReload() {
        Product dell = createProduct();
        ProductVariant xps13 = new ProductVariant("Dell XPS 13", "13-inch laptop", dell,
                new BigDecimal("4999.00"));
        variantRepository.save(xps13);
        entityManager.flush();

        reviewRepository.save(new Review(xps13, 4));
        reviewRepository.save(new Review(xps13, 2));
        entityManager.flush();
        entityManager.clear();

        ProductVariant reloaded = variantRepository.findById(xps13.getId()).orElseThrow();
        assertThat(reloaded.getReviews()).hasSize(2);
        assertThat(reloaded.getReviews()).extracting(Review::getRating).containsExactlyInAnyOrder(4, 2);
    }

    @Test
    void deletingVariantWithReviewsCascadesAndDoesNotThrow() {
        Product dell = createProduct();
        ProductVariant xps13 = new ProductVariant("Dell XPS 13", "13-inch laptop", dell,
                new BigDecimal("4999.00"));
        variantRepository.save(xps13);
        entityManager.flush();

        Review firstReview = reviewRepository.save(new Review(xps13, 4));
        Review secondReview = reviewRepository.save(new Review(xps13, 2));
        entityManager.flush();
        entityManager.clear();

        ProductVariant reloaded = variantRepository.findById(xps13.getId()).orElseThrow();
        variantRepository.delete(reloaded);
        entityManager.flush();
        entityManager.clear();

        assertThat(variantRepository.findById(xps13.getId())).isEmpty();
        assertThat(reviewRepository.findById(firstReview.getId())).isEmpty();
        assertThat(reviewRepository.findById(secondReview.getId())).isEmpty();
    }
}
