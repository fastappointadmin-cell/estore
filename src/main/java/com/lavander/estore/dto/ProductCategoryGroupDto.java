package com.lavander.estore.dto;

import com.lavander.estore.model.ProductCategoryGroup;

import java.util.List;

public record ProductCategoryGroupDto(
        Long id,
        String groupName,
        List<ProductSubCategoryGroupDto> subGroups,
        List<ProductCategoryDto> categories) {

    public static ProductCategoryGroupDto fromEntity(ProductCategoryGroup entity) {
        List<ProductSubCategoryGroupDto> subGroups = entity.getSubGroups().stream()
                .map(ProductSubCategoryGroupDto::fromEntity)
                .toList();
        List<ProductCategoryDto> categories = entity.getCategories().stream()
                .map(ProductCategoryDto::fromEntity)
                .toList();
        return new ProductCategoryGroupDto(entity.getId(), entity.getGroupName(), subGroups, categories);
    }
}
