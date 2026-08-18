package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PropertyValueInput(@NotNull Long propertyDefinitionId, @NotBlank String value) {
}
