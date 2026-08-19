package com.lavander.estore.service;

import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.dto.ReviewRequest;
import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.Review;
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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductServiceReviewTest {

    @Autowired
    private TestEntityManager entityManager;

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

    private ProductService productService;

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
    void submitReviewRecomputesAverageRatingAndCountAndPersists() {
        productService = new ProductService(
                productRepository,
                productVariantRepository,
                productCategoryRepository,
                propertyDefinitionRepository,
                tagRepository,
                reviewRepository);

        ProductVariant xps13 = createVariant();
        Long variantId = xps13.getId();

        ProductVariantDto afterFirst = productService.submitReview(variantId, new ReviewRequest(4));
        assertThat(afterFirst.starRating()).isEqualTo(4.0);
        assertThat(afterFirst.reviewCount()).isEqualTo(1);

        ProductVariantDto afterSecond = productService.submitReview(variantId, new ReviewRequest(2));
        assertThat(afterSecond.starRating()).isEqualTo(3.0);
        assertThat(afterSecond.reviewCount()).isEqualTo(2);

        entityManager.flush();
        entityManager.clear();

        ProductVariant reloaded = productVariantRepository.findById(variantId).orElseThrow();
        assertThat(reloaded.getReviews()).hasSize(2);
        assertThat(reloaded.getReviews()).extracting(Review::getRating)
                .containsExactlyInAnyOrder(4, 2);
    }
}
