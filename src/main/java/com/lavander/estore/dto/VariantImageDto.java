package com.lavander.estore.dto;

import com.lavander.estore.model.VariantImage;

public record VariantImageDto(Long id, String thumbnailUrl, String mediumUrl) {

    public static VariantImageDto fromEntity(VariantImage entity) {
        return new VariantImageDto(entity.getId(), entity.getThumbnailUrl(), entity.getMediumUrl());
    }
}
