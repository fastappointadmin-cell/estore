package com.lavander.estore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String categoryName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    @JsonIgnore
    private ProductCategoryGroup parentGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_subgroup_id")
    @JsonIgnore
    private ProductSubCategoryGroup parentSubGroup;

    @ManyToMany
    @JoinTable(
            name = "category_properties",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "property_definition_id"))
    private Set<PropertyDefinition> categoryProperties = new HashSet<>();

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
