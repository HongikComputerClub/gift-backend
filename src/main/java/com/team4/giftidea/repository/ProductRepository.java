package com.team4.giftidea.repository;

import com.team4.giftidea.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	/**
	 * 주어진 productId가 이미 존재하는지 확인합니다.
	 *
	 * @param productId 확인할 상품 ID
	 * @return 존재 여부
	 */
	boolean existsByProductId(String productId);
}