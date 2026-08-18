package com.lavander.estore.repository;

import com.lavander.estore.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    boolean existsByParentGroupId(Long parentGroupId);
    boolean existsByParentSubGroupId(Long parentSubGroupId);
    boolean existsByCategoryPropertiesId(Long propertyDefinitionId);
}
