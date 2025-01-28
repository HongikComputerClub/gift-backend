package com.team4.giftidea.service;

import com.team4.giftidea.entity.Product;
import com.team4.giftidea.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

	private final ProductRepository productRepository;

	/**
	 * ProductService 생성자
	 *
	 * @param productRepository ProductRepository 인스턴스 주입
	 */
	@Autowired
	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	/**
	 * 주어진 상품 리스트와 키워드를 기반으로 상품 정보를 저장합니다.
	 * 상품 ID가 이미 존재하는 경우, 중복 저장을 방지합니다.
	 *
	 * @param productList 상품 리스트
	 * @param keyword    상품에 연결할 키워드
	 */
	public void saveItems(List<Product> productList, String keyword) {
		for (Product product : productList) {
			product.setKeyword(keyword);  // 키워드 설정

			// 상품 ID가 존재하지 않으면 새 상품 저장
			if (!productRepository.existsByProductId(product.getProductId())) {
				productRepository.save(product);
			}
		}
	}
}