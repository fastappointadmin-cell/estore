package com.lavander.estore.dto;

import com.lavander.estore.model.ProductCategory;

public record ProductCategoryRefDto(Long id, String categoryName) {

    public static ProductCategoryRefDto fromEntity(ProductCategory entity) {
        return new ProductCategoryRefDto(entity.getId(), entity.getCategoryName());
    }
}
