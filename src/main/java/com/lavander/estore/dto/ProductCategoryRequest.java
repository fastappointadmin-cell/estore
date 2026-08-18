package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ProductCategoryRequest(
        @NotBlank String categoryName,
        Long parentGroupId,
        Long parentSubGroupId,
        List<Long> categoryPropertyIds) {
}
