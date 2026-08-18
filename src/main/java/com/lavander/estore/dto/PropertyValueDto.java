package com.lavander.estore.dto;

import com.lavander.estore.model.PropertyValue;

public record PropertyValueDto(Long id, PropertyDefinitionDto propertyDefinition, String propertyValue) {

    public static PropertyValueDto fromEntity(PropertyValue entity) {
        return new PropertyValueDto(
                entity.getId(),
                PropertyDefinitionDto.fromEntity(entity.getPropertyDefinition()),
                entity.getPropertyValue());
    }
}
