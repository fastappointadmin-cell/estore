# Admin CRUD API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Full create/update/delete (+ get/list) REST endpoints for the 6 catalog entities (`PropertyDefinition`, `ProductCategoryGroup`, `ProductSubCategoryGroup`, `ProductCategory`, `Product`, `ProductVariant`), for the upcoming admin UI to build against.

**Architecture:** One shared request DTO per entity (reused for POST and PUT). Delete operations guarded against orphaning references (409 on conflict) via new repository existence-check methods. A new `GlobalExceptionHandler` maps a new `NotFoundException`/`ConflictException` plus bean-validation failures to proper HTTP status codes — existing services' raw `RuntimeException("not found")` throws (which currently 500) are switched to `NotFoundException` for consistency.

**Tech Stack:** Spring Boot 4.1.0, Spring Data JPA, Jakarta Bean Validation (new dependency), Lombok.

## Global Constraints

- Root package: `com.lavander.estore`.
- Request DTOs live in `com.lavander.estore.dto` alongside the existing response DTOs; one shape per entity, no separate Create/Update types.
- `PropertyValue` gets no endpoints of its own — managed as part of `ProductVariantRequest.variantProperties`.
- Existing base paths are kept: groups under `/api/product-categories/groups`, subgroups under `/api/product-categories/subgroups`, categories under `/api/product-categories`, products under `/api/products`, variants under `/api/products/variants`.
- **Known pre-existing bugs fixed along the way** (both in files this plan already needs to rewrite, both would immediately block the admin workflow otherwise): `ProductService.getProductsByCategoryId` and `getProductVariantsByProductId` currently throw when the result list is *empty* (not just when the parent doesn't exist) — meaning a category or product created via this new admin API with zero children yet would 500 the moment the existing grid/detail pages tried to load it. Both are changed to return an empty list instead. `ProductService.getProductCategoryById` (a pre-existing misnamed method — it fetches a `Product`, not a category) is renamed to `getProductById` while gaining its first real controller mapping (there was none before).
- No automated tests are added for the new service/controller code — this project's established test convention (`@DataJpaTest` repository tests from the earlier entity-modeling work) doesn't extend to services/controllers, which have zero existing test coverage to follow. Verification is the full manual curl walkthrough in the final task.
- Prerequisite: local Postgres running (`localhost:5433`, db `lavander`) — already the case throughout this project.

---

### Task 1: Validation dependency + exception handling

**Files:**
- Modify: `build.gradle`
- Create: `src/main/java/com/lavander/estore/exception/NotFoundException.java`
- Create: `src/main/java/com/lavander/estore/exception/ConflictException.java`
- Create: `src/main/java/com/lavander/estore/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Add the validation dependency**

Add this line to `build.gradle`'s `dependencies { ... }` block, alongside the existing `implementation` lines:

```groovy
	implementation 'org.springframework.boot:spring-boot-starter-validation'
```

- [ ] **Step 2: Create `NotFoundException`**

```java
package com.lavander.estore.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
```

- [ ] **Step 3: Create `ConflictException`**

```java
package com.lavander.estore.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
```

- [ ] **Step 4: Create `GlobalExceptionHandler`**

```java
package com.lavander.estore.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (var error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Validation failed");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
```

- [ ] **Step 5: Verify the project still builds**

Run (with `export JAVA_HOME=/Users/adrianazoitei/Library/Java/JavaVirtualMachines/openjdk-26.0.1/Contents/Home` set first): `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add build.gradle src/main/java/com/lavander/estore/exception/
git commit -m "Add validation dependency and global exception handling"
```

---

### Task 2: Repository existence-check methods

**Files:**
- Modify: `src/main/java/com/lavander/estore/repository/ProductSubCategoryGroupRepository.java`
- Modify: `src/main/java/com/lavander/estore/repository/ProductCategoryRepository.java`
- Modify: `src/main/java/com/lavander/estore/repository/ProductRepository.java`
- Modify: `src/main/java/com/lavander/estore/repository/ProductVariantRepository.java`
- Modify: `src/main/java/com/lavander/estore/repository/PropertyValueRepository.java`

**Interfaces:**
- Produces: `ProductSubCategoryGroupRepository.existsByParentGroupId(Long)`, `ProductCategoryRepository.existsByParentGroupId(Long)`, `ProductCategoryRepository.existsByParentSubGroupId(Long)`, `ProductCategoryRepository.existsByCategoryPropertiesId(Long)`, `ProductRepository.existsByProductCategoryId(Long)`, `ProductRepository.existsByExtraPropertiesId(Long)`, `ProductVariantRepository.existsByProductId(Long)`, `PropertyValueRepository.existsByPropertyDefinitionId(Long)`

- [ ] **Step 1: `ProductSubCategoryGroupRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.ProductSubCategoryGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSubCategoryGroupRepository extends JpaRepository<ProductSubCategoryGroup, Long> {
    boolean existsByParentGroupId(Long parentGroupId);
}
```

- [ ] **Step 2: `ProductCategoryRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    boolean existsByParentGroupId(Long parentGroupId);
    boolean existsByParentSubGroupId(Long parentSubGroupId);
    boolean existsByCategoryPropertiesId(Long propertyDefinitionId);
}
```

- [ ] **Step 3: `ProductRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByProductCategoryId(Long categoryId);
    boolean existsByProductCategoryId(Long categoryId);
    boolean existsByExtraPropertiesId(Long propertyDefinitionId);
}
```

- [ ] **Step 4: `ProductVariantRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);
    boolean existsByProductId(Long productId);
}
```

- [ ] **Step 5: `PropertyValueRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.PropertyValue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyValueRepository extends JpaRepository<PropertyValue, Long> {
    boolean existsByPropertyDefinitionId(Long propertyDefinitionId);
}
```

- [ ] **Step 6: Verify**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL` (a bad derived-query method name shows up as a startup failure, not a compile error — this step only catches syntax issues; Task 7's full run is what actually exercises these queries).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/lavander/estore/repository/
git commit -m "Add delete-guard existence-check repository methods"
```

---

### Task 3: Request DTOs

**Files:**
- Create: `src/main/java/com/lavander/estore/dto/PropertyDefinitionRequest.java`
- Create: `src/main/java/com/lavander/estore/dto/ProductCategoryGroupRequest.java`
- Create: `src/main/java/com/lavander/estore/dto/ProductSubCategoryGroupRequest.java`
- Create: `src/main/java/com/lavander/estore/dto/ProductCategoryRequest.java`
- Create: `src/main/java/com/lavander/estore/dto/ProductRequest.java`
- Create: `src/main/java/com/lavander/estore/dto/PropertyValueInput.java`
- Create: `src/main/java/com/lavander/estore/dto/ProductVariantRequest.java`

- [ ] **Step 1: `PropertyDefinitionRequest`**

```java
package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;

public record PropertyDefinitionRequest(@NotBlank String propertyName) {
}
```

- [ ] **Step 2: `ProductCategoryGroupRequest`**

```java
package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductCategoryGroupRequest(@NotBlank String groupName) {
}
```

- [ ] **Step 3: `ProductSubCategoryGroupRequest`**

```java
package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductSubCategoryGroupRequest(@NotBlank String groupName, @NotNull Long parentGroupId) {
}
```

- [ ] **Step 4: `ProductCategoryRequest`**

```java
package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ProductCategoryRequest(
        @NotBlank String categoryName,
        Long parentGroupId,
        Long parentSubGroupId,
        List<Long> categoryPropertyIds) {
}
```

- [ ] **Step 5: `ProductRequest`**

```java
package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ProductRequest(
        @NotBlank String productName,
        String productDescription,
        @NotNull Long categoryId,
        List<Long> extraPropertyIds) {
}
```

- [ ] **Step 6: `PropertyValueInput`**

```java
package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PropertyValueInput(@NotNull Long propertyDefinitionId, @NotBlank String value) {
}
```

- [ ] **Step 7: `ProductVariantRequest`**

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
        @Valid List<PropertyValueInput> variantProperties) {
}
```

- [ ] **Step 8: Verify**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/lavander/estore/dto/PropertyDefinitionRequest.java src/main/java/com/lavander/estore/dto/ProductCategoryGroupRequest.java src/main/java/com/lavander/estore/dto/ProductSubCategoryGroupRequest.java src/main/java/com/lavander/estore/dto/ProductCategoryRequest.java src/main/java/com/lavander/estore/dto/ProductRequest.java src/main/java/com/lavander/estore/dto/PropertyValueInput.java src/main/java/com/lavander/estore/dto/ProductVariantRequest.java
git commit -m "Add request DTOs for admin CRUD endpoints"
```

---

### Task 4: PropertyDefinition CRUD

**Files:**
- Create: `src/main/java/com/lavander/estore/service/PropertyDefinitionService.java`
- Create: `src/main/java/com/lavander/estore/controller/PropertyDefinitionController.java`

**Interfaces:**
- Consumes: `PropertyDefinitionRequest` (Task 3), `NotFoundException`/`ConflictException` (Task 1), `ProductCategoryRepository.existsByCategoryPropertiesId`/`ProductRepository.existsByExtraPropertiesId`/`PropertyValueRepository.existsByPropertyDefinitionId` (Task 2)

- [ ] **Step 1: Create `PropertyDefinitionService`**

```java
package com.lavander.estore.service;

import com.lavander.estore.dto.PropertyDefinitionDto;
import com.lavander.estore.dto.PropertyDefinitionRequest;
import com.lavander.estore.exception.ConflictException;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.PropertyDefinitionRepository;
import com.lavander.estore.repository.PropertyValueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PropertyDefinitionService {

    private final PropertyDefinitionRepository propertyDefinitionRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;
    private final PropertyValueRepository propertyValueRepository;

    public PropertyDefinitionService(
            PropertyDefinitionRepository propertyDefinitionRepository,
            ProductCategoryRepository productCategoryRepository,
            ProductRepository productRepository,
            PropertyValueRepository propertyValueRepository) {
        this.propertyDefinitionRepository = propertyDefinitionRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productRepository = productRepository;
        this.propertyValueRepository = propertyValueRepository;
    }

    public List<PropertyDefinitionDto> getAll() {
        return propertyDefinitionRepository.findAll().stream().map(PropertyDefinitionDto::fromEntity).toList();
    }

    public PropertyDefinitionDto getById(Long id) {
        return PropertyDefinitionDto.fromEntity(findEntityById(id));
    }

    public PropertyDefinitionDto create(PropertyDefinitionRequest request) {
        PropertyDefinition entity = new PropertyDefinition(request.propertyName());
        return PropertyDefinitionDto.fromEntity(propertyDefinitionRepository.save(entity));
    }

    public PropertyDefinitionDto update(Long id, PropertyDefinitionRequest request) {
        PropertyDefinition entity = findEntityById(id);
        entity.setPropertyName(request.propertyName());
        return PropertyDefinitionDto.fromEntity(propertyDefinitionRepository.save(entity));
    }

    public void delete(Long id) {
        PropertyDefinition entity = findEntityById(id);
        if (productCategoryRepository.existsByCategoryPropertiesId(id)) {
            throw new ConflictException("Cannot delete property definition " + id + ": still used by a category");
        }
        if (productRepository.existsByExtraPropertiesId(id)) {
            throw new ConflictException("Cannot delete property definition " + id + ": still used by a product");
        }
        if (propertyValueRepository.existsByPropertyDefinitionId(id)) {
            throw new ConflictException("Cannot delete property definition " + id + ": still used by a variant property value");
        }
        propertyDefinitionRepository.delete(entity);
    }

    private PropertyDefinition findEntityById(Long id) {
        return propertyDefinitionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Property definition not found with id: " + id));
    }
}
```

- [ ] **Step 2: Create `PropertyDefinitionController`**

```java
package com.lavander.estore.controller;

import com.lavander.estore.dto.PropertyDefinitionDto;
import com.lavander.estore.dto.PropertyDefinitionRequest;
import com.lavander.estore.service.PropertyDefinitionService;
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
@RequestMapping("/api/property-definitions")
public class PropertyDefinitionController {

    private final PropertyDefinitionService propertyDefinitionService;

    public PropertyDefinitionController(PropertyDefinitionService propertyDefinitionService) {
        this.propertyDefinitionService = propertyDefinitionService;
    }

    @GetMapping
    public ResponseEntity<List<PropertyDefinitionDto>> getAll() {
        return ResponseEntity.ok(propertyDefinitionService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyDefinitionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(propertyDefinitionService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PropertyDefinitionDto> create(@Valid @RequestBody PropertyDefinitionRequest request) {
        return ResponseEntity.ok(propertyDefinitionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PropertyDefinitionDto> update(@PathVariable Long id, @Valid @RequestBody PropertyDefinitionRequest request) {
        return ResponseEntity.ok(propertyDefinitionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        propertyDefinitionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 3: Verify**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/lavander/estore/service/PropertyDefinitionService.java src/main/java/com/lavander/estore/controller/PropertyDefinitionController.java
git commit -m "Add PropertyDefinition CRUD endpoints"
```

---

### Task 5: Group + Subgroup + Category CRUD

**Files:**
- Modify: `src/main/java/com/lavander/estore/service/ProductCategoryService.java`
- Modify: `src/main/java/com/lavander/estore/controller/ProductCategoryController.java`

**Interfaces:**
- Consumes: `ProductCategoryGroupRequest`, `ProductSubCategoryGroupRequest`, `ProductCategoryRequest` (Task 3); `NotFoundException`/`ConflictException` (Task 1); Task 2's existence-check methods
- Produces: `ProductCategoryService` methods `getGroupById`, `createGroup`, `updateGroup`, `deleteGroup`, `getSubGroupById`, `createSubGroup`, `updateSubGroup`, `deleteSubGroup`, `createCategory`, `updateCategory`, `deleteCategory` (alongside the existing `getProductCategoryById`, `getAllCategoryGroups`)

- [ ] **Step 1: Rewrite `ProductCategoryService`**

```java
package com.lavander.estore.service;

import com.lavander.estore.dto.ProductCategoryDto;
import com.lavander.estore.dto.ProductCategoryGroupDto;
import com.lavander.estore.dto.ProductCategoryGroupRequest;
import com.lavander.estore.dto.ProductCategoryRequest;
import com.lavander.estore.dto.ProductSubCategoryGroupDto;
import com.lavander.estore.dto.ProductSubCategoryGroupRequest;
import com.lavander.estore.exception.ConflictException;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductSubCategoryGroup;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.repository.ProductCategoryGroupRepository;
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.ProductSubCategoryGroupRepository;
import com.lavander.estore.repository.PropertyDefinitionRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductCategoryGroupRepository categoryGroupRepository;
    private final ProductSubCategoryGroupRepository subCategoryGroupRepository;
    private final PropertyDefinitionRepository propertyDefinitionRepository;
    private final ProductRepository productRepository;

    public ProductCategoryService(
            ProductCategoryRepository productCategoryRepository,
            ProductCategoryGroupRepository categoryGroupRepository,
            ProductSubCategoryGroupRepository subCategoryGroupRepository,
            PropertyDefinitionRepository propertyDefinitionRepository,
            ProductRepository productRepository) {
        this.productCategoryRepository = productCategoryRepository;
        this.categoryGroupRepository = categoryGroupRepository;
        this.subCategoryGroupRepository = subCategoryGroupRepository;
        this.propertyDefinitionRepository = propertyDefinitionRepository;
        this.productRepository = productRepository;
    }

    // --- Category ---

    public ProductCategoryDto getProductCategoryById(Long id) {
        return ProductCategoryDto.fromEntity(findCategoryById(id));
    }

    public List<ProductCategoryGroupDto> getAllCategoryGroups() {
        return ProductCategoryGroupDto.fromEntities(categoryGroupRepository.findAll());
    }

    public ProductCategoryDto createCategory(ProductCategoryRequest request) {
        ProductCategory entity = new ProductCategory(request.categoryName());
        applyCategoryRequest(entity, request);
        return ProductCategoryDto.fromEntity(productCategoryRepository.save(entity));
    }

    public ProductCategoryDto updateCategory(Long id, ProductCategoryRequest request) {
        ProductCategory entity = findCategoryById(id);
        entity.setCategoryName(request.categoryName());
        applyCategoryRequest(entity, request);
        return ProductCategoryDto.fromEntity(productCategoryRepository.save(entity));
    }

    public void deleteCategory(Long id) {
        ProductCategory entity = findCategoryById(id);
        if (productRepository.existsByProductCategoryId(id)) {
            throw new ConflictException("Cannot delete category " + id + ": still has products");
        }
        productCategoryRepository.delete(entity);
    }

    private void applyCategoryRequest(ProductCategory entity, ProductCategoryRequest request) {
        boolean hasGroup = request.parentGroupId() != null;
        boolean hasSubGroup = request.parentSubGroupId() != null;
        if (hasGroup == hasSubGroup) {
            throw new IllegalArgumentException("Exactly one of parentGroupId or parentSubGroupId must be set");
        }

        if (hasGroup) {
            entity.setParentGroup(findGroupById(request.parentGroupId()));
            entity.setParentSubGroup(null);
        } else {
            entity.setParentSubGroup(findSubGroupById(request.parentSubGroupId()));
            entity.setParentGroup(null);
        }

        List<Long> propertyIds = request.categoryPropertyIds() == null ? List.of() : request.categoryPropertyIds();
        Set<PropertyDefinition> properties = new HashSet<>(propertyDefinitionRepository.findAllById(propertyIds));
        entity.setCategoryProperties(properties);
    }

    private ProductCategory findCategoryById(Long id) {
        return productCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product category not found with id: " + id));
    }

    // --- Group ---

    public ProductCategoryGroupDto getGroupById(Long id) {
        return ProductCategoryGroupDto.fromEntity(findGroupById(id));
    }

    public ProductCategoryGroupDto createGroup(ProductCategoryGroupRequest request) {
        ProductCategoryGroup entity = new ProductCategoryGroup(request.groupName());
        return ProductCategoryGroupDto.fromEntity(categoryGroupRepository.save(entity));
    }

    public ProductCategoryGroupDto updateGroup(Long id, ProductCategoryGroupRequest request) {
        ProductCategoryGroup entity = findGroupById(id);
        entity.setGroupName(request.groupName());
        return ProductCategoryGroupDto.fromEntity(categoryGroupRepository.save(entity));
    }

    public void deleteGroup(Long id) {
        ProductCategoryGroup entity = findGroupById(id);
        if (subCategoryGroupRepository.existsByParentGroupId(id)) {
            throw new ConflictException("Cannot delete group " + id + ": still has subgroups");
        }
        if (productCategoryRepository.existsByParentGroupId(id)) {
            throw new ConflictException("Cannot delete group " + id + ": still has categories");
        }
        categoryGroupRepository.delete(entity);
    }

    private ProductCategoryGroup findGroupById(Long id) {
        return categoryGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product category group not found with id: " + id));
    }

    // --- Subgroup ---

    public ProductSubCategoryGroupDto getSubGroupById(Long id) {
        return ProductSubCategoryGroupDto.fromEntity(findSubGroupById(id));
    }

    public ProductSubCategoryGroupDto createSubGroup(ProductSubCategoryGroupRequest request) {
        ProductCategoryGroup parentGroup = findGroupById(request.parentGroupId());
        ProductSubCategoryGroup entity = new ProductSubCategoryGroup(request.groupName(), parentGroup);
        return ProductSubCategoryGroupDto.fromEntity(subCategoryGroupRepository.save(entity));
    }

    public ProductSubCategoryGroupDto updateSubGroup(Long id, ProductSubCategoryGroupRequest request) {
        ProductSubCategoryGroup entity = findSubGroupById(id);
        entity.setGroupName(request.groupName());
        entity.setParentGroup(findGroupById(request.parentGroupId()));
        return ProductSubCategoryGroupDto.fromEntity(subCategoryGroupRepository.save(entity));
    }

    public void deleteSubGroup(Long id) {
        ProductSubCategoryGroup entity = findSubGroupById(id);
        if (productCategoryRepository.existsByParentSubGroupId(id)) {
            throw new ConflictException("Cannot delete subgroup " + id + ": still has categories");
        }
        subCategoryGroupRepository.delete(entity);
    }

    private ProductSubCategoryGroup findSubGroupById(Long id) {
        return subCategoryGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product subcategory group not found with id: " + id));
    }
}
```

- [ ] **Step 2: Rewrite `ProductCategoryController`**

```java
package com.lavander.estore.controller;

import com.lavander.estore.dto.ProductCategoryDto;
import com.lavander.estore.dto.ProductCategoryGroupDto;
import com.lavander.estore.dto.ProductCategoryGroupRequest;
import com.lavander.estore.dto.ProductCategoryRequest;
import com.lavander.estore.dto.ProductSubCategoryGroupDto;
import com.lavander.estore.dto.ProductSubCategoryGroupRequest;
import com.lavander.estore.service.ProductCategoryService;
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
@RequestMapping("/api/product-categories")
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    public ProductCategoryController(ProductCategoryService productCategoryService) {
        this.productCategoryService = productCategoryService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductCategoryDto> getProductCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(productCategoryService.getProductCategoryById(id));
    }

    @PostMapping
    public ResponseEntity<ProductCategoryDto> createCategory(@Valid @RequestBody ProductCategoryRequest request) {
        return ResponseEntity.ok(productCategoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductCategoryDto> updateCategory(@PathVariable Long id, @Valid @RequestBody ProductCategoryRequest request) {
        return ResponseEntity.ok(productCategoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        productCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/groups")
    public ResponseEntity<List<ProductCategoryGroupDto>> getAllCategoryGroups() {
        return ResponseEntity.ok(productCategoryService.getAllCategoryGroups());
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<ProductCategoryGroupDto> getGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(productCategoryService.getGroupById(id));
    }

    @PostMapping("/groups")
    public ResponseEntity<ProductCategoryGroupDto> createGroup(@Valid @RequestBody ProductCategoryGroupRequest request) {
        return ResponseEntity.ok(productCategoryService.createGroup(request));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<ProductCategoryGroupDto> updateGroup(@PathVariable Long id, @Valid @RequestBody ProductCategoryGroupRequest request) {
        return ResponseEntity.ok(productCategoryService.updateGroup(id, request));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        productCategoryService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subgroups/{id}")
    public ResponseEntity<ProductSubCategoryGroupDto> getSubGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(productCategoryService.getSubGroupById(id));
    }

    @PostMapping("/subgroups")
    public ResponseEntity<ProductSubCategoryGroupDto> createSubGroup(@Valid @RequestBody ProductSubCategoryGroupRequest request) {
        return ResponseEntity.ok(productCategoryService.createSubGroup(request));
    }

    @PutMapping("/subgroups/{id}")
    public ResponseEntity<ProductSubCategoryGroupDto> updateSubGroup(@PathVariable Long id, @Valid @RequestBody ProductSubCategoryGroupRequest request) {
        return ResponseEntity.ok(productCategoryService.updateSubGroup(id, request));
    }

    @DeleteMapping("/subgroups/{id}")
    public ResponseEntity<Void> deleteSubGroup(@PathVariable Long id) {
        productCategoryService.deleteSubGroup(id);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 3: Verify**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/lavander/estore/service/ProductCategoryService.java src/main/java/com/lavander/estore/controller/ProductCategoryController.java
git commit -m "Add Group/Subgroup/Category CRUD endpoints"
```

---

### Task 6: Product + Variant CRUD

**Files:**
- Modify: `src/main/java/com/lavander/estore/service/ProductService.java`
- Modify: `src/main/java/com/lavander/estore/controller/ProductController.java`

**Interfaces:**
- Consumes: `ProductRequest`, `ProductVariantRequest`, `PropertyValueInput` (Task 3); `NotFoundException`/`ConflictException` (Task 1); Task 2's existence-check methods; `ProductVariant.addVariantProperty` (existing)
- Produces: `ProductService` methods `getProductById`, `createProduct`, `updateProduct`, `deleteProduct`, `getVariantById`, `createVariant`, `updateVariant`, `deleteVariant` (`getProductsByCategoryId`/`getProductVariantsByProductId` keep their names but change behavior per Global Constraints)

- [ ] **Step 1: Rewrite `ProductService`**

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
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.PropertyDefinitionRepository;
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

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductCategoryRepository productCategoryRepository,
            PropertyDefinitionRepository propertyDefinitionRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.propertyDefinitionRepository = propertyDefinitionRepository;
    }

    // --- Product ---

    public ProductDto getProductById(Long id) {
        return ProductDto.fromEntity(findProductById(id));
    }

    public List<ProductDto> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByProductCategoryId(categoryId).stream().map(ProductDto::fromEntity).toList();
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

    public ProductVariantDto getVariantById(Long id) {
        return ProductVariantDto.fromEntity(findVariantById(id));
    }

    public ProductVariantDto createVariant(ProductVariantRequest request) {
        Product product = findProductById(request.productId());
        ProductVariant entity = new ProductVariant(
                request.variantName(), request.variantDescription(), product, request.price(), request.starRating());
        applyVariantProperties(entity, request.variantProperties());
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

    private ProductVariant findVariantById(Long id) {
        return productVariantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product variant not found with id: " + id));
    }
}
```

- [ ] **Step 2: Rewrite `ProductController`**

```java
package com.lavander.estore.controller;

import com.lavander.estore.dto.ProductDto;
import com.lavander.estore.dto.ProductRequest;
import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.dto.ProductVariantRequest;
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
}
```

- [ ] **Step 3: Verify**

Run: `./gradlew compileJava`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/lavander/estore/service/ProductService.java src/main/java/com/lavander/estore/controller/ProductController.java
git commit -m "Add Product/Variant CRUD endpoints"
```

---

### Task 7: Full manual verification

**Files:** none (verification only)

- [ ] **Step 1: Start the backend**

Restart it if already running (it needs the new code): stop the existing process on port 8080, then `export JAVA_HOME=/Users/adrianazoitei/Library/Java/JavaVirtualMachines/openjdk-26.0.1/Contents/Home && ./gradlew bootRun` (background).

- [ ] **Step 2: Full lifecycle for a leaf entity — PropertyDefinition**

```bash
curl -s -X POST localhost:8080/api/property-definitions -H 'Content-Type: application/json' -d '{"propertyName":"Test Prop"}'
# note the returned id, then:
curl -s localhost:8080/api/property-definitions/{id}
curl -s -X PUT localhost:8080/api/property-definitions/{id} -H 'Content-Type: application/json' -d '{"propertyName":"Renamed Prop"}'
curl -s -X DELETE localhost:8080/api/property-definitions/{id} -w '%{http_code}\n'
```
Expected: create returns 200 with the new id, get reflects it, update reflects the rename, delete returns 204.

- [ ] **Step 3: Group → Subgroup → Category chain, including the exactly-one-parent validation and delete guards**

```bash
curl -s -X POST localhost:8080/api/product-categories/groups -H 'Content-Type: application/json' -d '{"groupName":"Test Group"}'
# note groupId
curl -s -X POST localhost:8080/api/product-categories/subgroups -H 'Content-Type: application/json' -d '{"groupName":"Test Subgroup","parentGroupId":<groupId>}'
# note subGroupId
curl -s -X POST localhost:8080/api/product-categories -H 'Content-Type: application/json' -d '{"categoryName":"Test Category","parentSubGroupId":<subGroupId>}'
# note categoryId
curl -s -X POST localhost:8080/api/product-categories -H 'Content-Type: application/json' -d '{"categoryName":"Bad Category","parentGroupId":<groupId>,"parentSubGroupId":<subGroupId>}' -w '%{http_code}\n'
# expect 400 (both parents set)
curl -s -X DELETE localhost:8080/api/product-categories/groups/<groupId> -w '%{http_code}\n'
# expect 409 (still has subgroup)
curl -s -X DELETE localhost:8080/api/product-categories/<categoryId> -w '%{http_code}\n'
curl -s -X DELETE localhost:8080/api/product-categories/subgroups/<subGroupId> -w '%{http_code}\n'
curl -s -X DELETE localhost:8080/api/product-categories/groups/<groupId> -w '%{http_code}\n'
```
Expected: category creation succeeds; the both-parents-set request 400s; deleting the group while it still has a subgroup 409s; after deleting category, then subgroup, then the group deletes cleanly (204 each).

- [ ] **Step 4: Product → Variant chain against real seed data, including the empty-list fix and variant property replacement on update**

```bash
curl -s localhost:8080/api/products/category/1
# Laptops category (id 1 from seed data) — should list Dell/Apple/Apple(24GB)/Lenovo, not throw
curl -s -X POST localhost:8080/api/products -H 'Content-Type: application/json' -d '{"productName":"Test Product","productDescription":"desc","categoryId":1,"extraPropertyIds":[]}'
# note productId — a brand new product with zero variants
curl -s localhost:8080/api/products/<productId>/variants
# expect [] (empty list, not an error) — this is the pre-existing-bug fix
curl -s -X POST localhost:8080/api/products/variants -H 'Content-Type: application/json' -d '{"variantName":"Test Variant","variantDescription":"desc","productId":<productId>,"price":99.99,"starRating":4,"variantProperties":[{"propertyDefinitionId":1,"value":"16GB"}]}'
# note variantId
curl -s -X PUT localhost:8080/api/products/variants/<variantId> -H 'Content-Type: application/json' -d '{"variantName":"Test Variant","variantDescription":"desc","productId":<productId>,"price":149.99,"starRating":5,"variantProperties":[{"propertyDefinitionId":1,"value":"32GB"}]}'
curl -s localhost:8080/api/products/variants/<variantId>
# expect price 149.99, starRating 5, variantProperties has exactly one entry with value "32GB" (not two — confirms the clear+rebuild replaced rather than appended)
curl -s -X DELETE localhost:8080/api/products/<productId> -w '%{http_code}\n'
# expect 409 (still has the variant)
curl -s -X DELETE localhost:8080/api/products/variants/<variantId> -w '%{http_code}\n'
curl -s -X DELETE localhost:8080/api/products/<productId> -w '%{http_code}\n'
```
Expected: all as annotated above.

- [ ] **Step 5: Confirm the existing frontend still works against the updated backend**

Load `http://localhost:4200/products/electronics/computers/laptops` in a browser (or via Playwright) and confirm the grid, product detail, and variant switcher from the earlier session still work unchanged — this backend rewrite touched `ProductService`/`ProductCategoryService`/their controllers, which the existing frontend depends on for reads.
