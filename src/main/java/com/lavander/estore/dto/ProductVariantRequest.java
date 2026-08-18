package com.lavander.estore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ProductVariantRequest(
        @NotBlank String variantName,
        String variantDescription,
        @NotNull Long productId,
        @NotNull BigDecimal price,
        @NotNull Integer starRating,
        @Valid List<PropertyValueInput> variantProperties,
        List<Long> tagIds) {
}
