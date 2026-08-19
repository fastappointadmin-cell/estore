package com.lavander.estore.dto;

import com.lavander.estore.model.CartItem;

public record CartItemDto(Long id, ProductVariantDto variant, Integer quantity) {
    public static CartItemDto fromEntity(CartItem entity) {
        return new CartItemDto(entity.getId(), ProductVariantDto.fromEntity(entity.getVariant()), entity.getQuantity());
    }
}
