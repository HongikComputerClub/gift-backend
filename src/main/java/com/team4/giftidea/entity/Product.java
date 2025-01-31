package com.team4.giftidea.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품 정보를 저장하는 JPA 엔티티 클래스
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product {

	/**
	 * 기본 키 (자동 증가)
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * 상품 고유 ID (중복 방지)
	 */
	@Column(nullable = false, unique = true)
	private String productId;

	/**
	 * 상품명
	 */
	@Column(nullable = false)
	private String title;

	/**
	 * 상품 가격
	 */
	@Column(nullable = false)
	private Integer price;

	/**
	 * 상품 이미지 URL
	 */
	private String image;

	/**
	 * 판매처 (예: 네이버, 쿠팡, KREAM 등)
	 */
	@Column(nullable = false)
	private String mallName;

	/**
	 * 상품 상세 페이지 링크 (최대 길이 2048)
	 */
	@Column(length = 2048)
	private String link;

	/**
	 * 브랜드명
	 */
	private String brand;

	/**
	 * 상품 카테고리
	 */
	private String category;

	/**
	 * 검색 키워드 (상품과 연관된 키워드)
	 */
	@Column(nullable = false)
	private String keyword;
}