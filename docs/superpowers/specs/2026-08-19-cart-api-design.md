# Shopping Cart API — Design

## Goal

Add a shopping cart: multiple items per cart, quantity updates, item removal.
Works for anonymous users via a server-issued cookie; the data model leaves
room for a future logged-in owner but does not implement login or cart
merging (there is no login system yet).

## Architecture

Two new entities, `Cart` and `CartItem`. A cart is identified by an
HTTP-only `cart_token` cookie holding a random UUID — the backend sets it on
the first cart-touching request that doesn't already have one, and resolves
the `Cart` row from it on every request after. All cart mutation endpoints
return the full, freshly-computed `CartDto` (not 204), so the frontend never
needs a follow-up GET — the same pattern the reviews feature already
established for `submitReview`.

## Data Model

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ownerToken;

    /** Unused until login exists. Reserved so the schema doesn't need to change later. */
    private Long userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public Cart(String ownerToken) {
        this.ownerToken = ownerToken;
    }
}
```

```java
@Entity
@Getter
@Setter
@NoArgsConstructor
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    private Integer quantity;

    public CartItem(Cart cart, ProductVariant variant, Integer quantity) {
        this.cart = cart;
        this.variant = variant;
        this.quantity = quantity;
    }
}
```

`Cart.items` cascades with `orphanRemoval = true` — cart items have no
meaning outside their cart, same reasoning already applied to
`ProductVariant.variantProperties` and (after this session's final-review
fix) `ProductVariant.reviews`. Removing an item from the collection and
saving the cart deletes the row on flush; this exact
mutate-collection-then-save-parent pattern is already proven in this
codebase by `ProductVariantRepositoryTest.removingVariantPropertyFromListDeletesItOnFlush`,
which passes today with no `@Transactional` anywhere, relying on
open-in-view keeping one persistence context per request.

**Lesson carried over from the reviews feature's final-review bug:**
`ProductVariant` does NOT get a `cartItems` collection (no read path needs
one — carts are read by cart, not by variant), but `ProductService.deleteVariant`
must still clean up any `CartItem` rows referencing the variant being
deleted, or deleting a variant that's sitting in someone's cart will hit the
same FK-violation-on-delete regression the review feature had. See the
Service section below.

## Repositories

```java
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByOwnerToken(String ownerToken);
}
```

```java
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void deleteByVariantId(Long variantId);
}
```

## DTOs

```java
public record CartDto(Long id, List<CartItemDto> items) {
    public static CartDto fromEntity(Cart entity) {
        List<CartItemDto> items = entity.getItems().stream().map(CartItemDto::fromEntity).toList();
        return new CartDto(entity.getId(), items);
    }
}
```

```java
public record CartItemDto(Long id, ProductVariantDto variant, Integer quantity) {
    public static CartItemDto fromEntity(CartItem entity) {
        return new CartItemDto(entity.getId(), ProductVariantDto.fromEntity(entity.getVariant()), entity.getQuantity());
    }
}
```

Reusing `ProductVariantDto` for the item's variant gives the frontend
everything it needs (name, price, tags, rating) in one response, matching
how other endpoints in this codebase already nest full DTOs rather than
thin refs where the consumer needs the detail.

```java
public record AddCartItemRequest(@NotNull Long variantId, @NotNull @Min(1) Integer quantity) {
}

public record UpdateCartItemRequest(@NotNull @Min(1) Integer quantity) {
}
```

Quantity is never set to zero via the update endpoint — removing an item is
`DELETE /api/cart/items/{itemId}`, not `PUT ... {quantity: 0}`.

## Service

```java
@Service
public class CartService {

    private static final String CART_COOKIE_NAME = "cart_token";
    private static final int CART_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 365;

    private final CartRepository cartRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartService(CartRepository cartRepository, ProductVariantRepository productVariantRepository) {
        this.cartRepository = cartRepository;
        this.productVariantRepository = productVariantRepository;
    }

    public Cart resolveCart(String cartToken, HttpServletResponse response) {
        if (cartToken != null) {
            Optional<Cart> existing = cartRepository.findByOwnerToken(cartToken);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        String newToken = UUID.randomUUID().toString();
        Cart cart = cartRepository.save(new Cart(newToken));
        setCartCookie(response, newToken);
        return cart;
    }

    public CartDto getCart(String cartToken, HttpServletResponse response) {
        return CartDto.fromEntity(resolveCart(cartToken, response));
    }

    public CartDto addItem(String cartToken, HttpServletResponse response, AddCartItemRequest request) {
        Cart cart = resolveCart(cartToken, response);
        ProductVariant variant = productVariantRepository.findById(request.variantId())
                .orElseThrow(() -> new NotFoundException("Product variant not found with id: " + request.variantId()));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getVariant().getId().equals(variant.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.quantity());
        } else {
            cart.getItems().add(new CartItem(cart, variant, request.quantity()));
        }
        return CartDto.fromEntity(cartRepository.save(cart));
    }

    public CartDto updateItemQuantity(String cartToken, HttpServletResponse response, Long itemId, UpdateCartItemRequest request) {
        Cart cart = resolveCart(cartToken, response);
        findItemInCart(cart, itemId).setQuantity(request.quantity());
        return CartDto.fromEntity(cartRepository.save(cart));
    }

    public CartDto removeItem(String cartToken, HttpServletResponse response, Long itemId) {
        Cart cart = resolveCart(cartToken, response);
        cart.getItems().remove(findItemInCart(cart, itemId));
        return CartDto.fromEntity(cartRepository.save(cart));
    }

    private CartItem findItemInCart(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Cart item not found with id: " + itemId));
    }

    private void setCartCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(CART_COOKIE_NAME, token)
                .httpOnly(true)
                .path("/")
                .maxAge(CART_COOKIE_MAX_AGE_SECONDS)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
```

`SameSite=Lax` (no `Secure`) is enough here: `localhost:4200` and
`localhost:8080` are different origins (different ports) but the same site
(same scheme + registrable domain), which is all `SameSite` cares about.
`SameSite=None; Secure` would only be needed if the two apps were ever
deployed to genuinely different domains — out of scope for now (YAGNI, no
such deployment exists).

**`ProductService.deleteVariant` gains a cleanup step** (the cascade lesson
from above) — `ProductService` gains a `CartItemRepository` dependency:

```java
public void deleteVariant(Long id) {
    ProductVariant variant = findVariantById(id);
    cartItemRepository.deleteByVariantId(id);
    productVariantRepository.delete(variant);
}
```

## Controller

```java
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<CartDto> getCart(
            @CookieValue(value = "cart_token", required = false) String cartToken,
            HttpServletResponse response) {
        return ResponseEntity.ok(cartService.getCart(cartToken, response));
    }

    @PostMapping("/items")
    public ResponseEntity<CartDto> addItem(
            @CookieValue(value = "cart_token", required = false) String cartToken,
            HttpServletResponse response,
            @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(cartToken, response, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartDto> updateItemQuantity(
            @CookieValue(value = "cart_token", required = false) String cartToken,
            HttpServletResponse response,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(cartToken, response, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartDto> removeItem(
            @CookieValue(value = "cart_token", required = false) String cartToken,
            HttpServletResponse response,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(cartToken, response, itemId));
    }
}
```

## CORS

`WebConfig` currently allows all origins with no credentials. Cookies
require an explicit origin once credentials are involved:

```java
@Override
public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/**")
            .allowedOrigins("http://localhost:4200")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowCredentials(true);
}
```

This hardcodes the dev origin — matches how the frontend already hardcodes
`environment.backendUrl`; this app has no other deployment target yet.

## Testing

Following this codebase's established convention (repository/DTO-mapping
tests, plus a service-level test where an ORM subtlety is involved, no
mocks):

- A `@DataJpaTest` repository test proving: adding two different variants to
  a cart creates two items; adding the same variant twice increments
  quantity rather than duplicating a row; removing an item via
  `cart.getItems().remove(...)` + save deletes the row on flush (mirroring
  `removingVariantPropertyFromListDeletesItOnFlush`).
- A DTO-mapping test for `CartDto`/`CartItemDto.fromEntity`.
- A `CartService`-level test (mirroring `ProductServiceReviewTest`) proving
  `resolveCart` creates a new cart and sets the cookie on the first call
  with no token, and returns the same cart on a subsequent call with the
  token it issued.
- A repository test proving `ProductService.deleteVariant` on a variant with
  an existing cart item does not throw and removes the cart item (mirroring
  `deletingVariantWithReviewsCascadesAndDoesNotThrow`).
