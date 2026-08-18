# Catalog Models: Entities, DTOs, Repositories

Date: 2026-08-18

## Context

The `lavander` Angular frontend defines the product catalog domain in
`src/app/models/models.ts`: `PropertyDefinition`, `ProductCategoryGroup`,
`ProductSubCategoryGroup`, `ProductCategory`, `Product`, `PropertyValue`,
`ProductVariant`. The `estore` Spring Boot backend currently has no
persistence layer — this spec defines the JPA entities, DTOs, and
repositories needed to represent that same domain in the backend, backed by
the already-configured Postgres database (`application.yaml` sets
`spring.jpa.hibernate.ddl-auto: update` but the project has no JPA
dependency yet).

Scope: entities, DTOs, and repositories only. No services or REST
controllers in this pass.

## Entity model

Package: `com.lavander.estore.model`

All entities use `Long` IDs with `GenerationType.IDENTITY`.

### Group hierarchy

The frontend's `ProductCategory.parentGroup` is a union type
(`ProductCategoryGroup | ProductSubCategoryGroup`). Relational FKs can't be
polymorphic, so `ProductCategory` gets two nullable FKs, exactly one of
which must be set — enforced with a `@PrePersist`/`@PreUpdate` check in the
entity (no native "exactly one of" JPA constraint, and a raw DB CHECK
constraint could be bypassed silently by JPA).

```
ProductCategoryGroup                  (top-level only, no parent)
  id, groupName
  subGroups:  List<ProductSubCategoryGroup>  (@OneToMany mappedBy="parentGroup")
  categories: List<ProductCategory>          (@OneToMany mappedBy="parentGroup")

ProductSubCategoryGroup
  id, groupName
  parentGroup -> ProductCategoryGroup   (@ManyToOne, not null)
  categories: List<ProductCategory>     (@OneToMany mappedBy="parentSubGroup")

ProductCategory
  id, categoryName
  parentGroup    -> ProductCategoryGroup     (@ManyToOne, nullable)
  parentSubGroup -> ProductSubCategoryGroup  (@ManyToOne, nullable)
  categoryProperties: List<PropertyDefinition>  (@ManyToMany, join table category_properties)
```

The `@OneToMany` sides (`subGroups`, `categories` on the group/subgroup) are
`mappedBy` — non-owning, no extra columns, no cascade. Deleting a
category/subgroup is done directly, not by removing it from its parent's
list; adding/removing children happens by setting the child's own parent
FK.

### Shared PropertyDefinition

`PropertyDefinition` (e.g. "RAM", "Screen Size") is reused across owners in
the frontend mock data — the same definition is referenced from a
category's property list, a product's extra-property list, and a variant's
`PropertyValue`. Modeled as a shared entity with many-to-many links:

```
PropertyDefinition
  id, propertyName

ProductCategory.categoryProperties  <--@ManyToMany--> PropertyDefinition   (join table category_properties)
Product.extraProperties             <--@ManyToMany--> PropertyDefinition   (join table product_extra_properties)
PropertyValue.propertyDefinition    <--@ManyToOne -->  PropertyDefinition
```

### Product / Variant

```
Product
  id, productName, productDescription
  productCategory -> ProductCategory   (@ManyToOne, not null)
  extraProperties -> PropertyDefinition (@ManyToMany, as above)

ProductVariant
  id, variantName, variantDescription
  product -> Product   (@ManyToOne, not null)
  variantProperties: List<PropertyValue>  (@OneToMany mappedBy="variant", cascade=ALL, orphanRemoval=true)
  price: BigDecimal
  starRating: Integer

PropertyValue
  id
  variant -> ProductVariant   (@ManyToOne, owning side)
  propertyDefinition -> PropertyDefinition   (@ManyToOne)
  propertyValue: String
```

`PropertyValue` is owned by its `ProductVariant` (cascade all, orphan
removal) since it has no meaning outside its variant.

## DTOs

Package: `com.lavander.estore.dto`

DTOs cross a JSON boundary, unlike the frontend's in-memory TS objects, so
they can't mirror the frontend's bidirectional nesting directly — a
`ProductCategoryDto` nested inside its group and also holding a back-link
to that group would serialize forever. Parent-facing DTOs nest their
children (group → subgroups → categories); child→parent links are dropped
from the DTO. Cross-aggregate references (`Product` → `Category`,
`Variant` → `Product`) use a lightweight ref DTO (id + name only) instead
of embedding the full nested object graph.

```
PropertyDefinitionDto        { id, propertyName }

ProductCategoryGroupDto      { id, groupName,
                                subGroups: List<ProductSubCategoryGroupDto>,
                                categories: List<ProductCategoryDto> }

ProductSubCategoryGroupDto   { id, groupName,
                                categories: List<ProductCategoryDto> }

ProductCategoryDto           { id, categoryName,
                                categoryProperties: List<PropertyDefinitionDto> }

ProductCategoryRefDto        { id, categoryName }   -- used inside ProductDto

ProductDto                   { id, productName, productDescription,
                                category: ProductCategoryRefDto,
                                extraProperties: List<PropertyDefinitionDto> }

ProductRefDto                { id, productName }    -- used inside ProductVariantDto

PropertyValueDto             { id, propertyDefinition: PropertyDefinitionDto, propertyValue }

ProductVariantDto            { id, variantName, variantDescription,
                                product: ProductRefDto,
                                variantProperties: List<PropertyValueDto>,
                                price, starRating }
```

Each DTO gets a static `fromEntity(...)` mapping method (manual mapping,
no MapStruct). No bean-validation annotations on DTOs for this pass.

## Repositories

Package: `com.lavander.estore.repository`

One `interface XRepository extends JpaRepository<X, Long>` per entity, no
custom query methods:

- `PropertyDefinitionRepository`
- `ProductCategoryGroupRepository`
- `ProductSubCategoryGroupRepository`
- `ProductCategoryRepository`
- `ProductRepository`
- `ProductVariantRepository`
- `PropertyValueRepository`

## Build changes

`build.gradle` has no JPA dependency yet even though `application.yaml`
already configures Hibernate/Postgres. Add:

```
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
```

No other build changes.
