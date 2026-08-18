package com.lavander.estore.dto;

import com.lavander.estore.model.PropertyDefinition;

public record PropertyDefinitionDto(Long id, String propertyName) {

    public static PropertyDefinitionDto fromEntity(PropertyDefinition entity) {
        return new PropertyDefinitionDto(entity.getId(), entity.getPropertyName());
    }
}
