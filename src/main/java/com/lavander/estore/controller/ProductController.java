package com.lavander.estore.controller;

import com.lavander.estore.dto.ProductDto;
import com.lavander.estore.dto.ProductRequest;
import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.dto.AttachBucketImageRequest;
import com.lavander.estore.dto.ProductVariantRequest;
import com.lavander.estore.dto.ReviewRequest;
import com.lavander.estore.service.ImageStorageService;
import com.lavander.estore.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDto>> getProductsByCategoryId(@PathVariable Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByCategoryId(categoryId));
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{productId}/variants")
    public ResponseEntity<List<ProductVariantDto>> getProductVariantsByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(productService.getProductVariantsByProductId(productId));
    }

    @GetMapping("/variants")
    public ResponseEntity<List<ProductVariantDto>> getAllVariants() {
        return ResponseEntity.ok(productService.getAllVariants());
    }

    @GetMapping("/variants/{id}")
    public ResponseEntity<ProductVariantDto> getVariantById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getVariantById(id));
    }

    @PostMapping("/variants")
    public ResponseEntity<ProductVariantDto> createVariant(@Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(productService.createVariant(request));
    }

    @PutMapping("/variants/{id}")
    public ResponseEntity<ProductVariantDto> updateVariant(@PathVariable Long id, @Valid @RequestBody ProductVariantRequest request) {
        return ResponseEntity.ok(productService.updateVariant(id, request));
    }

    @DeleteMapping("/variants/{id}")
    public ResponseEntity<Void> deleteVariant(@PathVariable Long id) {
        productService.deleteVariant(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/variants/{id}/images")
    public ResponseEntity<ProductVariantDto> addVariantImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(productService.addVariantImage(id, file));
    }

    @DeleteMapping("/variants/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteVariantImage(@PathVariable Long id, @PathVariable Long imageId) {
        productService.deleteVariantImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/variants/images/browse")
    public ResponseEntity<List<ImageStorageService.BrowsedImage>> browseBucketImages() {
        return ResponseEntity.ok(productService.listBucketImages());
    }

    @PostMapping("/variants/{id}/images/from-bucket")
    public ResponseEntity<ProductVariantDto> attachVariantImageFromBucket(
            @PathVariable Long id, @Valid @RequestBody AttachBucketImageRequest request) {
        return ResponseEntity.ok(productService.attachVariantImageFromBucket(id, request.thumbnailKey()));
    }

    @PostMapping("/variants/{id}/reviews")
    public ResponseEntity<ProductVariantDto> submitReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(productService.submitReview(id, request));
    }
}
