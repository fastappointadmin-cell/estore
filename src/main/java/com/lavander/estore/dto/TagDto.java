package com.lavander.estore.dto;

import com.lavander.estore.model.Tag;

public record TagDto(Long id, String tagName) {

    public static TagDto fromEntity(Tag entity) {
        return new TagDto(entity.getId(), entity.getTagName());
    }
}
