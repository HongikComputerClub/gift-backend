package com.team4.giftidea.service;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.team4.giftidea.entity.Product;

@Service
public class KreamApiService {

	@Value("${selenium.chromedriver-path}")
	private String chromeDriverPath;

	@Autowired
	private ProductService productService;

	private static final String KREAM_SEARCH_URL = "https://kream.co.kr/search?keyword=%s&tab=products";

	/**
	 * 주어진 키워드에 대해 Kream에서 상품을 크롤링하여 리스트로 반환합니다.
	 * @param query 검색어
	 * @return 크롤링된 상품 리스트
	 */
	public List<Product> searchItems(String query) {
		List<Product> productList = new ArrayList<>();
		System.setProperty("webdriver.chrome.driver", chromeDriverPath);

		ChromeOptions options = new ChromeOptions();
		options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
		WebDriver driver = new ChromeDriver(options);

		try {
			String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
			String searchUrl = String.format(KREAM_SEARCH_URL, encodedQuery);
			driver.get(searchUrl);

			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
			wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".product_card")));

			List<WebElement> products = driver.findElements(By.cssSelector(".product_card"));

			for (WebElement product : products) {
				try {
					String title = product.findElement(By.className("name")).getText();
					String priceText = product.findElement(By.className("amount")).getText().replaceAll("[^0-9]", "");
					Integer price = priceText.isEmpty() ? 0 : Integer.parseInt(priceText);
					String imageUrl = product.findElement(By.tagName("img")).getAttribute("src");
					String link = product.findElement(By.tagName("a")).getAttribute("href");

					// Product 객체에 값 설정
					Product productEntity = new Product();
					productEntity.setTitle(title);
					productEntity.setPrice(price);
					productEntity.setImage(imageUrl);
					productEntity.setMallName("Kream");
					productEntity.setLink(link);
					productEntity.setKeyword(query);  // 키워드를 상품에 설정

					productList.add(productEntity);

					// 상품을 데이터베이스에 저장
					productService.saveItems(List.of(productEntity), query);

				} catch (NoSuchElementException e) {
					// 요소가 없을 때 발생하는 예외 처리
					e.printStackTrace();
				}
			}

		} catch (TimeoutException e) {
			// 페이지 로딩 타임아웃 예외 처리
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			driver.quit();  // 크롤링 후 브라우저 종료
		}

		return productList;
	}
}