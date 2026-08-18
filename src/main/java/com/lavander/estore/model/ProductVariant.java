package com.lavander.estore.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String variantName;

    private String variantDescription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PropertyValue> variantProperties = new ArrayList<>();

    private BigDecimal price;

    private Integer starRating;

    public ProductVariant(String variantName, String variantDescription, Product product,
                           BigDecimal price, Integer starRating) {
        this.variantName = variantName;
        this.variantDescription = variantDescription;
        this.product = product;
        this.price = price;
        this.starRating = starRating;
    }

    public void addVariantProperty(PropertyValue propertyValue) {
        variantProperties.add(propertyValue);
        propertyValue.setVariant(this);
    }

    public void removeVariantProperty(PropertyValue propertyValue) {
        variantProperties.remove(propertyValue);
        propertyValue.setVariant(null);
    }
}
