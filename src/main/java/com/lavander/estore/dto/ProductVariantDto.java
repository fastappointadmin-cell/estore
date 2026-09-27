package com.lavander.estore.dto;

import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.Review;
import com.lavander.estore.model.VariantImage;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

public record ProductVariantDto(
        Long id,
        String variantName,
        String variantDescription,
        ProductRefDto product,
        List<PropertyValueDto> variantProperties,
        List<TagDto> tags,
        List<VariantImageDto> images,
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
        List<VariantImageDto> images = entity.getImages().stream()
                .sorted(Comparator.comparing(VariantImage::getDisplayOrder))
                .map(VariantImageDto::fromEntity)
                .toList();
        return new ProductVariantDto(
                entity.getId(),
                entity.getVariantName(),
                entity.getVariantDescription(),
                ProductRefDto.fromEntity(entity.getProduct()),
                variantProperties,
                tags,
                images,
                entity.getPrice(),
                averageRating,
                reviews.size());
    }
}
