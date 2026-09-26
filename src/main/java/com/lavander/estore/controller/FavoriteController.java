package com.lavander.estore.controller;

import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ResponseEntity<List<ProductVariantDto>> getMyFavorites(Authentication authentication) {
        return ResponseEntity.ok(favoriteService.getMyFavorites(resolveUserEmail(authentication)));
    }

    @PostMapping("/{variantId}")
    public ResponseEntity<Void> addFavorite(Authentication authentication, @PathVariable Long variantId) {
        favoriteService.addFavorite(resolveUserEmail(authentication), variantId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<Void> removeFavorite(Authentication authentication, @PathVariable Long variantId) {
        favoriteService.removeFavorite(resolveUserEmail(authentication), variantId);
        return ResponseEntity.noContent().build();
    }

    private String resolveUserEmail(Authentication authentication) {
        if (authentication instanceof UsernamePasswordAuthenticationToken && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
}
