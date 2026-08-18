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
        List<TagDto> tags,
        BigDecimal price,
        Integer starRating) {

    public static ProductVariantDto fromEntity(ProductVariant entity) {
        List<PropertyValueDto> variantProperties = entity.getVariantProperties().stream()
                .map(PropertyValueDto::fromEntity)
                .toList();
        List<TagDto> tags = entity.getTags().stream().map(TagDto::fromEntity).toList();
        return new ProductVariantDto(
                entity.getId(),
                entity.getVariantName(),
                entity.getVariantDescription(),
                ProductRefDto.fromEntity(entity.getProduct()),
                variantProperties,
                tags,
                entity.getPrice(),
                entity.getStarRating());
    }
}
