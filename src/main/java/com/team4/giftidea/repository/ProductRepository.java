package com.team4.giftidea.repository;

import com.team4.giftidea.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 상품 정보를 저장 및 조회하는 JPA Repository 인터페이스
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

	/**
	 * 주어진 productId가 이미 존재하는지 확인합니다.
	 *
	 * @param productId 확인할 상품 ID
	 * @return 존재 여부 (true: 존재함, false: 존재하지 않음)
	 */
	boolean existsByProductId(String productId);
}