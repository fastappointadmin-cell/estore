# Promotion Tags API — Design

## Goal

Let a variant appear in multiple cross-cutting promotional listings (e.g. "Promoție
Primăvară", "Produse sub 10 Lei") without changing its category and without
duplicating product/variant data. Categories stay a strict one-parent tree (they also
define shared attributes); promotions are a separate, additive concept.

## Architecture

Two new entities, independent of the category hierarchy:

- `Tag` — a merchandising label (`tagName`), attachable to any number of variants.
- `PromotionGroup` — a named collection of tags. Its listing is computed on read: every
  variant that has **at least one** of the group's tags (OR match) — confirmed with the
  user, since a group like "Primăvara" pools spring-tagged items and under-10-lei items
  together rather than intersecting them.

`ProductVariant` gains a many-to-many `tags` relation, following the exact pattern
`Product.extraProperties` already uses for its many-to-many to `PropertyDefinition`.

## Data Model

```java
@Entity
public class Tag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tagName;
}

@Entity
public class PromotionGroup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String groupName;

    @ManyToMany
    @JoinTable(
        name = "promotion_group_tag",
        joinColumns = @JoinColumn(name = "promotion_group_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();
}
```

`ProductVariant` gains:

```java
@ManyToMany
@JoinTable(
    name = "variant_tag",
    joinColumns = @JoinColumn(name = "variant_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id"))
private Set<Tag> tags = new HashSet<>();
```

`Tag` itself carries no back-reference fields — same as `PropertyDefinition`, which is
referenced by three different owning sides without knowing about any of them.

**`ProductRefDto` gains `categoryId`.** This is the one change to an existing DTO. It's
needed so the frontend, when showing a variant inside a promotion listing (which pools
variants from unrelated categories), can still route to that variant's real
category-based product-detail page — see the UI spec for how this is used.
`ProductRefDto` is only referenced from `ProductVariantDto`, so this is a safe additive
change.

## DTOs

```java
public record TagDto(Long id, String tagName) {
    public static TagDto fromEntity(Tag entity) { ... }
}
public record TagRequest(@NotBlank String tagName) {}

public record PromotionGroupDto(Long id, String groupName, List<TagDto> tags) {
    public static PromotionGroupDto fromEntity(PromotionGroup entity) { ... }
}
public record PromotionGroupRequest(@NotBlank String groupName, List<Long> tagIds) {}
```

`ProductVariantDto` gains `List<TagDto> tags`. `ProductVariantRequest` gains
`List<Long> tagIds` (nullable/empty = no tags — same convention `extraPropertyIds`
already uses). `ProductRefDto` gains `Long categoryId`, populated from
`entity.getProductCategory().getId()`.

## Repositories

```java
public interface TagRepository extends JpaRepository<Tag, Long> {
}

public interface PromotionGroupRepository extends JpaRepository<PromotionGroup, Long> {
    boolean existsByTagsId(Long tagId);
}
```

`ProductVariantRepository` gains:

```java
boolean existsByTagsId(Long tagId);
List<ProductVariant> findDistinctByTagsIn(Collection<Tag> tags);
```

`findDistinctByTagsIn` is the OR-match query: Spring Data joins on the `tags`
collection and matches any variant whose tag set intersects the given collection;
`Distinct` collapses a variant that matches more than one tag in the group down to one
row.

## Services

**`TagService`** — `getAll`, `getById`, `create`, `update`, `delete`. Mirrors
`PropertyDefinitionService` exactly, including the delete guard:

```java
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
```

**`PromotionGroupService`** — `getAll`, `getById`, `create`, `update`, `delete` (no
delete guard needed: removing a group only clears rows in its own join table, nothing
becomes orphaned), plus:

```java
public List<ProductVariantDto> getVariantsForGroup(Long id) {
    PromotionGroup group = findGroupById(id);
    return productVariantRepository.findDistinctByTagsIn(group.getTags()).stream()
            .map(ProductVariantDto::fromEntity)
            .toList();
}
```

`create`/`update` resolve `tagIds` the same way `ProductService.resolvePropertyDefinitions`
resolves property ids: `new HashSet<>(tagRepository.findAllById(safeIds))`.

**`ProductService`** gains a private `applyVariantTags(ProductVariant variant, List<Long> tagIds)`,
mirroring `resolvePropertyDefinitions`, called from both `createVariant` and
`updateVariant` alongside the existing `applyVariantProperties` call.

## Controllers

```java
@RestController
@RequestMapping("/api/tags")
public class TagController {
    // GET (list), GET/{id}, POST, PUT/{id}, DELETE/{id} — same shape as PropertyDefinitionController
}

@RestController
@RequestMapping("/api/promotion-groups")
public class PromotionGroupController {
    // GET (list), GET/{id}, POST, PUT/{id}, DELETE/{id}
    @GetMapping("/{id}/variants")
    public ResponseEntity<List<ProductVariantDto>> getVariants(@PathVariable Long id) {
        return ResponseEntity.ok(promotionGroupService.getVariantsForGroup(id));
    }
}
```

`ProductController`'s existing variant endpoints are unchanged in shape — the request/
response records simply gained fields.

Validation and error handling reuse the existing `GlobalExceptionHandler`
(`NotFoundException` → 404, `ConflictException` → 409); no new exception types.

## Seed Data

Extend `scripts/seed-catalog-data.sql` with:
- A handful of tags, including at least one that's genuinely cross-category to prove
  the point of this feature — e.g. `"Produs sub 20 Lei"` tagged onto **Domestos Pine
  Fresh** (15 Lei, Curățenie) and **Alint Hărtie Igienică Piersică** (10 Lei, Igienă),
  two unrelated categories.
- One `PromotionGroup` ("Produse sub 20 Lei") referencing that tag, so the feature is
  visibly testable end-to-end after seeding.

## Testing

This codebase tests at the repository/DTO-mapping layer, not with mocked service unit
tests (see `ProductVariantRepositoryTest`, `PropertyDefinitionRepositoryTest`,
`ProductVariantDtoMappingTest`) — follow that convention rather than introducing a new
testing style:

- `ProductVariantRepositoryTest` gains a test for `findDistinctByTagsIn`: create a
  variant tagged with two tags that both belong to the same promotion group's tag set,
  assert it's returned exactly once (proves `Distinct` is doing its job).
- `ProductVariantDtoMappingTest` gains an assertion that `tags` round-trips through
  `ProductVariantDto.fromEntity`.
