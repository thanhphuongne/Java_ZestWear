package com.zestwear.backend.repositories;

import com.zestwear.backend.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
	Product findBySlug(String slug);

	List<Product> findByFeaturedTrue(org.springframework.data.domain.Pageable pageable);
}
