package com.lavander.estore.repository;

import com.lavander.estore.model.PromotionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromotionGroupRepository extends JpaRepository<PromotionGroup, Long> {
    boolean existsByTagsId(Long tagId);
}
