package com.lavander.estore.service;

import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.exception.UnauthorizedException;
import com.lavander.estore.model.Favorite;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.User;
import com.lavander.estore.repository.FavoriteRepository;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public FavoriteService(
            FavoriteRepository favoriteRepository,
            ProductVariantRepository productVariantRepository,
            UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository;
        this.productVariantRepository = productVariantRepository;
        this.userRepository = userRepository;
    }

    public List<ProductVariantDto> getMyFavorites(String userEmail) {
        Long userId = requireUserId(userEmail);
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(favorite -> ProductVariantDto.fromEntity(favorite.getVariant()))
                .toList();
    }

    /** Idempotent — favoriting an already-favorited variant is a no-op, not a conflict. */
    public void addFavorite(String userEmail, Long variantId) {
        Long userId = requireUserId(userEmail);
        if (favoriteRepository.findByUserIdAndVariantId(userId, variantId).isPresent()) {
            return;
        }
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new NotFoundException("Product variant not found with id: " + variantId));
        favoriteRepository.save(new Favorite(userId, variant));
    }

    /** Idempotent — removing a variant that isn't favorited is a no-op, not an error. */
    public void removeFavorite(String userEmail, Long variantId) {
        Long userId = requireUserId(userEmail);
        favoriteRepository.findByUserIdAndVariantId(userId, variantId)
                .ifPresent(favoriteRepository::delete);
    }

    private Long requireUserId(String userEmail) {
        if (userEmail == null) {
            throw new UnauthorizedException("Must be logged in to use favorites");
        }
        return userRepository.findByEmail(userEmail)
                .map(User::getId)
                .orElseThrow(() -> new UnauthorizedException("Must be logged in to use favorites"));
    }
}
