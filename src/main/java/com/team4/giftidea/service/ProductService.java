package com.team4.giftidea.service;

import com.team4.giftidea.entity.Product;
import com.team4.giftidea.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * ProductService - 상품 정보를 관리하는 서비스 클래스
 */
@Service
public class ProductService {

	private final ProductRepository productRepository;

	/**
	 * ProductService 생성자
	 *
	 * @param productRepository 상품 데이터 접근을 위한 리포지토리
	 */
	@Autowired
	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	/**
	 * 상품 리스트를 저장하며, 기존 상품 ID가 존재하는 경우 중복 저장을 방지합니다.
	 *
	 * @param productList 저장할 상품 리스트
	 * @param keyword     상품에 연관된 키워드
	 */
	public void saveItems(List<Product> productList, String keyword) {
		productList.forEach(product -> {
			product.setKeyword(keyword); // 키워드 설정

			// 상품 ID가 존재하지 않을 경우에만 저장
			if (!productRepository.existsByProductId(product.getProductId())) {
				productRepository.save(product);
			}
		});
	}

	/**
	 * 여러 키워드를 받아서 각 키워드에 해당하는 상품들을 최대 20개씩 반환합니다.
	 *
	 * @param keywords 검색할 키워드 목록
	 * @param pageNumber 페이지 번호
	 * @return 해당 키워드들에 맞는 상품 리스트
	 */
	public List<Product> searchByKeywords(List<String> keywords, int pageNumber) {
		List<Product> allProducts = new ArrayList<>();

		// 각 키워드로 상품 검색
		for (String keyword : keywords) {
			// 페이지네이션 처리
			Page<Product> productPage = productRepository.findByKeyword(keyword, PageRequest.of(pageNumber, 20));
			allProducts.addAll(productPage.getContent());  // getContent() 호출
		}

		return allProducts;
	}
}