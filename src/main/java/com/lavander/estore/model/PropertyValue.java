package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PropertyValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    @ManyToOne
    @JoinColumn(name = "property_definition_id", nullable = false)
    private PropertyDefinition propertyDefinition;

    private String propertyValue;

    public PropertyValue(PropertyDefinition propertyDefinition, String propertyValue) {
        this.propertyDefinition = propertyDefinition;
        this.propertyValue = propertyValue;
    }
}
