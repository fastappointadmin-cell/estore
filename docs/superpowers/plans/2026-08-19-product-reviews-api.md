# Product Reviews API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the admin-typed, static `ProductVariant.starRating` with a real customer review mechanism — a new `Review` entity, and a rating/count computed from it on every read.

**Architecture:** `Review { id, variant, rating }` is the only new persisted concept. `ProductVariant` drops `starRating` entirely and gains a `reviews` collection; `ProductVariantDto` computes the average and count from that collection at read time — no denormalized value to keep in sync. One new endpoint persists a review and returns the freshly-computed DTO in the same response.

**Tech Stack:** Spring Boot 4.1.0, Spring Data JPA, Jakarta Bean Validation, Lombok — same stack as the rest of this codebase, no new dependencies.

**Spec:** `docs/superpowers/specs/2026-08-19-product-reviews-api-design.md`

## Global Constraints

- Root package: `com.lavander.estore`.
- Full-file rewrites for modified files (shown in full below, not as diffs) — this codebase's established convention.
- No auth, no rate limiting, no dedup on review submission — matches the approved design ("unlimited, no tracking"); this app has no auth system anywhere else to hook into.
- Testing follows this codebase's actual convention: `@DataJpaTest` repository tests and plain DTO-mapping unit tests — not mocked service-layer tests, which this project has none of.
- **This change removes fields other code depends on** (`ProductVariant`'s 5-arg constructor, `ProductVariantRequest.starRating()`), so entity + DTOs + service + controller are one compile-coupled unit — Task 1 covers all of it at once rather than splitting by layer, because no intermediate split would actually compile.
- Prerequisite: local Postgres running (`localhost:5433`, db `lavander`).
- `JAVA_HOME` for all Gradle commands: `export JAVA_HOME=/Users/adrianazoitei/Library/Java/JavaVirtualMachines/openjdk-26.0.1/Contents/Home`.

---

### Task 1: `Review` entity, DTOs, service, and endpoint

**Files:**
- Create: `src/main/java/com/lavander/estore/model/Review.java`
- Modify: `src/main/java/com/lavander/estore/model/ProductVariant.java`
- Create: `src/main/java/com/lavander/estore/dto/ReviewRequest.java`
- Modify: `src/main/java/com/lavander/estore/dto/ProductVariantDto.java`
- Modify: `src/main/java/com/lavander/estore/dto/ProductVariantRequest.java`
- Create: `src/main/java/com/lavander/estore/repository/ReviewRepository.java`
- Modify: `src/main/java/com/lavander/estore/service/ProductService.java`
- Modify: `src/main/java/com/lavander/estore/controller/ProductController.java`

**Interfaces:**
- Produces: `Review(ProductVariant variant, Integer rating)`; `ProductVariant.getReviews()/setReviews(List<Review>)` (no cascade); `ReviewRequest(Integer rating)`; `ProductVariantDto` now `(..., Double starRating, Integer reviewCount)` (was `Integer starRating`, no count); `ProductVariantRequest` with `starRating` removed; `ReviewRepository extends JpaRepository<Review, Long>`; `ProductService.submitReview(Long variantId, ReviewRequest request): ProductVariantDto`; `POST /api/products/variants/{id}/reviews`.
- Note for Task 2 (which fixes existing tests): `ProductVariant`'s constructor is now `(String variantName, String variantDescription, Product product, BigDecimal price)` — 4 args, `starRating` removed.

- [ ] **Step 1: Create `Review`**

```java
package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    private Integer rating;

    public Review(ProductVariant variant, Integer rating) {
        this.variant = variant;
        this.rating = rating;
    }
}
```

- [ ] **Step 2: Remove `starRating` from `ProductVariant`, add `reviews`** — replace the full file with:

```java
package com.lavander.estore.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String variantName;

    private String variantDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyValue> variantProperties = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "variant_tag",
            joinColumns = @JoinColumn(name = "variant_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "variant")
    private List<Review> reviews = new ArrayList<>();

    private BigDecimal price;

    public ProductVariant(String variantName, String variantDescription, Product product, BigDecimal price) {
        this.variantName = variantName;
        this.variantDescription = variantDescription;
        this.product = product;
        this.price = price;
    }

    public void addVariantProperty(PropertyValue propertyValue) {
        variantProperties.add(propertyValue);
        propertyValue.setVariant(this);
    }

    public void removeVariantProperty(PropertyValue propertyValue) {
        variantProperties.remove(propertyValue);
        propertyValue.setVariant(null);
    }
}
```

`reviews` has no cascade — reviews are persisted directly via `ReviewRepository` in
Step 7's `submitReview`, not through the variant's own save, so cascading isn't
needed.

- [ ] **Step 3: Create `ReviewRequest`**

```java
package com.lavander.estore.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(@NotNull @Min(1) @Max(5) Integer rating) {
}
```

- [ ] **Step 4: `ProductVariantDto` computes rating and count from reviews** — replace the full file with:

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.Review;

import java.math.BigDecimal;
import java.util.List;

public record ProductVariantDto(
        Long id,
        String variantName,
        String variantDescription,
        ProductRefDto product,
        List<PropertyValueDto> variantProperties,
        List<TagDto> tags,
        BigDecimal price,
        Double starRating,
        Integer reviewCount) {

    public static ProductVariantDto fromEntity(ProductVariant entity) {
        List<PropertyValueDto> variantProperties = entity.getVariantProperties().stream()
                .map(PropertyValueDto::fromEntity)
                .toList();
        List<TagDto> tags = entity.getTags().stream().map(TagDto::fromEntity).toList();
        List<Review> reviews = entity.getReviews();
        double averageRating = reviews.isEmpty()
                ? 0.0
                : reviews.stream().mapToInt(Review::getRating).average().orElse(0.0);
        return new ProductVariantDto(
                entity.getId(),
                entity.getVariantName(),
                entity.getVariantDescription(),
                ProductRefDto.fromEntity(entity.getProduct()),
                variantProperties,
                tags,
                entity.getPrice(),
                averageRating,
                reviews.size());
    }
}
```

- [ ] **Step 5: Remove `starRating` from `ProductVariantRequest`** — replace the full file with:

```java
package com.lavander.estore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ProductVariantRequest(
        @NotBlank String variantName,
        String variantDescription,
        @NotNull Long productId,
        @NotNull BigDecimal price,
        @Valid List<PropertyValueInput> variantProperties,
        List<Long> tagIds) {
}
```

- [ ] **Step 6: Create `ReviewRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
```

- [ ] **Step 7: Wire reviews into `ProductService`** — replace the full file with:

```java
package com.lavander.estore.service;

import com.lavander.estore.dto.ProductDto;
import com.lavander.estore.dto.ProductRequest;
import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.dto.ProductVariantRequest;
import com.lavander.estore.dto.PropertyValueInput;
import com.lavander.estore.dto.ReviewRequest;
import com.lavander.estore.exception.ConflictException;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.model.PropertyValue;
import com.lavander.estore.model.Review;
import com.lavander.estore.model.Tag;
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.PropertyDefinitionRepository;
import com.lavander.estore.repository.ReviewRepository;
import com.lavander.estore.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final PropertyDefinitionRepository propertyDefinitionRepository;
    private final TagRepository tagRepository;
    private final ReviewRepository reviewRepository;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductCategoryRepository productCategoryRepository,
            PropertyDefinitionRepository propertyDefinitionRepository,
            TagRepository tagRepository,
            ReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.propertyDefinitionRepository = propertyDefinitionRepository;
        this.tagRepository = tagRepository;
        this.reviewRepository = reviewRepository;
    }

    // --- Product ---

    public ProductDto getProductById(Long id) {
        return ProductDto.fromEntity(findProductById(id));
    }

    public List<ProductDto> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByProductCategoryId(categoryId).stream().map(ProductDto::fromEntity).toList();
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream().map(ProductDto::fromEntity).toList();
    }

    public ProductDto createProduct(ProductRequest request) {
        ProductCategory category = findCategoryById(request.categoryId());
        Product entity = new Product(request.productName(), request.productDescription(), category);
        entity.setExtraProperties(resolvePropertyDefinitions(request.extraPropertyIds()));
        return ProductDto.fromEntity(productRepository.save(entity));
    }

    public ProductDto updateProduct(Long id, ProductRequest request) {
        Product entity = findProductById(id);
        entity.setProductName(request.productName());
        entity.setProductDescription(request.productDescription());
        entity.setProductCategory(findCategoryById(request.categoryId()));
        entity.setExtraProperties(resolvePropertyDefinitions(request.extraPropertyIds()));
        return ProductDto.fromEntity(productRepository.save(entity));
    }

    public void deleteProduct(Long id) {
        Product entity = findProductById(id);
        if (productVariantRepository.existsByProductId(id)) {
            throw new ConflictException("Cannot delete product " + id + ": still has variants");
        }
        productRepository.delete(entity);
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
    }

    private ProductCategory findCategoryById(Long id) {
        return productCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product category not found with id: " + id));
    }

    private Set<PropertyDefinition> resolvePropertyDefinitions(List<Long> ids) {
        List<Long> safeIds = ids == null ? List.of() : ids;
        return new HashSet<>(propertyDefinitionRepository.findAllById(safeIds));
    }

    // --- Variant ---

    public List<ProductVariantDto> getProductVariantsByProductId(Long productId) {
        return productVariantRepository.findByProductId(productId).stream().map(ProductVariantDto::fromEntity).toList();
    }

    public List<ProductVariantDto> getAllVariants() {
        return productVariantRepository.findAll().stream().map(ProductVariantDto::fromEntity).toList();
    }

    public ProductVariantDto getVariantById(Long id) {
        return ProductVariantDto.fromEntity(findVariantById(id));
    }

    public ProductVariantDto createVariant(ProductVariantRequest request) {
        Product product = findProductById(request.productId());
        ProductVariant entity = new ProductVariant(
                request.variantName(), request.variantDescription(), product, request.price());
        applyVariantProperties(entity, request.variantProperties());
        entity.setTags(resolveTags(request.tagIds()));
        return ProductVariantDto.fromEntity(productVariantRepository.save(entity));
    }

    public ProductVariantDto updateVariant(Long id, ProductVariantRequest request) {
        ProductVariant entity = findVariantById(id);
        entity.setVariantName(request.variantName());
        entity.setVariantDescription(request.variantDescription());
        entity.setProduct(findProductById(request.productId()));
        entity.setPrice(request.price());
        entity.getVariantProperties().clear();
        applyVariantProperties(entity, request.variantProperties());
        entity.setTags(resolveTags(request.tagIds()));
        return ProductVariantDto.fromEntity(productVariantRepository.save(entity));
    }

    public void deleteVariant(Long id) {
        productVariantRepository.delete(findVariantById(id));
    }

    public ProductVariantDto submitReview(Long variantId, ReviewRequest request) {
        ProductVariant variant = findVariantById(variantId);
        Review review = reviewRepository.save(new Review(variant, request.rating()));
        variant.getReviews().add(review);
        return ProductVariantDto.fromEntity(variant);
    }

    private void applyVariantProperties(ProductVariant variant, List<PropertyValueInput> inputs) {
        List<PropertyValueInput> safeInputs = inputs == null ? List.of() : inputs;
        for (PropertyValueInput input : safeInputs) {
            PropertyDefinition propertyDefinition = propertyDefinitionRepository.findById(input.propertyDefinitionId())
                    .orElseThrow(() -> new NotFoundException("Property definition not found with id: " + input.propertyDefinitionId()));
            variant.addVariantProperty(new PropertyValue(propertyDefinition, input.value()));
        }
    }

    private Set<Tag> resolveTags(List<Long> ids) {
        List<Long> safeIds = ids == null ? List.of() : ids;
        return new HashSet<>(tagRepository.findAllById(safeIds));
    }

    private ProductVariant findVariantById(Long id) {
        return productVariantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product variant not found with id: " + id));
    }
}
```

The key subtlety in `submitReview`: `reviews` is the *inverse* side of the
relationship (`Review` owns the FK via `variant_id`), so saving a new `Review`
doesn't automatically update an already-loaded `variant.getReviews()` list in the
same persistence context — it's appended manually so the DTO built right after
reflects it without a second query.

- [ ] **Step 8: Add the review endpoint to `ProductController`** — replace the full file with:

```java
package com.lavander.estore.controller;

import com.lavander.estore.dto.ProductDto;
import com.lavander.estore.dto.ProductRequest;
import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.dto.ProductVariantRequest;
import com.lavander.estore.dto.ReviewRequest;
import com.lavander.estore.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDto>> getProductsByCategoryId(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategoryId(categoryId));
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductVariantDto>> getProductVariantsByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductVariantsByProductId(productId));
    }

    @GetMapping("/variants")
    public ResponseEntity<List<ProductVariantDto>> getAllVariants() {
        return ResponseEntity.ok(productService.getAllVariants());
    }

    @GetMapping("/variants/{id}")
    public ResponseEntity<ProductVariantDto> getVariantById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getVariantById(id));
    }

    @PostMapping("/variants")
    public ResponseEntity<ProductVariantDto> createVariant(@Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(productService.createVariant(request));
    }

    @PutMapping("/variants/{id}")
    public ResponseEntity<ProductVariantDto> updateVariant(@PathVariable Long id, @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(productService.updateVariant(id, request));
    }

    @DeleteMapping("/variants/{id}")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long id) {
        productService.deleteVariant(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/variants/{id}/reviews")
    public ResponseEntity<ProductVariantDto> submitReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(productService.submitReview(id, request));
    }
}
```

- [ ] **Step 9: Verify**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`. (Test sources will NOT compile yet — `ProductVariantRepositoryTest`/`ProductVariantDtoMappingTest` still call the old 5-arg constructor. That's fixed in Task 2; `compileJava` only compiles main sources, so this is expected and fine.)

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/lavander/estore/model/Review.java src/main/java/com/lavander/estore/model/ProductVariant.java src/main/java/com/lavander/estore/dto/ReviewRequest.java src/main/java/com/lavander/estore/dto/ProductVariantDto.java src/main/java/com/lavander/estore/dto/ProductVariantRequest.java src/main/java/com/lavander/estore/repository/ReviewRepository.java src/main/java/com/lavander/estore/service/ProductService.java src/main/java/com/lavander/estore/controller/ProductController.java
git commit -m "Replace static starRating with a computed Review-based rating"
```

---

### Task 2: Fix existing tests, add review tests

**Files:**
- Modify: `src/test/java/com/lavander/estore/repository/ProductVariantRepositoryTest.java`
- Modify: `src/test/java/com/lavander/estore/dto/ProductVariantDtoMappingTest.java`

**Interfaces:**
- Consumes: `ProductVariant`'s new 4-arg constructor (Task 1); `Review(ProductVariant, Integer)` (Task 1); `ReviewRepository` (Task 1); `ProductVariantDto.starRating()` now `Double`, `reviewCount()` (Task 1).

- [ ] **Step 1: Fix the constructor calls and add a review-persistence test** — replace the full file with:

```java
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
}
```

- [ ] **Step 2: Fix the constructor calls and add rating/count mapping tests** — replace the full file with:

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.model.PropertyValue;
import com.lavander.estore.model.Review;
import com.lavander.estore.model.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

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
        apple.setExtraProperties(Set.of(chip));

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
                new BigDecimal("4999.00"));
        xps13.setId(100L);
        xps13.addVariantProperty(new PropertyValue(ram, "16GB"));

        ProductVariantDto dto = ProductVariantDto.fromEntity(xps13);

        assertThat(dto.product().id()).isEqualTo(1L);
        assertThat(dto.product().productName()).isEqualTo("Dell");
        assertThat(dto.variantProperties()).hasSize(1);
        assertThat(dto.variantProperties().get(0).propertyValue()).isEqualTo("16GB");
        assertThat(dto.price()).isEqualByComparingTo("4999.00");
    }

    @Test
    void variantMapsTagsAndProductCategoryId() {
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setId(10L);
        Product dell = new Product("Dell", "Dell laptops", laptops);
        dell.setId(1L);

        Tag springSale = new Tag("Promotie Primavara");
        springSale.setId(5L);

        ProductVariant xps13 = new ProductVariant("Dell XPS 13", "13-inch laptop", dell,
                new BigDecimal("4999.00"));
        xps13.setId(100L);
        xps13.setTags(Set.of(springSale));

        ProductVariantDto dto = ProductVariantDto.fromEntity(xps13);

        assertThat(dto.product().categoryId()).isEqualTo(10L);
        assertThat(dto.tags()).extracting(TagDto::tagName).containsExactly("Promotie Primavara");
    }

    @Test
    void variantWithNoReviewsMapsToZeroRatingAndZeroCount() {
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setId(10L);
        Product dell = new Product("Dell", "Dell laptops", laptops);
        dell.setId(1L);

        ProductVariant xps13 = new ProductVariant("Dell XPS 13", "13-inch laptop", dell,
                new BigDecimal("4999.00"));
        xps13.setId(100L);

        ProductVariantDto dto = ProductVariantDto.fromEntity(xps13);

        assertThat(dto.starRating()).isEqualTo(0.0);
        assertThat(dto.reviewCount()).isEqualTo(0);
    }

    @Test
    void variantWithReviewsMapsToAverageRatingAndCount() {
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setId(10L);
        Product dell = new Product("Dell", "Dell laptops", laptops);
        dell.setId(1L);

        ProductVariant xps13 = new ProductVariant("Dell XPS 13", "13-inch laptop", dell,
                new BigDecimal("4999.00"));
        xps13.setId(100L);
        xps13.setReviews(List.of(new Review(xps13, 3), new Review(xps13, 5)));

        ProductVariantDto dto = ProductVariantDto.fromEntity(xps13);

        assertThat(dto.starRating()).isEqualTo(4.0);
        assertThat(dto.reviewCount()).isEqualTo(2);
    }
}
```

- [ ] **Step 3: Run the tests**

Run: `./gradlew test --tests "com.lavander.estore.repository.ProductVariantRepositoryTest" --tests "com.lavander.estore.dto.ProductVariantDtoMappingTest"`
Expected: `BUILD SUCCESSFUL`, all 9 tests pass (4 in the repository test, 5 in the mapping test).

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/lavander/estore/repository/ProductVariantRepositoryTest.java src/test/java/com/lavander/estore/dto/ProductVariantDtoMappingTest.java
git commit -m "Fix tests for the new constructor signature; add review mapping/persistence tests"
```

---

### Task 3: Seed data + full manual verification

**Files:**
- Modify: `scripts/seed-catalog-data.sql`

- [ ] **Step 1: Add `review` to the `TRUNCATE TABLE` list**

```sql
TRUNCATE TABLE
  review,
  variant_tag,
  promotion_group_tag,
  promotion_group,
  tag,
  property_value,
  product_variant,
  product_extra_properties,
  category_properties,
  product,
  product_category,
  product_sub_category_group,
  product_category_group,
  property_definition
RESTART IDENTITY CASCADE;
```

- [ ] **Step 2: Drop `star_rating` from the `product_variant` INSERT**

Replace the existing `product_variant` INSERT block with:

```sql
-- Product variants
INSERT INTO product_variant (id, variant_name, variant_description, product_id, price) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Dell XPS 13', '13-inch Dell XPS laptop', 1, 4999),
  (2, 'MacBook Pro 14', '14-inch MacBook Pro', 2, 9999),
  (3, 'Lenovo ThinkPad X1', '14-inch Lenovo ThinkPad X1 Carbon', 3, 6499),
  (4, 'Ariel Detergent Lichid Alpine', 'Detergent lichid pentru rufe Ariel', 4, 65),
  (5, 'Lenor Perle Parfumate Spring Awakening', 'Balsam de rufe Lenor', 5, 25),
  (6, 'Domestos Pine Fresh', 'Dezinfectant si odorizant WC Domestos', 6, 15),
  (7, 'Blend-a-med 3D White Clinical Miracle Glow', 'Pasta de dinti Blend-a-med', 7, 12),
  (8, 'Alint Hartie Igienica Piersica', 'Hartie igienica Alint', 8, 10),
  (9, 'Pantene Pro-V Miracles Lift & Volume', 'Sampon Pantene', 9, 22),
  (10, 'MacBook Pro 16 (24GB)', '16-inch MacBook Pro, 24GB RAM', 2, 14999),
  (11, 'Ariel Detergent Lichid Alpine XXL', 'Detergent lichid pentru rufe Ariel, format XXL', 4, 110),
  (12, 'MacBook Pro 16', '16-inch MacBook Pro', 2, 12999);
```

- [ ] **Step 3: Seed one review per variant, right after the `property_value` INSERT block**

```sql
-- Reviews (one per variant, seeded from each variant's old static rating so the
-- site's initial display is unchanged even though the mechanism is now real)
INSERT INTO review (id, variant_id, rating) OVERRIDING SYSTEM VALUE VALUES
  (1, 1, 4),
  (2, 2, 5),
  (3, 3, 4),
  (4, 4, 4),
  (5, 5, 5),
  (6, 6, 4),
  (7, 7, 5),
  (8, 8, 3),
  (9, 9, 4),
  (10, 10, 5),
  (11, 11, 4),
  (12, 12, 5);
```

- [ ] **Step 4: Add the `review` sequence realignment**

Add this line to the `setval` section at the bottom, after the `property_value` line:

```sql
SELECT setval(pg_get_serial_sequence('review', 'id'), (SELECT MAX(id) FROM review));
```

- [ ] **Step 5: Re-run the seed script**

Run: `PGPASSWORD=parola123 psql -h localhost -p 5433 -U postgres -d lavander -f scripts/seed-catalog-data.sql`
Expected: `COMMIT` at the end, no errors.

- [ ] **Step 6: Restart the backend**

Stop any running instance on port 8080, then `export JAVA_HOME=/Users/adrianazoitei/Library/Java/JavaVirtualMachines/openjdk-26.0.1/Contents/Home && ./gradlew bootRun` (background).

- [ ] **Step 7: Confirm the seeded rating shows up**

```bash
curl -s localhost:8080/api/products/variants/1
```
Expected: `"starRating":4.0,"reviewCount":1` for Dell XPS 13 (matches its old static rating of 4).

- [ ] **Step 8: Submit a new review and confirm the average updates**

```bash
curl -s -X POST localhost:8080/api/products/variants/1/reviews -H 'Content-Type: application/json' -d '{"rating":2}'
```
Expected: response shows `"starRating":3.0,"reviewCount":2` ((4+2)/2 = 3.0).

- [ ] **Step 9: Confirm it persisted (not just an in-memory artifact of the POST response)**

```bash
curl -s localhost:8080/api/products/variants/1
```
Expected: same `"starRating":3.0,"reviewCount":2` as Step 8.

- [ ] **Step 10: Confirm an out-of-range rating is rejected**

```bash
curl -s -X POST localhost:8080/api/products/variants/1/reviews -H 'Content-Type: application/json' -d '{"rating":6}' -w '\nHTTP_CODE:%{http_code}\n'
```
Expected: `HTTP_CODE:400`.

- [ ] **Step 11: Confirm variant creation no longer accepts/requires `starRating`**

```bash
curl -s -X POST localhost:8080/api/products/variants -H 'Content-Type: application/json' \
  -d '{"variantName":"Test Variant","variantDescription":"desc","productId":1,"price":99.99,"variantProperties":[],"tagIds":[]}'
```
Expected: 200, with `"starRating":0.0,"reviewCount":0` (a brand-new variant with no reviews yet). Note the `productId` — use whatever product id 1 resolves to in your seeded data (Dell).

- [ ] **Step 12: Confirm the existing (not-yet-updated) frontend still loads**

Load `http://localhost:4200/products/electronics/computers/laptops` in a browser (or via Playwright) and confirm the grid and product detail page still render correctly with no console errors — `ProductVariantDto`'s `starRating` changed from `Integer` to `Double` and gained `reviewCount`, both additive/compatible changes from the frontend's point of view (it just reads `starRating` as a JS `number` either way), so nothing should break ahead of the UI plan landing.

- [ ] **Step 13: Commit**

```bash
git add scripts/seed-catalog-data.sql
git commit -m "Seed one review per variant; drop the static star_rating column from seed data"
```
