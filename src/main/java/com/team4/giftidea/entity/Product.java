package com.team4.giftidea.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "products")
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String productId;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private Integer price;

	private String image;

	@Column(nullable = false)
	private String mallName;

	@Column(length = 2048)
	private String link;

	private String brand;

	private String category;

	@Column(nullable = false)
	private String keyword;
}