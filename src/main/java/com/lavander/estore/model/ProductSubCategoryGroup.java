package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProductSubCategoryGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupName;

    @ManyToOne
    @JoinColumn(name = "parent_group_id", nullable = false)
    private ProductCategoryGroup parentGroup;

    @OneToMany(mappedBy = "parentSubGroup")
    private List<ProductCategory> categories = new ArrayList<>();

    public ProductSubCategoryGroup(String groupName, ProductCategoryGroup parentGroup) {
        this.groupName = groupName;
        this.parentGroup = parentGroup;
    }
}
