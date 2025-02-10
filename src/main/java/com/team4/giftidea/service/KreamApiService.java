package com.team4.giftidea.service;

import com.team4.giftidea.entity.Product;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Kream 웹사이트에서 상품 정보를 크롤링하는 서비스 클래스
 */
@Service
@Slf4j
public class KreamApiService {

	private static final String KREAM_SEARCH_URL = "https://kream.co.kr/search?keyword=%s&tab=products";
	private static final String USER_AGENT = "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.90 Safari/537.36";

	@Value("${selenium.chromedriver-path}")
	private String chromeDriverPath;

	private final ProductService productService;

	@Autowired
	public KreamApiService(ProductService productService) {
		this.productService = productService;
	}

	/**
	 * 주어진 키워드에 대해 Kream에서 상품을 크롤링하여 리스트로 반환합니다.
	 *
	 * @param query 검색 키워드
	 * @return 크롤링된 상품 리스트
	 */
	public List<Product> searchItems(String query) {
		List<Product> productList = new ArrayList<>();
		System.setProperty("webdriver.chrome.driver", chromeDriverPath);

		ChromeOptions options = new ChromeOptions();
		options.setBinary("/opt/google/chrome/chrome");  // 크롬 바이너리 직접 지정 (AWS 환경)
		options.addArguments("--disable-gpu");
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--remote-debugging-port=9222");
		options.addArguments("--window-size=1920,1080");  // 창 크기 설정
		options.addArguments("--disable-software-rasterizer");
		options.addArguments("--disable-crash-reporter");
		options.addArguments("--disable-extensions");
		options.addArguments("--disable-hang-monitor");
		
		// 최신 User-Agent 추가
		options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/113.0.5672.63 Safari/537.36");
		
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
					Product productEntity = extractProductInfo(product, query);
					if (productEntity != null) {
						productList.add(productEntity);
						productService.saveItems(List.of(productEntity), query);
					}
				} catch (NoSuchElementException e) {
					log.warn("요소를 찾을 수 없음: {}", e.getMessage());
				}
			}
		} catch (TimeoutException e) {
			log.error("페이지 로딩 시간 초과: {}", e.getMessage());
		} catch (Exception e) {
			log.error("크롤링 중 오류 발생: {}", e.getMessage());
		} finally {
			driver.quit();  // 크롤링 후 브라우저 종료
		}

		return productList;
	}

	/**
	 * 개별 상품 정보를 추출하여 Product 객체를 생성합니다.
	 *
	 * @param productElement 크롤링된 상품 요소
	 * @param query          검색 키워드
	 * @return Product 객체 (추출 실패 시 null 반환)
	 */
	private Product extractProductInfo(WebElement productElement, String query) {
		try {
			String title = productElement.findElement(By.className("name")).getText();
			String priceText = productElement.findElement(By.className("amount")).getText().replaceAll("[^0-9]", "");
			Integer price = priceText.isEmpty() ? 0 : Integer.parseInt(priceText);
			String imageUrl = productElement.findElement(By.tagName("img")).getAttribute("src");
			String link = productElement.findElement(By.tagName("a")).getAttribute("href");

			Product product = new Product();
			product.setTitle(title);
			product.setPrice(price);
			product.setImage(imageUrl);
			product.setMallName("Kream");
			product.setLink(link);
			product.setKeyword(query);

			return product;
		} catch (NoSuchElementException e) {
			log.warn("상품 정보 추출 실패: {}", e.getMessage());
			return null;
		}
	}
}
