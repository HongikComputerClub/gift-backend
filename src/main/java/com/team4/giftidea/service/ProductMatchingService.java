package com.team4.giftidea.service;

import com.team4.giftidea.dto.ItemDTO;
import org.springframework.stereotype.Service;

@Service
public class ProductMatchingService {

	public ItemDTO matchProducts(ItemDTO kreamItem, ItemDTO naverItem) {
		if (normalize(kreamItem.getTitle()).equalsIgnoreCase(normalize(naverItem.getTitle()))) {
			return kreamItem.getPrice() < naverItem.getPrice() ? kreamItem : naverItem;
		}
		return null;
	}

	private String normalize(String input) {
		return input.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
	}
}