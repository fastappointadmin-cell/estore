package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PromotionGroupRequest(
        @NotBlank String groupName, String description, boolean featured, List<Long> tagIds) {
}
