package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductCategoryGroupRequest(@NotBlank String groupName) {
}
