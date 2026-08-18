package com.lavander.estore.controller;

import com.lavander.estore.dto.ProductDto;
import com.lavander.estore.dto.ProductRequest;
import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.dto.ProductVariantRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
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
}
