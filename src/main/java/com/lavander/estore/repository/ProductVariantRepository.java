package com.lavander.estore.repository;

import com.lavander.estore.model.ProductVariant;
import com.lavander.estore.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);
    boolean existsByProductId(Long productId);
    boolean existsByTagsId(Long tagId);
    List<ProductVariant> findDistinctByTagsIn(Collection<Tag> tags);
}
