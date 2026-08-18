package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;

public record PropertyDefinitionRequest(@NotBlank String propertyName) {
}
