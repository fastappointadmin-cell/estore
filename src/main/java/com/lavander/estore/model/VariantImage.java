package com.lavander.estore.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
public class VariantImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    private String thumbnailUrl;

    private String mediumUrl;

    // R2 object keys, kept only to delete both sizes from the bucket later —
    // never exposed in ProductVariantDto.
    private String thumbnailKey;

    private String mediumKey;

    private Integer displayOrder;

    public VariantImage(
            ProductVariant variant, String thumbnailUrl, String mediumUrl, String thumbnailKey, String mediumKey, Integer displayOrder) {
        this.variant = variant;
        this.thumbnailUrl = thumbnailUrl;
        this.mediumUrl = mediumUrl;
        this.thumbnailKey = thumbnailKey;
        this.mediumKey = mediumKey;
        this.displayOrder = displayOrder;
    }
}
