package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ProductRequest(
        @NotBlank String productName,
        String productDescription,
        @NotNull Long categoryId,
        List<Long> extraPropertyIds) {
}
