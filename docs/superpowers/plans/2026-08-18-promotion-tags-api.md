# Promotion Tags API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a variant appear in multiple cross-cutting promotional listings (e.g. "Produse sub 20 Lei") without changing its category or duplicating product/variant data, via two new entities — `Tag` and `PromotionGroup` — independent of the existing category hierarchy.

**Architecture:** `Tag` is a simple merchandising label, many-to-many with `ProductVariant` (`variant_tag` join table). `PromotionGroup` is a named collection of tags, many-to-many with `Tag` (`promotion_group_tag` join table). A group's listing is computed on read as every variant that has at least one of the group's tags (OR match, via a `Distinct`+`In` derived query). `ProductRefDto` (nested inside `ProductVariantDto`) gains `categoryId` so the frontend can route a promoted variant back to its real category page.

**Tech Stack:** Spring Boot 4.1.0, Spring Data JPA, Jakarta Bean Validation, Lombok — same stack as the rest of this codebase, no new dependencies.

## Global Constraints

- Root package: `com.lavander.estore`.
- Follows the exact conventions established in `docs/superpowers/plans/2026-08-18-admin-crud-api.md`: one request DTO per entity reused for POST/PUT, `NotFoundException`/`ConflictException` via the existing `GlobalExceptionHandler`, delete-guards via repository `existsBy...` derived queries, constructor injection, full-file rewrites for modified service/DTO files (shown in full below, not as diffs).
- Testing follows this codebase's actual convention: `@DataJpaTest` repository tests and plain DTO-mapping unit tests (see `ProductVariantRepositoryTest`, `ProductVariantDtoMappingTest`) — not mocked service-layer tests, which this project has none of.
- Prerequisite: local Postgres running (`localhost:5433`, db `lavander`).
- `JAVA_HOME` for all Gradle commands: `export JAVA_HOME=/Users/adrianazoitei/Library/Java/JavaVirtualMachines/openjdk-26.0.1/Contents/Home`.

---

### Task 1: `Tag` and `PromotionGroup` entities

**Files:**
- Create: `src/main/java/com/lavander/estore/model/Tag.java`
- Create: `src/main/java/com/lavander/estore/model/PromotionGroup.java`
- Modify: `src/main/java/com/lavander/estore/model/ProductVariant.java`

- [ ] **Step 1: Create `Tag`**

```java
package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tagName;

    public Tag(String tagName) {
        this.tagName = tagName;
    }
}
```

- [ ] **Step 2: Create `PromotionGroup`**

```java
package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PromotionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupName;

    @ManyToMany
    @JoinTable(
            name = "promotion_group_tag",
            joinColumns = @JoinColumn(name = "promotion_group_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    public PromotionGroup(String groupName) {
        this.groupName = groupName;
    }
}
```

- [ ] **Step 3: Add a `tags` relation to `ProductVariant`** — replace the full file with:

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

    private BigDecimal price;

    private Integer starRating;

    public ProductVariant(String variantName, String variantDescription, Product product,
                           BigDecimal price, Integer starRating) {
        this.variantName = variantName;
        this.variantDescription = variantDescription;
        this.product = product;
        this.price = price;
        this.starRating = starRating;
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

- [ ] **Step 4: Verify**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/lavander/estore/model/Tag.java src/main/java/com/lavander/estore/model/PromotionGroup.java src/main/java/com/lavander/estore/model/ProductVariant.java
git commit -m "Add Tag and PromotionGroup entities"
```

---

### Task 2: DTOs

**Files:**
- Create: `src/main/java/com/lavander/estore/dto/TagDto.java`
- Create: `src/main/java/com/lavander/estore/dto/TagRequest.java`
- Create: `src/main/java/com/lavander/estore/dto/PromotionGroupDto.java`
- Create: `src/main/java/com/lavander/estore/dto/PromotionGroupRequest.java`
- Modify: `src/main/java/com/lavander/estore/dto/ProductRefDto.java`
- Modify: `src/main/java/com/lavander/estore/dto/ProductVariantDto.java`
- Modify: `src/main/java/com/lavander/estore/dto/ProductVariantRequest.java`

**Interfaces:**
- Consumes: `Tag`, `PromotionGroup` (Task 1)
- Produces: `TagDto.fromEntity(Tag)`, `PromotionGroupDto.fromEntity(PromotionGroup)`, `ProductRefDto` with a `categoryId` field, `ProductVariantDto` with a `tags` field, `ProductVariantRequest` with a `tagIds` field — all consumed by Tasks 4-6.

- [ ] **Step 1: Create `TagDto`**

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.Tag;

public record TagDto(Long id, String tagName) {

    public static TagDto fromEntity(Tag entity) {
        return new TagDto(entity.getId(), entity.getTagName());
    }
}
```

- [ ] **Step 2: Create `TagRequest`**

```java
package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;

public record TagRequest(@NotBlank String tagName) {
}
```

- [ ] **Step 3: Create `PromotionGroupDto`**

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.PromotionGroup;

import java.util.List;

public record PromotionGroupDto(Long id, String groupName, List<TagDto> tags) {

    public static PromotionGroupDto fromEntity(PromotionGroup entity) {
        List<TagDto> tags = entity.getTags().stream().map(TagDto::fromEntity).toList();
        return new PromotionGroupDto(entity.getId(), entity.getGroupName(), tags);
    }
}
```

- [ ] **Step 4: Create `PromotionGroupRequest`**

```java
package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PromotionGroupRequest(@NotBlank String groupName, List<Long> tagIds) {
}
```

- [ ] **Step 5: Add `categoryId` to `ProductRefDto`** — replace the full file with:

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.Product;

public record ProductRefDto(Long id, String productName, Long categoryId) {

    public static ProductRefDto fromEntity(Product entity) {
        return new ProductRefDto(entity.getId(), entity.getProductName(), entity.getProductCategory().getId());
    }
}
```

- [ ] **Step 6: Add `tags` to `ProductVariantDto`** — replace the full file with:

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.ProductVariant;

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
        Integer starRating) {

    public static ProductVariantDto fromEntity(ProductVariant entity) {
        List<PropertyValueDto> variantProperties = entity.getVariantProperties().stream()
                .map(PropertyValueDto::fromEntity)
                .toList();
        List<TagDto> tags = entity.getTags().stream().map(TagDto::fromEntity).toList();
        return new ProductVariantDto(
                entity.getId(),
                entity.getVariantName(),
                entity.getVariantDescription(),
                ProductRefDto.fromEntity(entity.getProduct()),
                variantProperties,
                tags,
                entity.getPrice(),
                entity.getStarRating());
    }
}
```

- [ ] **Step 7: Add `tagIds` to `ProductVariantRequest`** — replace the full file with:

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
        @NotNull Integer starRating,
        @Valid List<PropertyValueInput> variantProperties,
        List<Long> tagIds) {
}
```

- [ ] **Step 8: Verify**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/lavander/estore/dto/TagDto.java src/main/java/com/lavander/estore/dto/TagRequest.java src/main/java/com/lavander/estore/dto/PromotionGroupDto.java src/main/java/com/lavander/estore/dto/PromotionGroupRequest.java src/main/java/com/lavander/estore/dto/ProductRefDto.java src/main/java/com/lavander/estore/dto/ProductVariantDto.java src/main/java/com/lavander/estore/dto/ProductVariantRequest.java
git commit -m "Add Tag/PromotionGroup DTOs, categoryId and tags fields"
```

---

### Task 3: Repositories

**Files:**
- Create: `src/main/java/com/lavander/estore/repository/TagRepository.java`
- Create: `src/main/java/com/lavander/estore/repository/PromotionGroupRepository.java`
- Modify: `src/main/java/com/lavander/estore/repository/ProductVariantRepository.java`

**Interfaces:**
- Consumes: `Tag`, `PromotionGroup` (Task 1)
- Produces: `TagRepository` (plain CRUD), `PromotionGroupRepository.existsByTagsId(Long)`, `ProductVariantRepository.existsByTagsId(Long)`, `ProductVariantRepository.findDistinctByTagsIn(Collection<Tag>)` — consumed by Tasks 4-6.

- [ ] **Step 1: Create `TagRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
```

- [ ] **Step 2: Create `PromotionGroupRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.PromotionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionGroupRepository extends JpaRepository<PromotionGroup, Long> {
    boolean existsByTagsId(Long tagId);
}
```

- [ ] **Step 3: Add tag-matching methods to `ProductVariantRepository`** — replace the full file with:

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);
    boolean existsByProductId(Long productId);
    boolean existsByTagsId(Long tagId);
    List<ProductVariant> findDistinctByTagsIn(Collection<Tag> tags);
}
```

`findDistinctByTagsIn` is the OR-match used for a promotion group's listing: it joins on
the `tags` collection and matches any variant whose tag set intersects the given
collection; `Distinct` collapses a variant that matches more than one tag in the group
down to a single row.

- [ ] **Step 4: Verify**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL` (a bad derived-query method name only surfaces at app startup, not compile time — Task 7's repository test and Task 8's manual run are what actually exercise these queries).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/lavander/estore/repository/TagRepository.java src/main/java/com/lavander/estore/repository/PromotionGroupRepository.java src/main/java/com/lavander/estore/repository/ProductVariantRepository.java
git commit -m "Add Tag/PromotionGroup repositories and variant tag-matching query"
```

---

### Task 4: Tag CRUD

**Files:**
- Create: `src/main/java/com/lavander/estore/service/TagService.java`
- Create: `src/main/java/com/lavander/estore/controller/TagController.java`

**Interfaces:**
- Consumes: `TagDto`, `TagRequest` (Task 2); `TagRepository`, `PromotionGroupRepository.existsByTagsId`, `ProductVariantRepository.existsByTagsId` (Task 3); `NotFoundException`/`ConflictException` (existing, from `docs/superpowers/plans/2026-08-18-admin-crud-api.md` Task 1)

- [ ] **Step 1: Create `TagService`**

```java
package com.lavander.estore.service;

import com.lavander.estore.dto.TagDto;
import com.lavander.estore.dto.TagRequest;
import com.lavander.estore.exception.ConflictException;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.Tag;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.PromotionGroupRepository;
import com.lavander.estore.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;
    private final PromotionGroupRepository promotionGroupRepository;
    private final ProductVariantRepository productVariantRepository;

    public TagService(
            TagRepository tagRepository,
            PromotionGroupRepository promotionGroupRepository,
            ProductVariantRepository productVariantRepository) {
        this.tagRepository = tagRepository;
        this.promotionGroupRepository = promotionGroupRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public List<TagDto> getAll() {
        return tagRepository.findAll().stream().map(TagDto::fromEntity).toList();
    }

    public TagDto getById(Long id) {
        return TagDto.fromEntity(findEntityById(id));
    }

    public TagDto create(TagRequest request) {
        Tag entity = new Tag(request.tagName());
        return TagDto.fromEntity(tagRepository.save(entity));
    }

    public TagDto update(Long id, TagRequest request) {
        Tag entity = findEntityById(id);
        entity.setTagName(request.tagName());
        return TagDto.fromEntity(tagRepository.save(entity));
    }

    public void delete(Long id) {
        Tag entity = findEntityById(id);
        if (promotionGroupRepository.existsByTagsId(id)) {
            throw new ConflictException("Cannot delete tag " + id + ": still used by a promotion group");
        }
        if (productVariantRepository.existsByTagsId(id)) {
            throw new ConflictException("Cannot delete tag " + id + ": still used by a variant");
        }
        tagRepository.delete(entity);
    }

    private Tag findEntityById(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tag not found with id: " + id));
    }
}
```

- [ ] **Step 2: Create `TagController`**

```java
package com.lavander.estore.controller;

import com.lavander.estore.dto.TagDto;
import com.lavander.estore.dto.TagRequest;
import com.lavander.estore.service.TagService;
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
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ResponseEntity<List<TagDto>> getAll() {
        return ResponseEntity.ok(tagService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TagDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(tagService.getById(id));
    }

    @PostMapping
    public ResponseEntity<TagDto> create(@Valid @RequestBody TagRequest request) {
        return ResponseEntity.ok(tagService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TagDto> update(@PathVariable Long id, @Valid @RequestBody TagRequest request) {
        return ResponseEntity.ok(tagService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 3: Verify**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/lavander/estore/service/TagService.java src/main/java/com/lavander/estore/controller/TagController.java
git commit -m "Add Tag CRUD endpoints"
```

---

### Task 5: PromotionGroup CRUD + pooled-variants endpoint

**Files:**
- Create: `src/main/java/com/lavander/estore/service/PromotionGroupService.java`
- Create: `src/main/java/com/lavander/estore/controller/PromotionGroupController.java`

**Interfaces:**
- Consumes: `PromotionGroupDto`, `PromotionGroupRequest`, `ProductVariantDto` (Task 2); `PromotionGroupRepository`, `TagRepository`, `ProductVariantRepository.findDistinctByTagsIn` (Task 3); `NotFoundException` (existing)

- [ ] **Step 1: Create `PromotionGroupService`**

```java
package com.lavander.estore.service;

import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.dto.PromotionGroupDto;
import com.lavander.estore.dto.PromotionGroupRequest;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.PromotionGroup;
import com.lavander.estore.model.Tag;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.PromotionGroupRepository;
import com.lavander.estore.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PromotionGroupService {

    private final PromotionGroupRepository promotionGroupRepository;
    private final TagRepository tagRepository;
    private final ProductVariantRepository productVariantRepository;

    public PromotionGroupService(
            PromotionGroupRepository promotionGroupRepository,
            TagRepository tagRepository,
            ProductVariantRepository productVariantRepository) {
        this.promotionGroupRepository = promotionGroupRepository;
        this.tagRepository = tagRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public List<PromotionGroupDto> getAll() {
        return promotionGroupRepository.findAll().stream().map(PromotionGroupDto::fromEntity).toList();
    }

    public PromotionGroupDto getById(Long id) {
        return PromotionGroupDto.fromEntity(findEntityById(id));
    }

    public PromotionGroupDto create(PromotionGroupRequest request) {
        PromotionGroup entity = new PromotionGroup(request.groupName());
        entity.setTags(resolveTags(request.tagIds()));
        return PromotionGroupDto.fromEntity(promotionGroupRepository.save(entity));
    }

    public PromotionGroupDto update(Long id, PromotionGroupRequest request) {
        PromotionGroup entity = findEntityById(id);
        entity.setGroupName(request.groupName());
        entity.setTags(resolveTags(request.tagIds()));
        return PromotionGroupDto.fromEntity(promotionGroupRepository.save(entity));
    }

    public void delete(Long id) {
        promotionGroupRepository.delete(findEntityById(id));
    }

    public List<ProductVariantDto> getVariantsForGroup(Long id) {
        PromotionGroup group = findEntityById(id);
        return productVariantRepository.findDistinctByTagsIn(group.getTags()).stream()
                .map(ProductVariantDto::fromEntity)
                .toList();
    }

    private Set<Tag> resolveTags(List<Long> ids) {
        List<Long> safeIds = ids == null ? List.of() : ids;
        return new HashSet<>(tagRepository.findAllById(safeIds));
    }

    private PromotionGroup findEntityById(Long id) {
        return promotionGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Promotion group not found with id: " + id));
    }
}
```

Deleting a group needs no delete-guard (unlike deleting a `Tag`): removing a
`PromotionGroup` only clears rows in its own `promotion_group_tag` join table, which
doesn't orphan anything else.

- [ ] **Step 2: Create `PromotionGroupController`**

```java
package com.lavander.estore.controller;

import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.dto.PromotionGroupDto;
import com.lavander.estore.dto.PromotionGroupRequest;
import com.lavander.estore.service.PromotionGroupService;
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
@RequestMapping("/api/promotion-groups")
public class PromotionGroupController {

    private final PromotionGroupService promotionGroupService;

    public PromotionGroupController(PromotionGroupService promotionGroupService) {
        this.promotionGroupService = promotionGroupService;
    }

    @GetMapping
    public ResponseEntity<List<PromotionGroupDto>> getAll() {
        return ResponseEntity.ok(promotionGroupService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionGroupDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(promotionGroupService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PromotionGroupDto> create(@Valid @RequestBody PromotionGroupRequest request) {
        return ResponseEntity.ok(promotionGroupService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionGroupDto> update(@PathVariable Long id, @Valid @RequestBody PromotionGroupRequest request) {
        return ResponseEntity.ok(promotionGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promotionGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/variants")
    public ResponseEntity<List<ProductVariantDto>> getVariants(@PathVariable Long id) {
        return ResponseEntity.ok(promotionGroupService.getVariantsForGroup(id));
    }
}
```

- [ ] **Step 3: Verify**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/lavander/estore/service/PromotionGroupService.java src/main/java/com/lavander/estore/controller/PromotionGroupController.java
git commit -m "Add PromotionGroup CRUD and pooled-variants endpoint"
```

---

### Task 6: Wire tags into variant create/update

**Files:**
- Modify: `src/main/java/com/lavander/estore/service/ProductService.java`

**Interfaces:**
- Consumes: `TagRepository` (Task 3), `ProductVariantRequest.tagIds()` (Task 2)

- [ ] **Step 1: Inject `TagRepository` and resolve `tagIds` on create/update** — replace the full file with:

```java
package com.lavander.estore.service;

import com.lavander.estore.dto.ProductDto;
import com.lavander.estore.dto.ProductRequest;
import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.dto.ProductVariantRequest;
import com.lavander.estore.dto.PropertyValueInput;
import com.lavander.estore.exception.ConflictException;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.model.PropertyValue;
import com.lavander.estore.model.Tag;
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.PropertyDefinitionRepository;
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

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductCategoryRepository productCategoryRepository,
            PropertyDefinitionRepository propertyDefinitionRepository,
            TagRepository tagRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.propertyDefinitionRepository = propertyDefinitionRepository;
        this.tagRepository = tagRepository;
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
                request.variantName(), request.variantDescription(), product, request.price(), request.starRating());
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
        entity.setStarRating(request.starRating());
        entity.getVariantProperties().clear();
        applyVariantProperties(entity, request.variantProperties());
        entity.setTags(resolveTags(request.tagIds()));
        return ProductVariantDto.fromEntity(productVariantRepository.save(entity));
    }

    public void deleteVariant(Long id) {
        productVariantRepository.delete(findVariantById(id));
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

- [ ] **Step 2: Verify**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/lavander/estore/service/ProductService.java
git commit -m "Wire tags into variant create/update"
```

---

### Task 7: Repository and DTO-mapping tests

**Files:**
- Modify: `src/test/java/com/lavander/estore/repository/ProductVariantRepositoryTest.java`
- Modify: `src/test/java/com/lavander/estore/dto/ProductVariantDtoMappingTest.java`

- [ ] **Step 1: Add a `findDistinctByTagsIn` test** — replace the full file with:

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.model.PropertyValue;
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
                new BigDecimal("4999.00"), 4);
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
                new BigDecimal("4999.00"), 4);
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
                new BigDecimal("4999.00"), 4);
        xps13.setTags(Set.of(springSale, under20));
        variantRepository.save(xps13);
        entityManager.flush();
        entityManager.clear();

        List<ProductVariant> matches = variantRepository.findDistinctByTagsIn(List.of(springSale, under20));

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getId()).isEqualTo(xps13.getId());
    }
}
```

- [ ] **Step 2: Add a tags/`categoryId` mapping test** — replace the full file with:

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.model.PropertyValue;
import com.lavander.estore.model.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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

    @Test
    void variantMapsTagsAndProductCategoryId() {
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setId(10L);
        Product dell = new Product("Dell", "Dell laptops", laptops);
        dell.setId(1L);

        Tag springSale = new Tag("Promotie Primavara");
        springSale.setId(5L);

        ProductVariant xps13 = new ProductVariant("Dell XPS 13", "13-inch laptop", dell,
                new BigDecimal("4999.00"), 4);
        xps13.setId(100L);
        xps13.setTags(Set.of(springSale));

        ProductVariantDto dto = ProductVariantDto.fromEntity(xps13);

        assertThat(dto.product().categoryId()).isEqualTo(10L);
        assertThat(dto.tags()).extracting(TagDto::tagName).containsExactly("Promotie Primavara");
    }
}
```

- [ ] **Step 3: Run the tests**

Run: `./gradlew test --tests "com.lavander.estore.repository.ProductVariantRepositoryTest" --tests "com.lavander.estore.dto.ProductVariantDtoMappingTest"`
Expected: `BUILD SUCCESSFUL`, all 5 tests pass (2 pre-existing + 1 new in the repository test, 2 pre-existing + 1 new in the mapping test).

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/lavander/estore/repository/ProductVariantRepositoryTest.java src/test/java/com/lavander/estore/dto/ProductVariantDtoMappingTest.java
git commit -m "Add tests for tag-matching query and tag/categoryId DTO mapping"
```

---

### Task 8: Seed data + full manual verification

**Files:**
- Modify: `scripts/seed-catalog-data.sql`

- [ ] **Step 1: Extend the seed script**

In the `TRUNCATE TABLE` statement near the top, add the four new tables (join tables
first, since they reference the others, though `CASCADE` makes the order not strictly
required — kept explicit for consistency with every other table already listed):

```sql
TRUNCATE TABLE
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

After the existing `property_value` INSERT block (the file's last data block before the
`setval` section), add:

```sql
-- Tags
INSERT INTO tag (id, tag_name) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Produs sub 20 Lei');

-- Promotion groups
INSERT INTO promotion_group (id, group_name) OVERRIDING SYSTEM VALUE VALUES
  (1, 'Produse sub 20 Lei');

-- Promotion group <-> tag (many-to-many)
INSERT INTO promotion_group_tag (promotion_group_id, tag_id) VALUES
  (1, 1);

-- Variant <-> tag (many-to-many) — deliberately cross-category (Curatenie + Igiena)
-- to prove a promotion pools variants that don't share a real category.
INSERT INTO variant_tag (variant_id, tag_id) VALUES
  (6, 1),  -- Domestos Pine Fresh, 15 Lei
  (8, 1);  -- Alint Hartie Igienica Piersica, 10 Lei
```

In the `setval` section at the bottom, add two lines after the existing
`property_value` one:

```sql
SELECT setval(pg_get_serial_sequence('tag', 'id'), (SELECT MAX(id) FROM tag));
SELECT setval(pg_get_serial_sequence('promotion_group', 'id'), (SELECT MAX(id) FROM promotion_group));
```

- [ ] **Step 2: Re-run the seed script**

Run: `PGPASSWORD=parola123 psql -h localhost -p 5433 -U postgres -d lavander -f scripts/seed-catalog-data.sql`
Expected: `COMMIT` at the end, no errors.

- [ ] **Step 3: Start the backend**

Restart it if already running (it needs the new code): stop the existing process on
port 8080, then `export JAVA_HOME=/Users/adrianazoitei/Library/Java/JavaVirtualMachines/openjdk-26.0.1/Contents/Home && ./gradlew bootRun` (background).

- [ ] **Step 4: Tag CRUD + delete guard**

```bash
curl -s localhost:8080/api/tags
# expect the seeded "Produs sub 20 Lei" tag with id 1
curl -s -X POST localhost:8080/api/tags -H 'Content-Type: application/json' -d '{"tagName":"Test Tag"}'
# note id
curl -s -X PUT localhost:8080/api/tags/{id} -H 'Content-Type: application/json' -d '{"tagName":"Renamed Tag"}'
curl -s -X DELETE localhost:8080/api/tags/1 -w '%{http_code}\n'
# expect 409 — seeded tag is still used by promotion group 1 and by variants 6/8
```

- [ ] **Step 5: Pooled listing proves the OR-match and cross-category pooling**

```bash
curl -s localhost:8080/api/promotion-groups
# expect the seeded "Produse sub 20 Lei" group, with one tag
curl -s localhost:8080/api/promotion-groups/1/variants
# expect exactly 2 variants: "Domestos Pine Fresh" (id 6, Curatenie) and
# "Alint Hartie Igienica Piersica" (id 8, Igiena si Cosmetice) — proves the
# listing pools across unrelated categories, and that Distinct doesn't
# duplicate a variant that happens to match more than one tag
```

- [ ] **Step 6: Tag a variant through the normal update flow (not seed SQL) and confirm it round-trips**

```bash
curl -s localhost:8080/api/products/variants/1
# Dell XPS 13 — note its current variantProperties so the PUT below doesn't drop them
curl -s -X PUT localhost:8080/api/products/variants/1 -H 'Content-Type: application/json' \
  -d '{"variantName":"Dell XPS 13","variantDescription":"13-inch Dell XPS laptop","productId":1,"price":4999,"starRating":4,"variantProperties":[{"propertyDefinitionId":1,"value":"16GB"},{"propertyDefinitionId":2,"value":"13 inch"}],"tagIds":[{new tag id from Step 4}]}'
curl -s localhost:8080/api/products/variants/1
# expect tags now includes the new tag, variantProperties unchanged
curl -s -X DELETE localhost:8080/api/tags/{new tag id} -w '%{http_code}\n'
# expect 409 — still assigned to variant 1
curl -s -X PUT localhost:8080/api/products/variants/1 -H 'Content-Type: application/json' \
  -d '{"variantName":"Dell XPS 13","variantDescription":"13-inch Dell XPS laptop","productId":1,"price":4999,"starRating":4,"variantProperties":[{"propertyDefinitionId":1,"value":"16GB"},{"propertyDefinitionId":2,"value":"13 inch"}],"tagIds":[]}'
curl -s -X DELETE localhost:8080/api/tags/{new tag id} -w '%{http_code}\n'
# expect 204 now that it's untagged
```

- [ ] **Step 7: Confirm the existing frontend still works against the updated backend**

Load `http://localhost:4200/products/electronics/computers/laptops` in a browser (or
via Playwright) and confirm the grid, product detail, and variant switcher still work —
`ProductVariantDto`/`ProductRefDto` gained fields in Task 2, and the frontend hasn't
been updated to expect them yet, so this confirms the additions are backward-compatible.

- [ ] **Step 8: Commit**

```bash
git add scripts/seed-catalog-data.sql
git commit -m "Seed a cross-category promotion tag and group"
```
