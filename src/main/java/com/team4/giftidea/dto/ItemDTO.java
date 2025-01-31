package com.team4.giftidea.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품 정보를 담는 DTO 클래스
 */
@Getter
@Setter
@NoArgsConstructor
public class ItemDTO {

	/**
	 * 상품명
	 */
	private String title;

	/**
	 * 상품 상세 페이지 링크
	 */
	private String link;

	/**
	 * 상품 가격
	 */
	private Integer price;

	/**
	 * 상품 이미지 URL
	 */
	private String image;

	/**
	 * 쇼핑몰 이름
	 */
	private String mallName;

	/**
	 * 상품 고유 ID
	 */
	private String productId;

	/**
	 * 브랜드명
	 */
	private String brand;

	/**
	 * 상품 카테고리
	 */
	private String category;
}