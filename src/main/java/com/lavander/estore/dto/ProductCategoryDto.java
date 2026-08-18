package com.lavander.estore.dto;

import com.lavander.estore.model.ProductCategory;

import java.util.List;

public record ProductCategoryDto(Long id, String categoryName, List<PropertyDefinitionDto> categoryProperties) {

    public static ProductCategoryDto fromEntity(ProductCategory entity) {
        List<PropertyDefinitionDto> properties = entity.getCategoryProperties().stream()
                .map(PropertyDefinitionDto::fromEntity)
                .toList();
        return new ProductCategoryDto(entity.getId(), entity.getCategoryName(), properties);
    }
}
