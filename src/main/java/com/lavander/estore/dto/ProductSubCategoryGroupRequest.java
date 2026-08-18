package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductSubCategoryGroupRequest(@NotBlank String groupName, @NotNull Long parentGroupId) {
}
