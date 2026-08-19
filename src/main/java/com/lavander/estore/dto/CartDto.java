package com.lavander.estore.dto;

import com.lavander.estore.model.Cart;

import java.util.List;

public record CartDto(Long id, List<CartItemDto> items) {
    public static CartDto fromEntity(Cart entity) {
        List<CartItemDto> items = entity.getItems().stream().map(CartItemDto::fromEntity).toList();
        return new CartDto(entity.getId(), items);
    }
}
