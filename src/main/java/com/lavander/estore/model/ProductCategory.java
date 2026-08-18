package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String categoryName;

    @ManyToOne
    @JoinColumn(name = "parent_group_id")
    private ProductCategoryGroup parentGroup;

    @ManyToOne
    @JoinColumn(name = "parent_subgroup_id")
    private ProductSubCategoryGroup parentSubGroup;

    @ManyToMany
    @JoinTable(
            name = "category_properties",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "property_definition_id"))
    private List<PropertyDefinition> categoryProperties = new ArrayList<>();

    public ProductCategory(String categoryName) {
        this.categoryName = categoryName;
    }

    @PrePersist
    @PreUpdate
    private void validateExactlyOneParent() {
        boolean hasGroup = parentGroup != null;
        boolean hasSubGroup = parentSubGroup != null;
        if (hasGroup == hasSubGroup) {
            throw new IllegalStateException(
                    "ProductCategory must have exactly one of parentGroup or parentSubGroup set");
        }
    }
}
