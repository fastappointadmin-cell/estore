package com.lavander.estore.controller;

import com.lavander.estore.dto.ProductCategoryDto;
import com.lavander.estore.dto.ProductCategoryGroupDto;
import com.lavander.estore.dto.ProductCategoryGroupRequest;
import com.lavander.estore.dto.ProductCategoryRequest;
import com.lavander.estore.dto.ProductSubCategoryGroupDto;
import com.lavander.estore.dto.ProductSubCategoryGroupRequest;
import com.lavander.estore.service.ProductCategoryService;
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
@RequestMapping("/api/product-categories")
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    public ProductCategoryController(ProductCategoryService productCategoryService) {
        this.productCategoryService = productCategoryService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductCategoryDto> getProductCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(productCategoryService.getProductCategoryById(id));
    }

    @PostMapping
    public ResponseEntity<ProductCategoryDto> createCategory(@Valid @RequestBody ProductCategoryRequest request) {
        return ResponseEntity.ok(productCategoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductCategoryDto> updateCategory(@PathVariable Long id, @Valid @RequestBody ProductCategoryRequest request) {
        return ResponseEntity.ok(productCategoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        productCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/groups")
    public ResponseEntity<List<ProductCategoryGroupDto>> getAllCategoryGroups() {
        return ResponseEntity.ok(productCategoryService.getAllCategoryGroups());
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<ProductCategoryGroupDto> getGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(productCategoryService.getGroupById(id));
    }

    @PostMapping("/groups")
    public ResponseEntity<ProductCategoryGroupDto> createGroup(@Valid @RequestBody ProductCategoryGroupRequest request) {
        return ResponseEntity.ok(productCategoryService.createGroup(request));
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<ProductCategoryGroupDto> updateGroup(@PathVariable Long id, @Valid @RequestBody ProductCategoryGroupRequest request) {
        return ResponseEntity.ok(productCategoryService.updateGroup(id, request));
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        productCategoryService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subgroups/{id}")
    public ResponseEntity<ProductSubCategoryGroupDto> getSubGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(productCategoryService.getSubGroupById(id));
    }

    @PostMapping("/subgroups")
    public ResponseEntity<ProductSubCategoryGroupDto> createSubGroup(@Valid @RequestBody ProductSubCategoryGroupRequest request) {
        return ResponseEntity.ok(productCategoryService.createSubGroup(request));
    }

    @PutMapping("/subgroups/{id}")
    public ResponseEntity<ProductSubCategoryGroupDto> updateSubGroup(@PathVariable Long id, @Valid @RequestBody ProductSubCategoryGroupRequest request) {
        return ResponseEntity.ok(productCategoryService.updateSubGroup(id, request));
    }

    @DeleteMapping("/subgroups/{id}")
    public ResponseEntity<Void> deleteSubGroup(@PathVariable Long id) {
        productCategoryService.deleteSubGroup(id);
        return ResponseEntity.noContent().build();
    }
}
