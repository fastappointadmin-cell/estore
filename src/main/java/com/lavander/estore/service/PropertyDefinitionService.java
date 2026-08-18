package com.lavander.estore.service;

import com.lavander.estore.dto.PropertyDefinitionDto;
import com.lavander.estore.dto.PropertyDefinitionRequest;
import com.lavander.estore.exception.ConflictException;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.PropertyDefinitionRepository;
import com.lavander.estore.repository.PropertyValueRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PropertyDefinitionService {

    private final PropertyDefinitionRepository propertyDefinitionRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductRepository productRepository;
    private final PropertyValueRepository propertyValueRepository;

    public PropertyDefinitionService(
            PropertyDefinitionRepository propertyDefinitionRepository,
            ProductCategoryRepository productCategoryRepository,
            ProductRepository productRepository,
            PropertyValueRepository propertyValueRepository) {
        this.propertyDefinitionRepository = propertyDefinitionRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productRepository = productRepository;
        this.propertyValueRepository = propertyValueRepository;
    }

    public List<PropertyDefinitionDto> getAll() {
        return propertyDefinitionRepository.findAll().stream().map(PropertyDefinitionDto::fromEntity).toList();
    }

    public PropertyDefinitionDto getById(Long id) {
        return PropertyDefinitionDto.fromEntity(findEntityById(id));
    }

    public PropertyDefinitionDto create(PropertyDefinitionRequest request) {
        PropertyDefinition entity = new PropertyDefinition(request.propertyName());
        return PropertyDefinitionDto.fromEntity(propertyDefinitionRepository.save(entity));
    }

    public PropertyDefinitionDto update(Long id, PropertyDefinitionRequest request) {
        PropertyDefinition entity = findEntityById(id);
        entity.setPropertyName(request.propertyName());
        return PropertyDefinitionDto.fromEntity(propertyDefinitionRepository.save(entity));
    }

    public void delete(Long id) {
        PropertyDefinition entity = findEntityById(id);
        if (productCategoryRepository.existsByCategoryPropertiesId(id)) {
            throw new ConflictException("Cannot delete property definition " + id + ": still used by a category");
        }
        if (productRepository.existsByExtraPropertiesId(id)) {
            throw new ConflictException("Cannot delete property definition " + id + ": still used by a product");
        }
        if (propertyValueRepository.existsByPropertyDefinitionId(id)) {
            throw new ConflictException("Cannot delete property definition " + id + ": still used by a variant property value");
        }
        propertyDefinitionRepository.delete(entity);
    }

    private PropertyDefinition findEntityById(Long id) {
        return propertyDefinitionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Property definition not found with id: " + id));
    }
}
