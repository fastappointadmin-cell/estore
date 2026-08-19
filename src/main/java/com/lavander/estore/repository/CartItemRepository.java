package com.lavander.estore.repository;

import com.lavander.estore.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void deleteByVariantId(Long variantId);
}
