package com.team4.giftidea.service;

import com.team4.giftidea.entity.Product;
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
public class CoupangApiService {

	@Value("${selenium.chromedriver-path}")
	private String chromeDriverPath;

	private static final String COUPANG_SEARCH_URL = "https://www.coupang.com/np/search?q=%s&channel=user";

	private final ProductService productService;

	/**
	 * CoupangApiService 생성자
	 *
	 * @param productService ProductService 인스턴스 주입
	 */
	public CoupangApiService(ProductService productService) {
		this.productService = productService;
	}

	/**
	 * 주어진 키워드에 대해 Coupang에서 상품 정보를 크롤링합니다.
	 *
	 * @param query 검색할 키워드
	 * @return 크롤링된 상품 리스트
	 */
	public List<Product> searchItems(String query) {
		List<Product> productList = new ArrayList<>();
		System.setProperty("webdriver.chrome.driver", chromeDriverPath);

		ChromeOptions options = new ChromeOptions();
		options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
		WebDriver driver = new ChromeDriver(options);

		try {
			// 검색어 URL 인코딩 및 페이지 접속
			String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
			String searchUrl = String.format(COUPANG_SEARCH_URL, encodedQuery);
			driver.get(searchUrl);

			// 페이지가 완전히 로드될 때까지 대기
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(90));
			wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".search-product")));

			List<WebElement> products = driver.findElements(By.cssSelector(".search-product"));

			// 상품 정보 크롤링
			for (WebElement product : products) {
				try {
					// 상품 제목 추출
					String title = product.findElement(By.cssSelector(".name")).getText();

					// 가격 추출
					String priceText = product.findElement(By.cssSelector(".price-value")).getText().replaceAll("[^0-9]", "");
					Integer price = priceText.isEmpty() ? 0 : Integer.parseInt(priceText);

					// 이미지 URL 추출
					String imageUrl = product.findElement(By.tagName("img")).getAttribute("src");

					// 상품 링크 추출
					String link = product.findElement(By.tagName("a")).getAttribute("href");

					// 상품 고유 ID 추출
					String productId = link.split("\\?")[0].split("/")[5];

					// Product 객체 생성 및 값 설정
					Product productEntity = new Product();
					productEntity.setTitle(title);
					productEntity.setPrice(price);
					productEntity.setImage(imageUrl);
					productEntity.setMallName("Coupang");
					productEntity.setLink(link);
					productEntity.setKeyword(query);  // 키워드 설정
					productEntity.setProductId(productId);  // 고유 ID 설정

					// DB에 저장
					productService.saveItems(List.of(productEntity), query);
					productList.add(productEntity);

				} catch (NoSuchElementException e) {
					System.err.println("Error parsing product element: " + e.getMessage());
				}
			}

		} catch (TimeoutException e) {
			System.err.println("Timeout while waiting for page elements: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			driver.quit();  // 브라우저 종료
		}

		return productList;
	}
}