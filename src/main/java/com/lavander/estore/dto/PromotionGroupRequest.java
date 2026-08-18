package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record PromotionGroupRequest(@NotBlank String groupName, List<Long> tagIds) {
}
