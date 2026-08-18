package com.lavander.estore.repository;

import com.lavander.estore.model.PropertyValue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyValueRepository extends JpaRepository<PropertyValue, Long> {
    boolean existsByPropertyDefinitionId(Long propertyDefinitionId);
}
