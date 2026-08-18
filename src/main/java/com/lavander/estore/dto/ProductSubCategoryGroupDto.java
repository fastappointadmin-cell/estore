package com.lavander.estore.dto;

import com.lavander.estore.model.ProductSubCategoryGroup;

import java.util.List;

public record ProductSubCategoryGroupDto(Long id, String groupName, List<ProductCategoryDto> categories) {

    public static ProductSubCategoryGroupDto fromEntity(ProductSubCategoryGroup entity) {
        List<ProductCategoryDto> categories = entity.getCategories().stream()
                .map(ProductCategoryDto::fromEntity)
                .toList();
        return new ProductSubCategoryGroupDto(entity.getId(), entity.getGroupName(), categories);
    }
}
