package com.team4.giftidea.repository;

import com.team4.giftidea.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	/**
	 * 주어진 키워드로 상품을 검색하고 페이지네이션을 적용합니다.
	 *
	 * @param keyword 검색할 키워드
	 * @param pageable 페이지네이션 정보
	 * @return 해당 키워드에 맞는 상품 리스트
	 */
	Page<Product> findByKeyword(String keyword, Pageable pageable);
}