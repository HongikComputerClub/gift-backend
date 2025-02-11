package com.team4.giftidea.service;

import com.team4.giftidea.entity.Product;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 쿠팡에서 상품을 크롤링하는 서비스 클래스
 */
@Service
@Slf4j
public class CoupangApiService {

	private static final String COUPANG_SEARCH_URL = "https://www.coupang.com/np/search?q=%s&channel=user";
	private static final String USER_AGENT = "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.5735.90 Safari/537.36";

	@Value("${selenium.chromedriver-path}")
	private String chromeDriverPath;

	private final ProductService productService;

	/**
	 * CoupangApiService 생성자
	 *
	 * @param productService 상품 저장을 위한 서비스
	 */
	public CoupangApiService(ProductService productService) {
		this.productService = productService;
	}

	/**
     * 주어진 키워드에 대해 쿠팡에서 상품을 검색하고 리스트로 반환합니다.
     *
     * @param query 검색 키워드
     * @return 크롤링된 상품 리스트
     */
    public List<Product> searchItems(String query) {
        List<Product> productList = new ArrayList<>();
        log.info("크롤링 시작: 키워드 = {}", query);

        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        log.debug("ChromeDriver 경로: {}", chromeDriverPath);

        ChromeOptions options = new ChromeOptions();
	options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--remote-debugging-port=9222");
        options.addArguments("--disable-software-rasterizer");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--window-size=1920,1080");
        options.addArguments(USER_AGENT);

        WebDriver driver = null; // 드라이버 선언

        try {
            driver = new ChromeDriver(options); // 드라이버 초기화
            log.info("ChromeDriver 초기화 성공");

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchUrl = String.format(COUPANG_SEARCH_URL, encodedQuery);

            log.info("쿠팡 검색 URL: {}", searchUrl);
            driver.get(searchUrl);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(90));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".search-product")));
            log.info("쿠팡 검색 페이지 로딩 완료");


            List<WebElement> products = driver.findElements(By.cssSelector(".search-product"));
            log.info("검색된 상품 개수: {}", products.size());

            for (WebElement productElement : products) {
                try {
                    Product productEntity = extractProductInfo(productElement, query);
                    if (productEntity != null) {
                        productList.add(productEntity);
                        productService.saveItems(List.of(productEntity), query);
                        log.debug("상품 정보 추출 및 저장 성공: {}", productEntity);
                    }
                } catch (NoSuchElementException e) {
                    log.warn("요소 찾기 실패: {}", e.getMessage(), e); // 예외 객체 e도 함께 로깅
                }
            }
        } catch (TimeoutException e) {
            log.error("페이지 로딩 시간 초과: {}", e.getMessage(), e); // 예외 객체 e도 함께 로깅
        } catch (SessionNotCreatedException e) {
            log.error("세션 생성 실패: {}", e.getMessage(), e);
        } catch (WebDriverException e) {
            log.error("WebDriver 오류: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("크롤링 중 오류 발생: {}", e.getMessage(), e); // 예외 객체 e도 함께 로깅
        } finally {
            if (driver != null) { // 드라이버가 null이 아닌 경우에만 종료
                driver.quit();
                log.info("ChromeDriver 종료");
            }
        }

        log.info("크롤링 종료: 반환된 상품 개수 = {}", productList.size());
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
			String title = productElement.findElement(By.cssSelector(".name")).getText();
			String priceText = productElement.findElement(By.cssSelector(".price-value"))
				.getText().replaceAll("[^0-9]", "");
			Integer price = priceText.isEmpty() ? 0 : Integer.parseInt(priceText);
			String imageUrl = productElement.findElement(By.tagName("img")).getAttribute("src");
			String link = productElement.findElement(By.tagName("a")).getAttribute("href");

			// 쿠팡 상품의 고유 ID 추출
			String productId = extractProductId(link);

			Product product = new Product();
			product.setTitle(title);
			product.setPrice(price);
			product.setImage(imageUrl);
			product.setMallName("Coupang");
			product.setLink(link);
			product.setKeyword(query);
			product.setProductId(productId);

			return product;
		} catch (NoSuchElementException e) {
			log.warn("상품 정보 추출 실패: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * 상품 링크에서 고유한 상품 ID를 추출합니다.
	 *
	 * @param link 상품 링크
	 * @return 추출된 상품 ID
	 */
	private String extractProductId(String link) {
		try {
			return link.split("\\?")[0].split("/")[5];
		} catch (ArrayIndexOutOfBoundsException e) {
			log.warn("상품 ID 추출 실패: {}", e.getMessage());
			return "unknown";
		}
	}
}
