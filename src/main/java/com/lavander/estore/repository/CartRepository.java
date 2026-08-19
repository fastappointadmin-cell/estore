package com.lavander.estore.repository;

import com.lavander.estore.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByOwnerToken(String ownerToken);
}
