package com.lavander.estore.service;

import com.lavander.estore.dto.ProductCategoryDto;
import com.lavander.estore.dto.ProductCategoryGroupDto;
import com.lavander.estore.dto.ProductCategoryGroupRequest;
import com.lavander.estore.dto.ProductCategoryRequest;
import com.lavander.estore.dto.ProductSubCategoryGroupDto;
import com.lavander.estore.dto.ProductSubCategoryGroupRequest;
import com.lavander.estore.exception.ConflictException;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductCategoryGroup;
import com.lavander.estore.model.ProductSubCategoryGroup;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.repository.ProductCategoryGroupRepository;
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.ProductSubCategoryGroupRepository;
import com.lavander.estore.repository.PropertyDefinitionRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;
    private final ProductCategoryGroupRepository categoryGroupRepository;
    private final ProductSubCategoryGroupRepository subCategoryGroupRepository;
    private final PropertyDefinitionRepository propertyDefinitionRepository;
    private final ProductRepository productRepository;

    public ProductCategoryService(
            ProductCategoryRepository productCategoryRepository,
            ProductCategoryGroupRepository categoryGroupRepository,
            ProductSubCategoryGroupRepository subCategoryGroupRepository,
            PropertyDefinitionRepository propertyDefinitionRepository,
            ProductRepository productRepository) {
        this.productCategoryRepository = productCategoryRepository;
        this.categoryGroupRepository = categoryGroupRepository;
        this.subCategoryGroupRepository = subCategoryGroupRepository;
        this.propertyDefinitionRepository = propertyDefinitionRepository;
        this.productRepository = productRepository;
    }

    // --- Category ---

    public ProductCategoryDto getProductCategoryById(Long id) {
        return ProductCategoryDto.fromEntity(findCategoryById(id));
    }

    public List<ProductCategoryGroupDto> getAllCategoryGroups() {
        return ProductCategoryGroupDto.fromEntities(categoryGroupRepository.findAll());
    }

    public ProductCategoryDto createCategory(ProductCategoryRequest request) {
        ProductCategory entity = new ProductCategory(request.categoryName());
        applyCategoryRequest(entity, request);
        return ProductCategoryDto.fromEntity(productCategoryRepository.save(entity));
    }

    public ProductCategoryDto updateCategory(Long id, ProductCategoryRequest request) {
        ProductCategory entity = findCategoryById(id);
        entity.setCategoryName(request.categoryName());
        applyCategoryRequest(entity, request);
        return ProductCategoryDto.fromEntity(productCategoryRepository.save(entity));
    }

    public void deleteCategory(Long id) {
        ProductCategory entity = findCategoryById(id);
        if (productRepository.existsByProductCategoryId(id)) {
            throw new ConflictException("Cannot delete category " + id + ": still has products");
        }
        productCategoryRepository.delete(entity);
    }

    private void applyCategoryRequest(ProductCategory entity, ProductCategoryRequest request) {
        boolean hasGroup = request.parentGroupId() != null;
        boolean hasSubGroup = request.parentSubGroupId() != null;
        if (hasGroup == hasSubGroup) {
            throw new IllegalArgumentException("Exactly one of parentGroupId or parentSubGroupId must be set");
        }

        if (hasGroup) {
            entity.setParentGroup(findGroupById(request.parentGroupId()));
            entity.setParentSubGroup(null);
        } else {
            entity.setParentSubGroup(findSubGroupById(request.parentSubGroupId()));
            entity.setParentGroup(null);
        }

        List<Long> propertyIds = request.categoryPropertyIds() == null ? List.of() : request.categoryPropertyIds();
        Set<PropertyDefinition> properties = new HashSet<>(propertyDefinitionRepository.findAllById(propertyIds));
        entity.setCategoryProperties(properties);
    }

    private ProductCategory findCategoryById(Long id) {
        return productCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product category not found with id: " + id));
    }

    // --- Group ---

    public ProductCategoryGroupDto getGroupById(Long id) {
        return ProductCategoryGroupDto.fromEntity(findGroupById(id));
    }

    public ProductCategoryGroupDto createGroup(ProductCategoryGroupRequest request) {
        ProductCategoryGroup entity = new ProductCategoryGroup(request.groupName());
        return ProductCategoryGroupDto.fromEntity(categoryGroupRepository.save(entity));
    }

    public ProductCategoryGroupDto updateGroup(Long id, ProductCategoryGroupRequest request) {
        ProductCategoryGroup entity = findGroupById(id);
        entity.setGroupName(request.groupName());
        return ProductCategoryGroupDto.fromEntity(categoryGroupRepository.save(entity));
    }

    public void deleteGroup(Long id) {
        ProductCategoryGroup entity = findGroupById(id);
        if (subCategoryGroupRepository.existsByParentGroupId(id)) {
            throw new ConflictException("Cannot delete group " + id + ": still has subgroups");
        }
        if (productCategoryRepository.existsByParentGroupId(id)) {
            throw new ConflictException("Cannot delete group " + id + ": still has categories");
        }
        categoryGroupRepository.delete(entity);
    }

    private ProductCategoryGroup findGroupById(Long id) {
        return categoryGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product category group not found with id: " + id));
    }

    // --- Subgroup ---

    public ProductSubCategoryGroupDto getSubGroupById(Long id) {
        return ProductSubCategoryGroupDto.fromEntity(findSubGroupById(id));
    }

    public ProductSubCategoryGroupDto createSubGroup(ProductSubCategoryGroupRequest request) {
        ProductCategoryGroup parentGroup = findGroupById(request.parentGroupId());
        ProductSubCategoryGroup entity = new ProductSubCategoryGroup(request.groupName(), parentGroup);
        return ProductSubCategoryGroupDto.fromEntity(subCategoryGroupRepository.save(entity));
    }

    public ProductSubCategoryGroupDto updateSubGroup(Long id, ProductSubCategoryGroupRequest request) {
        ProductSubCategoryGroup entity = findSubGroupById(id);
        entity.setGroupName(request.groupName());
        entity.setParentGroup(findGroupById(request.parentGroupId()));
        return ProductSubCategoryGroupDto.fromEntity(subCategoryGroupRepository.save(entity));
    }

    public void deleteSubGroup(Long id) {
        ProductSubCategoryGroup entity = findSubGroupById(id);
        if (productCategoryRepository.existsByParentSubGroupId(id)) {
            throw new ConflictException("Cannot delete subgroup " + id + ": still has categories");
        }
        subCategoryGroupRepository.delete(entity);
    }

    private ProductSubCategoryGroup findSubGroupById(Long id) {
        return subCategoryGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product subcategory group not found with id: " + id));
    }
}
