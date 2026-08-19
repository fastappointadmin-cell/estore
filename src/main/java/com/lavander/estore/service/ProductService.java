package com.lavander.estore.service;

import com.lavander.estore.dto.ProductDto;
import com.lavander.estore.dto.ProductRequest;
import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.dto.ProductVariantRequest;
import com.lavander.estore.dto.PropertyValueInput;
import com.lavander.estore.dto.ReviewRequest;
import com.lavander.estore.exception.ConflictException;
import com.lavander.estore.exception.NotFoundException;
import com.lavander.estore.model.Product;
import com.lavander.estore.model.ProductCategory;
import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.PropertyDefinition;
import com.lavander.estore.model.PropertyValue;
import com.lavander.estore.model.Review;
import com.lavander.estore.model.Tag;
import com.lavander.estore.repository.ProductCategoryRepository;
import com.lavander.estore.repository.ProductRepository;
import com.lavander.estore.repository.ProductVariantRepository;
import com.lavander.estore.repository.PropertyDefinitionRepository;
import com.lavander.estore.repository.ReviewRepository;
import com.lavander.estore.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final PropertyDefinitionRepository propertyDefinitionRepository;
    private final TagRepository tagRepository;
    private final ReviewRepository reviewRepository;

    public ProductService(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductCategoryRepository productCategoryRepository,
            PropertyDefinitionRepository propertyDefinitionRepository,
            TagRepository tagRepository,
            ReviewRepository reviewRepository) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.propertyDefinitionRepository = propertyDefinitionRepository;
        this.tagRepository = tagRepository;
        this.reviewRepository = reviewRepository;
    }

    // --- Product ---

    public ProductDto getProductById(Long id) {
        return ProductDto.fromEntity(findProductById(id));
    }

    public List<ProductDto> getProductsByCategoryId(Long categoryId) {
        return productRepository.findByProductCategoryId(categoryId).stream().map(ProductDto::fromEntity).toList();
    }

    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream().map(ProductDto::fromEntity).toList();
    }

    public ProductDto createProduct(ProductRequest request) {
        ProductCategory category = findCategoryById(request.categoryId());
        Product entity = new Product(request.productName(), request.productDescription(), category);
        entity.setExtraProperties(resolvePropertyDefinitions(request.extraPropertyIds()));
        return ProductDto.fromEntity(productRepository.save(entity));
    }

    public ProductDto updateProduct(Long id, ProductRequest request) {
        Product entity = findProductById(id);
        entity.setProductName(request.productName());
        entity.setProductDescription(request.productDescription());
        entity.setProductCategory(findCategoryById(request.categoryId()));
        entity.setExtraProperties(resolvePropertyDefinitions(request.extraPropertyIds()));
        return ProductDto.fromEntity(productRepository.save(entity));
    }

    public void deleteProduct(Long id) {
        Product entity = findProductById(id);
        if (productVariantRepository.existsByProductId(id)) {
            throw new ConflictException("Cannot delete product " + id + ": still has variants");
        }
        productRepository.delete(entity);
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
    }

    private ProductCategory findCategoryById(Long id) {
        return productCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product category not found with id: " + id));
    }

    private Set<PropertyDefinition> resolvePropertyDefinitions(List<Long> ids) {
        List<Long> safeIds = ids == null ? List.of() : ids;
        return new HashSet<>(propertyDefinitionRepository.findAllById(safeIds));
    }

    // --- Variant ---

    public List<ProductVariantDto> getProductVariantsByProductId(Long productId) {
        return productVariantRepository.findByProductId(productId).stream().map(ProductVariantDto::fromEntity).toList();
    }

    public List<ProductVariantDto> getAllVariants() {
        return productVariantRepository.findAll().stream().map(ProductVariantDto::fromEntity).toList();
    }

    public ProductVariantDto getVariantById(Long id) {
        return ProductVariantDto.fromEntity(findVariantById(id));
    }

    public ProductVariantDto createVariant(ProductVariantRequest request) {
        Product product = findProductById(request.productId());
        ProductVariant entity = new ProductVariant(
                request.variantName(), request.variantDescription(), product, request.price());
        applyVariantProperties(entity, request.variantProperties());
        entity.setTags(resolveTags(request.tagIds()));
        return ProductVariantDto.fromEntity(productVariantRepository.save(entity));
    }

    public ProductVariantDto updateVariant(Long id, ProductVariantRequest request) {
        ProductVariant entity = findVariantById(id);
        entity.setVariantName(request.variantName());
        entity.setVariantDescription(request.variantDescription());
        entity.setProduct(findProductById(request.productId()));
        entity.setPrice(request.price());
        entity.getVariantProperties().clear();
        applyVariantProperties(entity, request.variantProperties());
        entity.setTags(resolveTags(request.tagIds()));
        return ProductVariantDto.fromEntity(productVariantRepository.save(entity));
    }

    public void deleteVariant(Long id) {
        productVariantRepository.delete(findVariantById(id));
    }

    public ProductVariantDto submitReview(Long variantId, ReviewRequest request) {
        ProductVariant variant = findVariantById(variantId);
        Review review = reviewRepository.save(new Review(variant, request.rating()));
        variant.getReviews().add(review);
        return ProductVariantDto.fromEntity(variant);
    }

    private void applyVariantProperties(ProductVariant variant, List<PropertyValueInput> inputs) {
        List<PropertyValueInput> safeInputs = inputs == null ? List.of() : inputs;
        for (PropertyValueInput input : safeInputs) {
            PropertyDefinition propertyDefinition = propertyDefinitionRepository.findById(input.propertyDefinitionId())
                    .orElseThrow(() -> new NotFoundException("Property definition not found with id: " + input.propertyDefinitionId()));
            variant.addVariantProperty(new PropertyValue(propertyDefinition, input.value()));
        }
    }

    private Set<Tag> resolveTags(List<Long> ids) {
        List<Long> safeIds = ids == null ? List.of() : ids;
        return new HashSet<>(tagRepository.findAllById(safeIds));
    }

    private ProductVariant findVariantById(Long id) {
        return productVariantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product variant not found with id: " + id));
    }
}
