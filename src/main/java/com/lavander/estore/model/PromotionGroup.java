package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import org.hibernate.annotations.ColumnDefault;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class PromotionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupName;

    private String description;

    // Any number of groups can be featured at once — the site banner cycles through
    // all of them. Explicit default so `ddl-auto: update` can add this NOT NULL column
    // to a table that already has rows.
    @ColumnDefault("false")
    private boolean featured;

    @ManyToMany
    @JoinTable(
            name = "promotion_group_tag",
            joinColumns = @JoinColumn(name = "promotion_group_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    public PromotionGroup(String groupName) {
        this.groupName = groupName;
    }
}
