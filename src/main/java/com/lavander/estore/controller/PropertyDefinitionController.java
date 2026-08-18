package com.lavander.estore.controller;

import com.lavander.estore.dto.PropertyDefinitionDto;
import com.lavander.estore.dto.PropertyDefinitionRequest;
import com.lavander.estore.service.PropertyDefinitionService;
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
@RequestMapping("/api/property-definitions")
public class PropertyDefinitionController {

    private final PropertyDefinitionService propertyDefinitionService;

    public PropertyDefinitionController(PropertyDefinitionService propertyDefinitionService) {
        this.propertyDefinitionService = propertyDefinitionService;
    }

    @GetMapping
    public ResponseEntity<List<PropertyDefinitionDto>> getAll() {
        return ResponseEntity.ok(propertyDefinitionService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyDefinitionDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(propertyDefinitionService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PropertyDefinitionDto> create(@Valid @RequestBody PropertyDefinitionRequest request) {
        return ResponseEntity.ok(propertyDefinitionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PropertyDefinitionDto> update(@PathVariable Long id, @Valid @RequestBody PropertyDefinitionRequest request) {
        return ResponseEntity.ok(propertyDefinitionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        propertyDefinitionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
