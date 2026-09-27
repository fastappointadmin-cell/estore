package com.lavander.estore.repository;

import com.lavander.estore.model.VariantImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VariantImageRepository extends JpaRepository<VariantImage, Long> {
    List<VariantImage> findByVariantIdOrderByDisplayOrder(Long variantId);
    Optional<VariantImage> findByIdAndVariantId(Long id, Long variantId);
    int countByVariantId(Long variantId);
}
