package com.lavander.estore.repository;

import com.lavander.estore.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByProductCategoryId(Long categoryId);
    boolean existsByProductCategoryId(Long categoryId);
    boolean existsByExtraPropertiesId(Long propertyDefinitionId);
}
