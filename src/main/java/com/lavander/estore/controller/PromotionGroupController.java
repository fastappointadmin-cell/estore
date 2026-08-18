package com.lavander.estore.controller;

import com.lavander.estore.dto.ProductVariantDto;
import com.lavander.estore.dto.PromotionGroupDto;
import com.lavander.estore.dto.PromotionGroupRequest;
import com.lavander.estore.service.PromotionGroupService;
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
@RequestMapping("/api/promotion-groups")
public class PromotionGroupController {

    private final PromotionGroupService promotionGroupService;

    public PromotionGroupController(PromotionGroupService promotionGroupService) {
        this.promotionGroupService = promotionGroupService;
    }

    @GetMapping
    public ResponseEntity<List<PromotionGroupDto>> getAll() {
        return ResponseEntity.ok(promotionGroupService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionGroupDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(promotionGroupService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PromotionGroupDto> create(@Valid @RequestBody PromotionGroupRequest request) {
        return ResponseEntity.ok(promotionGroupService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromotionGroupDto> update(@PathVariable Long id, @Valid @RequestBody PromotionGroupRequest request) {
        return ResponseEntity.ok(promotionGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        promotionGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/variants")
    public ResponseEntity<List<ProductVariantDto>> getVariants(@PathVariable Long id) {
        return ResponseEntity.ok(promotionGroupService.getVariantsForGroup(id));
    }
}
