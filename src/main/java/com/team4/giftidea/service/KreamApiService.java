package com.team4.giftidea.service;

import com.team4.giftidea.dto.ItemDTO;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class KreamApiService {

	@Value("${selenium.chromedriver-path}")
	private String chromeDriverPath;

	private static final String KREAM_SEARCH_URL = "https://kream.co.kr/search?keyword=%s&tab=products";

	public List<ItemDTO> searchItems(String query) {
		List<ItemDTO> itemList = new ArrayList<>();
		System.setProperty("webdriver.chrome.driver", chromeDriverPath);

		ChromeOptions options = new ChromeOptions();
		// 원래 제공된 user-agent 값 유지
		options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
		WebDriver driver = new ChromeDriver(options);

		try {
			// 검색어 URL 인코딩 및 페이지 접속
			String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
			String searchUrl = String.format(KREAM_SEARCH_URL, encodedQuery);
			System.out.println("Fetching URL: " + searchUrl);
			driver.get(searchUrl);

			// 페이지가 완전히 로드될 때까지 기다리기
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
			wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product_card")));

			// 상품 정보 크롤링
			List<WebElement> products = driver.findElements(By.cssSelector(".product_card"));
			for (WebElement product : products) {
				try {
					// 상품 이름, 가격, 이미지 URL, 상품 링크 추출
					String title = product.findElement(By.className("name")).getText();
					String priceText = product.findElement(By.className("amount")).getText().replaceAll("[^0-9]", "");
					Integer price = priceText.isEmpty() ? 0 : Integer.parseInt(priceText);
					String imageUrl = product.findElement(By.tagName("img")).getAttribute("src");
					String link = product.findElement(By.tagName("a")).getAttribute("href");

					// ItemDTO 객체에 데이터 저장
					ItemDTO item = new ItemDTO();
					item.setTitle(title);
					item.setPrice(price);
					item.setImage(imageUrl);
					item.setMallName("KREAM");
					item.setLink(link);
					item.applyWeight();

					itemList.add(item);
				} catch (NoSuchElementException e) {
					System.err.println("Error parsing product element: " + e.getMessage());
				}
			}

			System.out.println("Total items scraped: " + itemList.size());

		} catch (TimeoutException e) {
			System.err.println("Timeout while waiting for page elements: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			driver.quit();  // 브라우저 종료
		}

		return itemList;
	}
}