package com.lavander.estore.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @ManyToMany
    @JoinTable(
            name = "variant_tag",
            joinColumns = @JoinColumn(name = "variant_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "variant")
    private List<Review> reviews = new ArrayList<>();

    private BigDecimal price;

    public ProductVariant(String variantName, String variantDescription, Product product, BigDecimal price) {
        this.variantName = variantName;
        this.variantDescription = variantDescription;
        this.product = product;
        this.price = price;
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
