package com.lavander.estore.service;

import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.exception.UnauthorizedException;
import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.Role;
import com.lavander.estore.model.User;
import com.lavander.estore.repository.FavoriteRepository;
import com.lavander.estore.repository.ProductCategoryGroupRepository;
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FavoriteServiceTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository productCategoryRepository;

    @Autowired
    private ProductCategoryGroupRepository productCategoryGroupRepository;

    @Autowired
    private UserRepository userRepository;

    private FavoriteService newFavoriteService() {
        return new FavoriteService(favoriteRepository, productVariantRepository, userRepository);
    }

    private ProductVariant createVariant(String name, String price) {
        ProductCategoryGroup electronics = productCategoryGroupRepository.save(new ProductCategoryGroup("Electronics"));
        ProductCategory laptops = new ProductCategory("Laptops");
        laptops.setParentGroup(electronics);
        productCategoryRepository.save(laptops);
        Product dell = productRepository.save(new Product("Dell", "Dell laptops", laptops));

        ProductVariant variant = new ProductVariant(name, "A laptop", dell, new BigDecimal(price));
        productVariantRepository.save(variant);
        entityManager.flush();
        return variant;
    }

    private String createUser() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        userRepository.save(new User(email, "hash", "Test User", Role.USER));
        entityManager.flush();
        return email;
    }

    @Test
    void addFavoriteThenListReturnsIt() {
        FavoriteService service = newFavoriteService();
        String email = createUser();
        ProductVariant xps13 = createVariant("Dell XPS 13", "4999.00");

        service.addFavorite(email, xps13.getId());
        List<ProductVariantDto> favorites = service.getMyFavorites(email);

        assertThat(favorites).hasSize(1);
        assertThat(favorites.get(0).id()).isEqualTo(xps13.getId());
    }

    @Test
    void addFavoriteTwiceDoesNotDuplicate() {
        FavoriteService service = newFavoriteService();
        String email = createUser();
        ProductVariant xps13 = createVariant("Dell XPS 13", "4999.00");

        service.addFavorite(email, xps13.getId());
        service.addFavorite(email, xps13.getId());

        assertThat(service.getMyFavorites(email)).hasSize(1);
    }

    @Test
    void removeFavoriteDeletesIt() {
        FavoriteService service = newFavoriteService();
        String email = createUser();
        ProductVariant xps13 = createVariant("Dell XPS 13", "4999.00");
        service.addFavorite(email, xps13.getId());

        service.removeFavorite(email, xps13.getId());

        assertThat(service.getMyFavorites(email)).isEmpty();
    }

    @Test
    void removingANonFavoritedVariantDoesNotThrow() {
        FavoriteService service = newFavoriteService();
        String email = createUser();
        ProductVariant xps13 = createVariant("Dell XPS 13", "4999.00");

        service.removeFavorite(email, xps13.getId());

        assertThat(service.getMyFavorites(email)).isEmpty();
    }

    @Test
    void favoritesAreIsolatedPerUser() {
        FavoriteService service = newFavoriteService();
        String emailA = createUser();
        String emailB = createUser();
        ProductVariant xps13 = createVariant("Dell XPS 13", "4999.00");

        service.addFavorite(emailA, xps13.getId());

        assertThat(service.getMyFavorites(emailA)).hasSize(1);
        assertThat(service.getMyFavorites(emailB)).isEmpty();
    }

    @Test
    void addFavoriteWithUnknownVariantThrowsNotFound() {
        FavoriteService service = newFavoriteService();
        String email = createUser();

        assertThatThrownBy(() -> service.addFavorite(email, 999999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getMyFavoritesWithNoLoggedInUserThrowsUnauthorized() {
        FavoriteService service = newFavoriteService();

        assertThatThrownBy(() -> service.getMyFavorites(null))
                .isInstanceOf(UnauthorizedException.class);
    }
}
