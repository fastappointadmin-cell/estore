package com.lavander.estore.dto;

import com.lavander.estore.model.Product;

public record ProductRefDto(Long id, String productName, Long categoryId) {

    public static ProductRefDto fromEntity(Product entity) {
        return new ProductRefDto(entity.getId(), entity.getProductName(), entity.getProductCategory().getId());
    }
}
