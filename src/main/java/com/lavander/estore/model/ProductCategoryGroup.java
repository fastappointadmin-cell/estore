package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class ProductCategoryGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupName;

    @OneToMany(mappedBy = "parentGroup")
    private List<ProductSubCategoryGroup> subGroups = new ArrayList<>();

    @OneToMany(mappedBy = "parentGroup")
    private List<ProductCategory> categories = new ArrayList<>();

    public ProductCategoryGroup(String groupName) {
        this.groupName = groupName;
    }
}
