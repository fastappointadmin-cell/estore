package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;

public record TagRequest(@NotBlank String tagName) {
}
