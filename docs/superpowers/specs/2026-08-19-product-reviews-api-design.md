# Product Reviews API — Design

## Goal

Replace the admin-typed, static `ProductVariant.starRating` with a real customer
review mechanism: anyone can submit a 1-5 star rating for a variant, and the
displayed rating becomes the computed average of all submitted ratings.

## Architecture

A new `Review` entity (`variant`, `rating` — no text, no timestamp, per the
approved design) is the only new persisted concept. `ProductVariant` drops its
`starRating` column entirely and gains a `reviews` collection; `ProductVariantDto`
computes the average (and a count) from that collection on every read, so there is
no denormalized/cached rating to keep in sync. Submitting a review is a single new
endpoint that persists the `Review` and returns the freshly-computed
`ProductVariantDto`, so the frontend needs no follow-up GET to reflect the new
average.

## Data Model

```java
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

`ProductVariant` changes: the `starRating` field and its constructor parameter are
removed; a `reviews` collection is added (plain `@OneToMany(mappedBy = "variant")`,
no cascade — reviews are persisted directly via `ReviewRepository`, not through the
variant's own save, so cascading isn't needed):

```java
@OneToMany(mappedBy = "variant")
private List<Review> reviews = new ArrayList<>();

public ProductVariant(String variantName, String variantDescription, Product product, BigDecimal price) {
    this.variantName = variantName;
    this.variantDescription = variantDescription;
    this.product = product;
    this.price = price;
}
```

## DTOs

`ProductVariantDto.starRating` changes from `Integer` to `Double` (a computed
average is naturally fractional) and gains `reviewCount`:

```java
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

`ProductVariantRequest` drops `starRating` entirely — it's no longer a settable
field at create or update time:

```java
public record ProductVariantRequest(
        @NotBlank String variantName,
        String variantDescription,
        @NotNull Long productId,
        @NotNull BigDecimal price,
        @Valid List<PropertyValueInput> variantProperties,
        List<Long> tagIds) {
}
```

New `ReviewRequest`:

```java
public record ReviewRequest(@NotNull @Min(1) @Max(5) Integer rating) {
}
```

## Repository

```java
public interface ReviewRepository extends JpaRepository<Review, Long> {
}
```

No derived finders needed — the service saves directly and reflects the new row
into the already-loaded variant in memory (see below), and reads always go through
`ProductVariant.getReviews()`.

## Service

`ProductService` gains a `ReviewRepository` dependency and one new method. The key
subtlety: `reviews` is the *inverse* side of the relationship (`Review` owns the FK),
so saving a new `Review` doesn't automatically update an already-loaded
`variant.getReviews()` list in the same persistence context — it's appended
manually so the DTO built right after reflects it without a second query:

```java
public ProductVariantDto submitReview(Long variantId, ReviewRequest request) {
    ProductVariant variant = findVariantById(variantId);
    Review review = reviewRepository.save(new Review(variant, request.rating()));
    variant.getReviews().add(review);
    return ProductVariantDto.fromEntity(variant);
}
```

`createVariant`/`updateVariant` drop their `request.starRating()` /
`entity.setStarRating(...)` calls (the field no longer exists on the request or the
entity) — otherwise unchanged.

## Controller

```java
@PostMapping("/variants/{id}/reviews")
public ResponseEntity<ProductVariantDto> submitReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
    return ResponseEntity.ok(productService.submitReview(id, request));
}
```

Added to the existing `ProductController` (`/api/products/variants/{id}/reviews`),
alongside the other variant endpoints. No auth, no rate limiting, no dedup — matches
the approved "unlimited, no tracking" scope; this app has no auth system anywhere
else to hook into.

## Seed Data

`scripts/seed-catalog-data.sql`: the `star_rating` column drops out of the
`product_variant` INSERT (the column itself will remain in Postgres since
Hibernate's `ddl-auto: update` doesn't drop columns, but nothing writes to it or
reads from it anymore — an inert leftover, acceptable for a demo app). One `review`
row is seeded per variant, using that variant's old static rating as the single
review's rating, so the site's initial visual state (stars shown per card) is
unchanged even though the mechanism behind it is now real:

```sql
INSERT INTO review (id, variant_id, rating) OVERRIDING SYSTEM VALUE VALUES
  (1, 1, 4), (2, 2, 5), (3, 3, 4), (4, 4, 4), (5, 5, 5), (6, 6, 4),
  (7, 7, 5), (8, 8, 3), (9, 9, 4), (10, 10, 5), (11, 11, 4), (12, 12, 5);
```

`review` is added to the leading `TRUNCATE TABLE` list, and a `setval` line is
added for its sequence, following the file's existing pattern for every other
table.

## Testing

Following this codebase's established convention (repository/DTO-mapping tests,
not mocked service-layer tests):

- A DTO-mapping test proving `ProductVariantDto.fromEntity` computes the average
  and count correctly from a variant with multiple reviews (e.g. ratings `[3, 5]`
  → `starRating = 4.0`, `reviewCount = 2`), and that a variant with zero reviews
  maps to `starRating = 0.0`, `reviewCount = 0`.
- A repository test (or extending the existing `ProductVariantRepositoryTest`)
  proving a variant saved via `ProductService.submitReview` — or the equivalent
  repository-level save-then-append sequence — round-trips correctly: reload the
  variant fresh from the repository afterward and confirm its review count/average
  reflects the submission, not just the in-memory object from the same call.
