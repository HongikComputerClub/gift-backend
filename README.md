# PresenTalk - Backend Server

> **AI 기반 맞춤형 선물 추천 및 멀티 채널 상품 수집 서버**
> *Java 21 & Spring Boot 3.4 기반의 고성능 데이터 파이프라인 저장소*

## Project Overview
**PresenTalk Backend**는 카카오톡 대화 분석을 통해 사용자 취향을 도출하고, **Naver·Coupang·KREAM** 등 파편화된 커머스 플랫폼의 상품 데이터를 통합(Aggregation)하여 최적의 선물을 추천하는 시스템입니다.
공식 API가 없는 플랫폼의 데이터를 수집하기 위해 **Selenium 기반의 동적 크롤링**을 구현하였으며, 대용량 트래픽 제어를 위한 **자체 Rate Limiter**와 **HMAC 보안 모듈**을 구축하여 안정적인 데이터 파이프라인을 확보했습니다.

<br>

## System Architecture
> **전체 시스템의 데이터 흐름과 하이브리드 수집 구조도입니다.**

<img src="https://github.com/user-attachments/assets/095c4d38-4432-4b79-ba54-d3bd2d553cd0" width="100%" alt="System Architecture Diagram">

<br>

## Tech Stack

| Category | Stack | Version / Details |
| :--- | :--- | :--- |
| **Language** | Java | **JDK 21 LTS** |
| **Framework** | Spring Boot | **3.4.1** (Latest) |
| **Data Collection** | Selenium, WebClient | **Hybrid Scraping (Dynamic & API)** |
| **Security** | Spring Security, HMAC | JWT & **Custom Signature Generation** |
| **Concurrency** | Synchronized, Deque | **Custom Rate Limiter (Sliding Window)** |
| **Database** | MySQL, JPA | Data Persistence |
| **Docs** | Swagger (OpenAPI) | API Documentation |

<br>

## Key Features & Implementation Depth

### 1. 하이브리드 데이터 수집 (Hybrid Data Collection)
* **WebClient (Non-blocking):** 공식 API가 제공되는 **네이버 쇼핑**은 `WebClient`를 사용하여 비동기 논블로킹 방식으로 리소스를 효율적으로 사용하며 대량의 데이터를 수집합니다.
* **Selenium (Browser Automation):** API 접근이 제한적인 **쿠팡(Coupang)**과 **크림(KREAM)**은 `Selenium WebDriver`를 활용하여 동적 렌더링(Dynamic Rendering)되는 페이지의 가격, 이미지, 상품 정보를 정확하게 추출합니다.

### 2. 커스텀 Rate Limiter 구현 (Custom Rate Limiter)
* **Problem:** 쿠팡 파트너스 API의 엄격한 호출 제한(**분당 100회**)으로 인해, 트래픽 급증 시 API 호출 실패 및 파트너 자격 정지 위험이 존재했습니다.
* **Solution:** 외부 라이브러리(Bucket4j 등)에 의존하지 않고, **`Deque<Long>`와 `synchronized` 블록을 활용한 Sliding Window 알고리즘**을 직접 구현했습니다.
    * 1분(60,000ms) 윈도우 내의 호출 타임스탬프를 실시간으로 추적하여 정확히 100회 제한을 준수하며, 초과 시 남은 시간만큼 스레드를 대기(Wait)시킨 후 재요청하도록 설계하여 **API 성공률 100%**를 달성했습니다.

### 3. HMAC 보안 서명 직접 구현 (Security Engineering)
* **HMAC-SHA256 Signature:** 쿠팡 파트너스 API 인증에 필요한 서명 생성 로직을 `javax.crypto.Mac`을 사용하여 직접 구현했습니다.
* API 요청 시점의 타임스탬프와 Access Key, Secret Key를 조합하여 위변조가 불가능한 보안 헤더를 생성, 외부 통신 보안을 강화했습니다.

### 4. 데이터 정합성 관리 (Upsert Optimization)
* **Product Service:** 동일 상품의 중복 저장을 방지하기 위해, 상품 ID(ProductId) 기준으로 **조회 후 업데이트(Upsert)** 로직을 적용했습니다.
* 트랜잭션(`@Transactional`) 범위 내에서 대량의 크롤링 데이터 저장 시 데이터 일관성을 보장합니다.

<br>

## 📂 Folder Structure

```bash
src/main/java/com/team4/giftidea
├── 📂 config          # Global Configuration (Security, Selenium, Swagger)
├── 📂 controller      # API Entry Points (REST Controller)
├── 📂 dto             # Data Transfer Objects (Request/Response)
├── 📂 entity          # JPA Entities (DB Tables)
├── 📂 repository      # Data Access Layer (Spring Data JPA)
└── 📂 service         # Business Logic Layer
    ├── CoupangApiService.java      # Selenium 기반 동적 크롤링
    ├── KreamApiService.java        # 리셀 플랫폼 데이터 수집
    ├── NaverApiService.java        # WebClient 기반 비동기 API 연동
    ├── CoupangPartnersService.java # Rate Limiter & HMAC 서명 구현
    └── ProductService.java         # 상품 Upsert 및 트랜잭션 관리
