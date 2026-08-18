# Admin CRUD API

Date: 2026-08-18

## Context

`lavander` is getting an admin page for managing the catalog (groups,
subgroups, categories, property definitions, products, variants). The
backend (`estore`) currently only exposes read endpoints
(`ProductController`, `ProductCategoryController`) — no create, update, or
delete anywhere. This is sub-project 1 of that admin feature: the backend
CRUD API the admin UI (sub-project 2, a separate spec) will be built
against.

No authentication exists anywhere in the app and none is added here — the
admin endpoints are unprotected, matching the rest of the API.

## Scope: 6 entities, not 7

`PropertyDefinition`, `ProductCategoryGroup`, `ProductSubCategoryGroup`,
`ProductCategory`, `Product`, `ProductVariant` each get full CRUD.
`PropertyValue` does not get its own endpoints — it's already modeled as
an owned collection of `ProductVariant` (`cascade = ALL,
orphanRemoval = true`), so it's managed as part of a variant's
create/update request body, not as an independent resource.

## Request DTOs

Package: `com.lavander.estore.dto` (alongside the existing response
DTOs). One shape per entity, reused for both `POST` (create) and `PUT`
(update) — no separate Create/Update types. Bean validation
(`spring-boot-starter-validation`, newly added) on required fields.

```java
record PropertyDefinitionRequest(@NotBlank String propertyName)

record ProductCategoryGroupRequest(@NotBlank String groupName)

record ProductSubCategoryGroupRequest(@NotBlank String groupName, @NotNull Long parentGroupId)

record ProductCategoryRequest(
    @NotBlank String categoryName,
    Long parentGroupId,
    Long parentSubGroupId,
    List<Long> categoryPropertyIds)   // nullable/empty allowed; exactly one of parentGroupId/parentSubGroupId enforced in the service

record ProductRequest(
    @NotBlank String productName,
    String productDescription,
    @NotNull Long categoryId,
    List<Long> extraPropertyIds)

record ProductVariantRequest(
    @NotBlank String variantName,
    String variantDescription,
    @NotNull Long productId,
    @NotNull BigDecimal price,
    @NotNull Integer starRating,
    List<PropertyValueInput> variantProperties)

record PropertyValueInput(@NotNull Long propertyDefinitionId, @NotBlank String value)
```

The exactly-one-of-parentGroupId/parentSubGroupId rule for
`ProductCategoryRequest` is a cross-field check bean validation can't
express cleanly, so it's checked explicitly in
`ProductCategoryService`, raising a validation-style 400 (via
`IllegalArgumentException`, mapped by the global handler) rather than
relying on the entity's existing `@PrePersist`/`@PreUpdate` check (which
still stays as the last-resort DB-level guard it already is).

## Endpoints

Existing base paths are kept; groups/subgroups continue to live under
`/api/product-categories/groups` and `/api/product-categories/subgroups`
per the current controller's convention.

```
GET    /api/property-definitions            list all
GET    /api/property-definitions/{id}
POST   /api/property-definitions
PUT    /api/property-definitions/{id}
DELETE /api/property-definitions/{id}

GET    /api/product-categories/groups/{id}
POST   /api/product-categories/groups
PUT    /api/product-categories/groups/{id}
DELETE /api/product-categories/groups/{id}
       (GET /api/product-categories/groups — list — already exists)

GET    /api/product-categories/subgroups/{id}
POST   /api/product-categories/subgroups
PUT    /api/product-categories/subgroups/{id}
DELETE /api/product-categories/subgroups/{id}

POST   /api/product-categories
PUT    /api/product-categories/{id}
DELETE /api/product-categories/{id}
       (GET /api/product-categories/{id} already exists)

GET    /api/products/{id}
POST   /api/products
PUT    /api/products/{id}
DELETE /api/products/{id}
       (GET /api/products/category/{categoryId} already exists)

GET    /api/products/variants/{id}
POST   /api/products/variants
PUT    /api/products/variants/{id}
DELETE /api/products/variants/{id}
       (GET /api/products/{productId}/variants already exists)
```

All new endpoints return the same response DTOs the existing GET
endpoints already use (`ProductCategoryGroupDto`, `ProductDto`,
`ProductVariantDto`, etc.) via each entity's existing `fromEntity`.

## Delete safety

Deleting an entity still referenced elsewhere fails with `409 Conflict`
and a message naming what's referencing it, rather than a raw
`DataIntegrityViolationException` from an FK constraint (none of the
parent→child relationships cascade-delete — that's deliberate, from the
original data-model design). Checked via new repository existence-check
methods:

- Group: blocked if it has subgroups or categories
  (`existsByParentGroupId` on both `ProductSubCategoryGroupRepository`
  and `ProductCategoryRepository`).
- Subgroup: blocked if it has categories
  (`existsByParentSubGroupId` on `ProductCategoryRepository`).
- Category: blocked if it has products (`existsByProductCategoryId` on
  `ProductRepository`).
- Product: blocked if it has variants (`existsByProductId` on
  `ProductVariantRepository`).
- PropertyDefinition: blocked if referenced by any category's
  `categoryProperties`, any product's `extraProperties`, or any
  `PropertyValue` (three existence checks against the join
  tables/`PropertyValueRepository`).
- Variant: never blocked — its `PropertyValue`s cascade-delete
  automatically (already configured), nothing else references a variant.

## Error handling

New package `com.lavander.estore.exception`:

- `NotFoundException` (unchecked) — existing services' raw
  `RuntimeException("... not found ...")` throws are switched to this for
  consistency; today those 500, which is a real gap once a UI needs to
  distinguish "not found" from a server error.
- `ConflictException` (unchecked) — the delete-guard case above.

New `GlobalExceptionHandler` (`@RestControllerAdvice`):

```
NotFoundException              -> 404 { message }
ConflictException              -> 409 { message }
MethodArgumentNotValidException -> 400 { fieldErrors: { field: message } }
IllegalArgumentException       -> 400 { message }
```

## Out of scope

- Authentication/authorization.
- Pagination/sorting on list endpoints (lists stay small for this
  dataset).
- Bulk operations.
- The admin UI itself (sub-project 2, separate spec, built against this
  API once it exists).
