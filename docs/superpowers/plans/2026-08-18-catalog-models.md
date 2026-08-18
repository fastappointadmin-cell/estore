# Catalog Models Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add JPA entities, DTOs, and Spring Data repositories to the `estore` backend for the product catalog domain (groups, subgroups, categories, products, variants, property definitions/values), mirroring the frontend's `lavander/src/app/models/models.ts` domain.

**Architecture:** Seven `@Entity` classes in `com.lavander.estore.model`, backed by seven `JpaRepository` interfaces in `com.lavander.estore.repository`. Nine record-based DTOs in `com.lavander.estore.dto`, each with a static `fromEntity(...)` mapping method, mirror the tree shape needed by the frontend while breaking parent-back-references to avoid JSON cycles.

**Tech Stack:** Spring Boot 4.1.0, Spring Data JPA / Hibernate, Lombok, Postgres, Java 26 (records for DTOs), JUnit 5 + AssertJ + `@DataJpaTest`.

**Design reference:** `docs/superpowers/specs/2026-08-18-catalog-models-design.md`

## Global Constraints

- Root package: `com.lavander.estore` (matches existing `EstoreApplication`).
- Entity IDs: `Long`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- DTOs: Java `record`s with a static `fromEntity(Entity)` method — no MapStruct.
- `ProductCategory` has two nullable `@ManyToOne` parents (`parentGroup`, `parentSubGroup`); exactly one must be set, enforced by a `@PrePersist`/`@PreUpdate` check that throws `IllegalStateException`.
- `PropertyDefinition` is shared via `@ManyToMany` from both `ProductCategory.categoryProperties` and `Product.extraProperties`.
- `PropertyValue` is owned by `ProductVariant.variantProperties` (`cascade = CascadeType.ALL, orphanRemoval = true`).
- Parent→child `@OneToMany` sides (`ProductCategoryGroup.subGroups`/`.categories`, `ProductSubCategoryGroup.categories`) are `mappedBy`, no cascade — children are created/deleted directly, not through the parent's collection.
- **Prerequisite:** local Postgres must be running per `application.yaml` (`localhost:5433`, db `lavander`, user `postgres`, password `parola123`) — repository tests use `@DataJpaTest` with `@AutoConfigureTestDatabase(replace = Replace.NONE)` against the real datasource (no H2), relying on `spring.jpa.hibernate.ddl-auto: update` to create tables. Each test runs in a transaction that's rolled back automatically.

---

### Task 1: Project setup + PropertyDefinition

**Files:**
- Modify: `build.gradle`
- Create: `src/main/java/com/lavander/estore/model/PropertyDefinition.java`
- Create: `src/main/java/com/lavander/estore/repository/PropertyDefinitionRepository.java`
- Test: `src/test/java/com/lavander/estore/repository/PropertyDefinitionRepositoryTest.java`

**Interfaces:**
- Produces: `PropertyDefinition { Long id; String propertyName; PropertyDefinition(String propertyName); }`, `PropertyDefinitionRepository extends JpaRepository<PropertyDefinition, Long>`

- [ ] **Step 1: Add JPA + test dependencies to `build.gradle`**

Add these two lines to the existing `dependencies { ... }` block (alongside the existing `implementation`/`testImplementation` lines):

```groovy
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'
```

- [ ] **Step 2: Write the failing test**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.PropertyDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PropertyDefinitionRepositoryTest {

    @Autowired
    private PropertyDefinitionRepository propertyDefinitionRepository;

    @Test
    void savesAndReloadsPropertyDefinition() {
        PropertyDefinition ram = new PropertyDefinition("RAM");

        PropertyDefinition saved = propertyDefinitionRepository.save(ram);

        PropertyDefinition reloaded = propertyDefinitionRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getPropertyName()).isEqualTo("RAM");
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew test --tests "com.lavander.estore.repository.PropertyDefinitionRepositoryTest"`
Expected: FAIL — compile error, `PropertyDefinition`/`PropertyDefinitionRepository` don't exist yet.

- [ ] **Step 4: Create `PropertyDefinition` entity**

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
public class PropertyDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String propertyName;

    public PropertyDefinition(String propertyName) {
        this.propertyName = propertyName;
    }
}
```

- [ ] **Step 5: Create `PropertyDefinitionRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.PropertyDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyDefinitionRepository extends JpaRepository<PropertyDefinition, Long> {
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew test --tests "com.lavander.estore.repository.PropertyDefinitionRepositoryTest"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add build.gradle src/main/java/com/lavander/estore/model/PropertyDefinition.java src/main/java/com/lavander/estore/repository/PropertyDefinitionRepository.java src/test/java/com/lavander/estore/repository/PropertyDefinitionRepositoryTest.java
git commit -m "Add PropertyDefinition entity and repository"
```

---

### Task 2: ProductCategoryGroup + ProductSubCategoryGroup

**Files:**
- Create: `src/main/java/com/lavander/estore/model/ProductCategoryGroup.java`
- Create: `src/main/java/com/lavander/estore/model/ProductSubCategoryGroup.java`
- Create: `src/main/java/com/lavander/estore/repository/ProductCategoryGroupRepository.java`
- Create: `src/main/java/com/lavander/estore/repository/ProductSubCategoryGroupRepository.java`
- Test: `src/test/java/com/lavander/estore/repository/ProductCategoryGroupRepositoryTest.java`

**Interfaces:**
- Consumes: none (new aggregate)
- Produces: `ProductCategoryGroup { Long id; String groupName; List<ProductSubCategoryGroup> subGroups; List<ProductCategory> categories; ProductCategoryGroup(String groupName); }`, `ProductSubCategoryGroup { Long id; String groupName; ProductCategoryGroup parentGroup; List<ProductCategory> categories; ProductSubCategoryGroup(String groupName, ProductCategoryGroup parentGroup); }`

- [ ] **Step 1: Write the failing test**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductSubCategoryGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductCategoryGroupRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductCategoryGroupRepository groupRepository;

    @Autowired
    private ProductSubCategoryGroupRepository subGroupRepository;

    @Test
    void subGroupIsVisibleFromParentGroupAfterReload() {
        ProductCategoryGroup electronics = groupRepository.save(new ProductCategoryGroup("Electronics"));
        subGroupRepository.save(new ProductSubCategoryGroup("Computers", electronics));

        entityManager.flush();
        entityManager.clear();

        ProductCategoryGroup reloaded = groupRepository.findById(electronics.getId()).orElseThrow();
        assertThat(reloaded.getSubGroups()).hasSize(1);
        assertThat(reloaded.getSubGroups().get(0).getGroupName()).isEqualTo("Computers");
    }

    @Test
    void deletingSubGroupDoesNotCascadeDeleteParentGroup() {
        ProductCategoryGroup electronics = groupRepository.save(new ProductCategoryGroup("Electronics"));
        ProductSubCategoryGroup computers = subGroupRepository.save(new ProductSubCategoryGroup("Computers", electronics));
        entityManager.flush();

        subGroupRepository.delete(computers);
        entityManager.flush();

        assertThat(groupRepository.findById(electronics.getId())).isPresent();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.lavander.estore.repository.ProductCategoryGroupRepositoryTest"`
Expected: FAIL — compile error, entities/repositories don't exist yet.

- [ ] **Step 3: Create `ProductCategoryGroup` entity**

```java
package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProductCategoryGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupName;

    @OneToMany(mappedBy = "parentGroup")
    private List<ProductSubCategoryGroup> subGroups = new ArrayList<>();

    @OneToMany(mappedBy = "parentGroup")
    private List<ProductCategory> categories = new ArrayList<>();

    public ProductCategoryGroup(String groupName) {
        this.groupName = groupName;
    }
}
```

- [ ] **Step 4: Create `ProductSubCategoryGroup` entity**

```java
package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProductSubCategoryGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupName;

    @ManyToOne
    @JoinColumn(name = "parent_group_id", nullable = false)
    private ProductCategoryGroup parentGroup;

    @OneToMany(mappedBy = "parentSubGroup")
    private List<ProductCategory> categories = new ArrayList<>();

    public ProductSubCategoryGroup(String groupName, ProductCategoryGroup parentGroup) {
        this.groupName = groupName;
        this.parentGroup = parentGroup;
    }
}
```

Note: `ProductCategory` is referenced here (`parentSubGroup`, `List<ProductCategory>`) but doesn't exist until Task 3 — this file won't compile on its own until Task 3 is also done. That's fine within this task's step ordering (test still won't pass until Task 3), but if running tasks strictly one-at-a-time via subagents, do Task 3 immediately after this one before expecting `./gradlew build` to succeed project-wide.

- [ ] **Step 5: Create `ProductCategoryGroupRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.ProductCategoryGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryGroupRepository extends JpaRepository<ProductCategoryGroup, Long> {
}
```

- [ ] **Step 6: Create `ProductSubCategoryGroupRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.ProductSubCategoryGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSubCategoryGroupRepository extends JpaRepository<ProductSubCategoryGroup, Long> {
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./gradlew test --tests "com.lavander.estore.repository.ProductCategoryGroupRepositoryTest"`
Expected: PASS (requires Task 3's `ProductCategory` class to exist to compile — do Task 3 first if compilation fails on `ProductCategory` symbol, then return here)

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/lavander/estore/model/ProductCategoryGroup.java src/main/java/com/lavander/estore/model/ProductSubCategoryGroup.java src/main/java/com/lavander/estore/repository/ProductCategoryGroupRepository.java src/main/java/com/lavander/estore/repository/ProductSubCategoryGroupRepository.java src/test/java/com/lavander/estore/repository/ProductCategoryGroupRepositoryTest.java
git commit -m "Add ProductCategoryGroup and ProductSubCategoryGroup entities"
```

---

### Task 3: ProductCategory

**Files:**
- Create: `src/main/java/com/lavander/estore/model/ProductCategory.java`
- Create: `src/main/java/com/lavander/estore/repository/ProductCategoryRepository.java`
- Test: `src/test/java/com/lavander/estore/repository/ProductCategoryRepositoryTest.java`

**Interfaces:**
- Consumes: `ProductCategoryGroup` (Task 2), `ProductSubCategoryGroup` (Task 2), `PropertyDefinition` (Task 1)
- Produces: `ProductCategory { Long id; String categoryName; ProductCategoryGroup parentGroup; ProductSubCategoryGroup parentSubGroup; List<PropertyDefinition> categoryProperties; ProductCategory(String categoryName); }`

- [ ] **Step 1: Write the failing test**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductSubCategoryGroup;
import com.lavander.estore.model.PropertyDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

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
        }).isInstanceOf(IllegalStateException.class);
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
        }).isInstanceOf(IllegalStateException.class);
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.lavander.estore.repository.ProductCategoryRepositoryTest"`
Expected: FAIL — compile error, `ProductCategory`/`ProductCategoryRepository` don't exist yet.

- [ ] **Step 3: Create `ProductCategory` entity**

```java
package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String categoryName;

    @ManyToOne
    @JoinColumn(name = "parent_group_id")
    private ProductCategoryGroup parentGroup;

    @ManyToOne
    @JoinColumn(name = "parent_subgroup_id")
    private ProductSubCategoryGroup parentSubGroup;

    @ManyToMany
    @JoinTable(
            name = "category_properties",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "property_definition_id"))
    private List<PropertyDefinition> categoryProperties = new ArrayList<>();

    public ProductCategory(String categoryName) {
        this.categoryName = categoryName;
    }

    @PrePersist
    @PreUpdate
    private void validateExactlyOneParent() {
        boolean hasGroup = parentGroup != null;
        boolean hasSubGroup = parentSubGroup != null;
        if (hasGroup == hasSubGroup) {
            throw new IllegalStateException(
                    "ProductCategory must have exactly one of parentGroup or parentSubGroup set");
        }
    }
}
```

- [ ] **Step 4: Create `ProductCategoryRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "com.lavander.estore.repository.ProductCategoryRepositoryTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/lavander/estore/model/ProductCategory.java src/main/java/com/lavander/estore/repository/ProductCategoryRepository.java src/test/java/com/lavander/estore/repository/ProductCategoryRepositoryTest.java
git commit -m "Add ProductCategory entity with parent-group validation"
```

---

### Task 4: Product

**Files:**
- Create: `src/main/java/com/lavander/estore/model/Product.java`
- Create: `src/main/java/com/lavander/estore/repository/ProductRepository.java`
- Test: `src/test/java/com/lavander/estore/repository/ProductRepositoryTest.java`

**Interfaces:**
- Consumes: `ProductCategory` (Task 3), `ProductCategoryGroup` (Task 2), `PropertyDefinition` (Task 1)
- Produces: `Product { Long id; String productName; String productDescription; ProductCategory productCategory; List<PropertyDefinition> extraProperties; Product(String productName, String productDescription, ProductCategory productCategory); }`

- [ ] **Step 1: Write the failing test**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.PropertyDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

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
        apple.setExtraProperties(List.of(chip));
        productRepository.save(apple);

        entityManager.flush();
        entityManager.clear();

        Product reloaded = productRepository.findById(apple.getId()).orElseThrow();
        assertThat(reloaded.getProductCategory().getCategoryName()).isEqualTo("Laptops");
        assertThat(reloaded.getExtraProperties()).extracting(PropertyDefinition::getId)
                .containsExactly(chip.getId());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.lavander.estore.repository.ProductRepositoryTest"`
Expected: FAIL — compile error, `Product`/`ProductRepository` don't exist yet.

- [ ] **Step 3: Create `Product` entity**

```java
package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productName;

    private String productDescription;

    @ManyToOne
    @JoinColumn(name = "product_category_id", nullable = false)
    private ProductCategory productCategory;

    @ManyToMany
    @JoinTable(
            name = "product_extra_properties",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "property_definition_id"))
    private List<PropertyDefinition> extraProperties = new ArrayList<>();

    public Product(String productName, String productDescription, ProductCategory productCategory) {
        this.productName = productName;
        this.productDescription = productDescription;
        this.productCategory = productCategory;
    }
}
```

- [ ] **Step 4: Create `ProductRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "com.lavander.estore.repository.ProductRepositoryTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/lavander/estore/model/Product.java src/main/java/com/lavander/estore/repository/ProductRepository.java src/test/java/com/lavander/estore/repository/ProductRepositoryTest.java
git commit -m "Add Product entity"
```

---

### Task 5: ProductVariant + PropertyValue

**Files:**
- Create: `src/main/java/com/lavander/estore/model/ProductVariant.java`
- Create: `src/main/java/com/lavander/estore/model/PropertyValue.java`
- Create: `src/main/java/com/lavander/estore/repository/ProductVariantRepository.java`
- Create: `src/main/java/com/lavander/estore/repository/PropertyValueRepository.java`
- Test: `src/test/java/com/lavander/estore/repository/ProductVariantRepositoryTest.java`

**Interfaces:**
- Consumes: `Product` (Task 4), `PropertyDefinition` (Task 1)
- Produces: `ProductVariant { Long id; String variantName; String variantDescription; Product product; List<PropertyValue> variantProperties; BigDecimal price; Integer starRating; ProductVariant(String variantName, String variantDescription, Product product, BigDecimal price, Integer starRating); void addVariantProperty(PropertyValue); void removeVariantProperty(PropertyValue); }`, `PropertyValue { Long id; ProductVariant variant; PropertyDefinition propertyDefinition; String propertyValue; PropertyValue(PropertyDefinition propertyDefinition, String propertyValue); }`

- [ ] **Step 1: Write the failing test**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.model.PropertyValue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;

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
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.lavander.estore.repository.ProductVariantRepositoryTest"`
Expected: FAIL — compile error, `ProductVariant`/`PropertyValue`/repositories don't exist yet.

- [ ] **Step 3: Create `PropertyValue` entity**

```java
package com.lavander.estore.model;

import jakarta.persistence.Entity;
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
public class PropertyValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne
    @JoinColumn(name = "property_definition_id", nullable = false)
    private PropertyDefinition propertyDefinition;

    private String propertyValue;

    public PropertyValue(PropertyDefinition propertyDefinition, String propertyValue) {
        this.propertyDefinition = propertyDefinition;
        this.propertyValue = propertyValue;
    }
}
```

- [ ] **Step 4: Create `ProductVariant` entity**

```java
package com.lavander.estore.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyValue> variantProperties = new ArrayList<>();

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

- [ ] **Step 5: Create `ProductVariantRepository` and `PropertyValueRepository`**

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
}
```

```java
package com.lavander.estore.repository;

import com.lavander.estore.model.PropertyValue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyValueRepository extends JpaRepository<PropertyValue, Long> {
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew test --tests "com.lavander.estore.repository.ProductVariantRepositoryTest"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/lavander/estore/model/ProductVariant.java src/main/java/com/lavander/estore/model/PropertyValue.java src/main/java/com/lavander/estore/repository/ProductVariantRepository.java src/main/java/com/lavander/estore/repository/PropertyValueRepository.java src/test/java/com/lavander/estore/repository/ProductVariantRepositoryTest.java
git commit -m "Add ProductVariant and PropertyValue entities"
```

---

### Task 6: Catalog-tree DTOs (PropertyDefinition, Category, SubGroup, Group)

**Files:**
- Create: `src/main/java/com/lavander/estore/dto/PropertyDefinitionDto.java`
- Create: `src/main/java/com/lavander/estore/dto/ProductCategoryDto.java`
- Create: `src/main/java/com/lavander/estore/dto/ProductSubCategoryGroupDto.java`
- Create: `src/main/java/com/lavander/estore/dto/ProductCategoryGroupDto.java`
- Test: `src/test/java/com/lavander/estore/dto/CatalogTreeDtoMappingTest.java`

**Interfaces:**
- Consumes: `PropertyDefinition` (Task 1), `ProductCategory` (Task 3), `ProductSubCategoryGroup` (Task 2), `ProductCategoryGroup` (Task 2)
- Produces: `PropertyDefinitionDto(Long id, String propertyName)`, `ProductCategoryDto(Long id, String categoryName, List<PropertyDefinitionDto> categoryProperties)`, `ProductSubCategoryGroupDto(Long id, String groupName, List<ProductCategoryDto> categories)`, `ProductCategoryGroupDto(Long id, String groupName, List<ProductSubCategoryGroupDto> subGroups, List<ProductCategoryDto> categories)` — each with static `fromEntity(...)`

- [ ] **Step 1: Write the failing test**

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductSubCategoryGroup;
import com.lavander.estore.model.PropertyDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        laptops.setCategoryProperties(List.of(ram));

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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.lavander.estore.dto.CatalogTreeDtoMappingTest"`
Expected: FAIL — compile error, DTO classes don't exist yet.

- [ ] **Step 3: Create `PropertyDefinitionDto`**

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.PropertyDefinition;

public record PropertyDefinitionDto(Long id, String propertyName) {

    public static PropertyDefinitionDto fromEntity(PropertyDefinition entity) {
        return new PropertyDefinitionDto(entity.getId(), entity.getPropertyName());
    }
}
```

- [ ] **Step 4: Create `ProductCategoryDto`**

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.ProductCategory;

import java.util.List;

public record ProductCategoryDto(Long id, String categoryName, List<PropertyDefinitionDto> categoryProperties) {

    public static ProductCategoryDto fromEntity(ProductCategory entity) {
        List<PropertyDefinitionDto> properties = entity.getCategoryProperties().stream()
                .map(PropertyDefinitionDto::fromEntity)
                .toList();
        return new ProductCategoryDto(entity.getId(), entity.getCategoryName(), properties);
    }
}
```

- [ ] **Step 5: Create `ProductSubCategoryGroupDto`**

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.ProductSubCategoryGroup;

import java.util.List;

public record ProductSubCategoryGroupDto(Long id, String groupName, List<ProductCategoryDto> categories) {

    public static ProductSubCategoryGroupDto fromEntity(ProductSubCategoryGroup entity) {
        List<ProductCategoryDto> categories = entity.getCategories().stream()
                .map(ProductCategoryDto::fromEntity)
                .toList();
        return new ProductSubCategoryGroupDto(entity.getId(), entity.getGroupName(), categories);
    }
}
```

- [ ] **Step 6: Create `ProductCategoryGroupDto`**

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.ProductCategoryGroup;

import java.util.List;

public record ProductCategoryGroupDto(
        Long id,
        String groupName,
        List<ProductSubCategoryGroupDto> subGroups,
        List<ProductCategoryDto> categories) {

    public static ProductCategoryGroupDto fromEntity(ProductCategoryGroup entity) {
        List<ProductSubCategoryGroupDto> subGroups = entity.getSubGroups().stream()
                .map(ProductSubCategoryGroupDto::fromEntity)
                .toList();
        List<ProductCategoryDto> categories = entity.getCategories().stream()
                .map(ProductCategoryDto::fromEntity)
                .toList();
        return new ProductCategoryGroupDto(entity.getId(), entity.getGroupName(), subGroups, categories);
    }
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./gradlew test --tests "com.lavander.estore.dto.CatalogTreeDtoMappingTest"`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/lavander/estore/dto/PropertyDefinitionDto.java src/main/java/com/lavander/estore/dto/ProductCategoryDto.java src/main/java/com/lavander/estore/dto/ProductSubCategoryGroupDto.java src/main/java/com/lavander/estore/dto/ProductCategoryGroupDto.java src/test/java/com/lavander/estore/dto/CatalogTreeDtoMappingTest.java
git commit -m "Add catalog-tree DTOs with fromEntity mapping"
```

---

### Task 7: Product/Variant DTOs

**Files:**
- Create: `src/main/java/com/lavander/estore/dto/ProductCategoryRefDto.java`
- Create: `src/main/java/com/lavander/estore/dto/ProductDto.java`
- Create: `src/main/java/com/lavander/estore/dto/ProductRefDto.java`
- Create: `src/main/java/com/lavander/estore/dto/PropertyValueDto.java`
- Create: `src/main/java/com/lavander/estore/dto/ProductVariantDto.java`
- Test: `src/test/java/com/lavander/estore/dto/ProductVariantDtoMappingTest.java`

**Interfaces:**
- Consumes: `Product` (Task 4), `ProductCategory` (Task 3), `ProductVariant` (Task 5), `PropertyValue` (Task 5), `PropertyDefinitionDto` (Task 6)
- Produces: `ProductCategoryRefDto(Long id, String categoryName)`, `ProductDto(Long id, String productName, String productDescription, ProductCategoryRefDto category, List<PropertyDefinitionDto> extraProperties)`, `ProductRefDto(Long id, String productName)`, `PropertyValueDto(Long id, PropertyDefinitionDto propertyDefinition, String propertyValue)`, `ProductVariantDto(Long id, String variantName, String variantDescription, ProductRefDto product, List<PropertyValueDto> variantProperties, BigDecimal price, Integer starRating)` — each with static `fromEntity(...)`

- [ ] **Step 1: Write the failing test**

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.lavander.estore.dto.ProductVariantDtoMappingTest"`
Expected: FAIL — compile error, DTO classes don't exist yet.

- [ ] **Step 3: Create `ProductCategoryRefDto`**

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.ProductCategory;

public record ProductCategoryRefDto(Long id, String categoryName) {

    public static ProductCategoryRefDto fromEntity(ProductCategory entity) {
        return new ProductCategoryRefDto(entity.getId(), entity.getCategoryName());
    }
}
```

- [ ] **Step 4: Create `ProductDto`**

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.Product;

import java.util.List;

public record ProductDto(
        Long id,
        String productName,
        String productDescription,
        ProductCategoryRefDto category,
        List<PropertyDefinitionDto> extraProperties) {

    public static ProductDto fromEntity(Product entity) {
        List<PropertyDefinitionDto> extraProperties = entity.getExtraProperties().stream()
                .map(PropertyDefinitionDto::fromEntity)
                .toList();
        return new ProductDto(
                entity.getId(),
                entity.getProductName(),
                entity.getProductDescription(),
                ProductCategoryRefDto.fromEntity(entity.getProductCategory()),
                extraProperties);
    }
}
```

- [ ] **Step 5: Create `ProductRefDto`**

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.Product;

public record ProductRefDto(Long id, String productName) {

    public static ProductRefDto fromEntity(Product entity) {
        return new ProductRefDto(entity.getId(), entity.getProductName());
    }
}
```

- [ ] **Step 6: Create `PropertyValueDto`**

```java
package com.lavander.estore.dto;

import com.lavander.estore.model.PropertyValue;

public record PropertyValueDto(Long id, PropertyDefinitionDto propertyDefinition, String propertyValue) {

    public static PropertyValueDto fromEntity(PropertyValue entity) {
        return new PropertyValueDto(
                entity.getId(),
                PropertyDefinitionDto.fromEntity(entity.getPropertyDefinition()),
                entity.getPropertyValue());
    }
}
```

- [ ] **Step 7: Create `ProductVariantDto`**

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
        BigDecimal price,
        Integer starRating) {

    public static ProductVariantDto fromEntity(ProductVariant entity) {
        List<PropertyValueDto> variantProperties = entity.getVariantProperties().stream()
                .map(PropertyValueDto::fromEntity)
                .toList();
        return new ProductVariantDto(
                entity.getId(),
                entity.getVariantName(),
                entity.getVariantDescription(),
                ProductRefDto.fromEntity(entity.getProduct()),
                variantProperties,
                entity.getPrice(),
                entity.getStarRating());
    }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `./gradlew test --tests "com.lavander.estore.dto.ProductVariantDtoMappingTest"`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/lavander/estore/dto/ProductCategoryRefDto.java src/main/java/com/lavander/estore/dto/ProductDto.java src/main/java/com/lavander/estore/dto/ProductRefDto.java src/main/java/com/lavander/estore/dto/PropertyValueDto.java src/main/java/com/lavander/estore/dto/ProductVariantDto.java src/test/java/com/lavander/estore/dto/ProductVariantDtoMappingTest.java
git commit -m "Add Product/Variant DTOs with fromEntity mapping"
```

---

### Task 8: Full build verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew test`
Expected: All tests pass (7 repository tests across Tasks 1-5, 2 DTO mapping test classes from Tasks 6-7).

- [ ] **Step 2: Run a full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL
