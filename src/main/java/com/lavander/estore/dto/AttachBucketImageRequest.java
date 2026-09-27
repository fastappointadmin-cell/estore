package com.lavander.estore.dto;

import jakarta.validation.constraints.NotBlank;

public record AttachBucketImageRequest(@NotBlank String thumbnailKey) {
}
