package com.lavander.estore.dto;

import com.lavander.estore.model.PromotionGroup;

import java.util.List;

public record PromotionGroupDto(Long id, String groupName, List<TagDto> tags) {

    public static PromotionGroupDto fromEntity(PromotionGroup entity) {
        List<TagDto> tags = entity.getTags().stream().map(TagDto::fromEntity).toList();
        return new PromotionGroupDto(entity.getId(), entity.getGroupName(), tags);
    }
}
