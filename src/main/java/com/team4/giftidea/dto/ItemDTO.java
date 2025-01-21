package com.team4.giftidea.dto;

import lombok.Getter;

@Getter
public class ItemDTO {

	private String title;
	private String link;
	private Integer price;
	private String image;
	private String mallName;
	private String productId;
	private String brand;
	private String category;

	public void setTitle(String title) { this.title = title; }

	public void setLink(String link) { this.link = link; }

	public void setPrice(Integer price) { this.price = price; }

	public void setImage(String image) { this.image = image; }

	public void setMallName(String mallName) { this.mallName = mallName; }

	public void setProductId(String productId) { this.productId = productId; }

	public void setBrand(String brand) { this.brand = brand; }

	public void setCategory(String category) { this.category = category; }
}