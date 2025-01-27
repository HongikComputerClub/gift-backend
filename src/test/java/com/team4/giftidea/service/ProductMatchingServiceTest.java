package com.team4.giftidea.service;

import com.team4.giftidea.dto.ItemDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProductMatchingServiceTest {

	private final ProductMatchingService productMatchingService = new ProductMatchingService();

	@Test
	void testMatchProducts_WhenProductsMatch_ReturnCheaperItem() {
		ItemDTO kreamItem = new ItemDTO();
		kreamItem.setTitle("Nike Air Max");
		kreamItem.setPrice(150000);

		ItemDTO naverItem = new ItemDTO();
		naverItem.setTitle("Nike Air Max");
		naverItem.setPrice(130000);

		ItemDTO result = productMatchingService.matchProducts(kreamItem, naverItem);

		assertNotNull(result);
		assertEquals(130000, result.getPrice());
	}

	@Test
	void testMatchProducts_WhenProductsDoNotMatch_ReturnNull() {
		ItemDTO kreamItem = new ItemDTO();
		kreamItem.setTitle("Nike Air Max");
		kreamItem.setPrice(150000);

		ItemDTO naverItem = new ItemDTO();
		naverItem.setTitle("Adidas Ultra Boost");
		naverItem.setPrice(140000);

		ItemDTO result = productMatchingService.matchProducts(kreamItem, naverItem);

		assertNull(result);
	}
}