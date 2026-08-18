package com.lavander.estore.dto;

import com.lavander.estore.model.Product;

import java.util.List;

public record ProductDto(
        Long id,
        String productName,
        String productDescription,
        ProductCategoryRefDto category,
        List<PropertyDefinitionDto> extraProperties) {

    public static ProductDto fromEntity(Product entity) {
        List<PropertyDefinitionDto> extraProperties = entity.getExtraProperties().stream()
                .map(PropertyDefinitionDto::fromEntity)
                .toList();
        return new ProductDto(
                entity.getId(),
                entity.getProductName(),
                entity.getProductDescription(),
                ProductCategoryRefDto.fromEntity(entity.getProductCategory()),
                extraProperties);
    }
}
