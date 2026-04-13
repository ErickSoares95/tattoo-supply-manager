package com.ericksoares.tattoo.product.repository;

import com.ericksoares.tattoo.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
