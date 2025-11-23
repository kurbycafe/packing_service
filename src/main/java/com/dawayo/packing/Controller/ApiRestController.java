package com.dawayo.packing.Controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dawayo.packing.Service.OrderService;
import com.dawayo.packing.VO.PackingRequestVO;
import com.dawayo.packing.VO.PackingVO;
import com.dawayo.packing.VO.ScanErrorVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiRestController {

    private final OrderService orderService;
    private final String consumer_key = "ck_b2f69874352c6c35c49dff10d254a36986a2cc26";
    private final String consumer_secret = "cs_e36d3ff7a86398ceb6885cac27b921c6b6707ce7";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    @PostMapping("/getOrderDetail")
    @ResponseBody
    public ResponseEntity<String> getOrderDetail(@RequestParam("orderNumber") String orderNumber)
            throws IOException, InterruptedException {

        System.err.println("? 주문 번호 요청: " + orderNumber);
        if (orderService.existsByOrderNumber(orderNumber)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("FEHLER: Order number " + orderNumber + " already exists in the database.");
        }

        String orderUrl = String.format(
                "https://dawayo.de/wp-json/wc/v3/orders/%s?consumer_key=%s&consumer_secret=%s",
                orderNumber, consumer_key, consumer_secret);

        System.err.println("? 주문 API 요청: " + orderUrl);

        HttpResponse<String> orderResponse = sendRequest(orderUrl);
        String orderBody = orderResponse.body();

        if (!isJson(orderBody)) {
            System.err.println("? JSON 형식 아님 (" + orderResponse.statusCode() + ")");
            return ResponseEntity.status(500).body("WooCommerce returned invalid response (not JSON)");
        }

        Map<String, Object> orderMap = objectMapper.readValue(orderBody, new TypeReference<>() {});
        List<Map<String, Object>> lineItems = (List<Map<String, Object>>) orderMap.get("line_items");
        if (lineItems == null || lineItems.isEmpty()) {
            return ResponseEntity.status(404).body("No line items found for order " + orderNumber);
        }

        ArrayNode resultArray = objectMapper.createArrayNode();
        ExecutorService executor = Executors.newFixedThreadPool(5);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map<String, Object> item : lineItems) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    ObjectNode itemNode = processLineItem(item, orderNumber);
                    synchronized (resultArray) {
                        resultArray.add(itemNode);
                    }
                } catch (Exception e) {
                    System.err.println("?? Line item 처리 중 오류: " + e.getMessage());
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        String resultJson = objectMapper.writeValueAsString(resultArray);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultJson);
    }

    /** 각 상품별 상세 데이터 처리 */
private ObjectNode processLineItem(Map<String, Object> item, String orderNumber)
        throws IOException, InterruptedException {

    ObjectNode itemNode = objectMapper.createObjectNode();
    String name = (String) item.get("name");
    int quantity = (int) item.getOrDefault("quantity", 0);
    String productId = String.valueOf(item.get("product_id"));

    // -------------------------
    // WooCommerce에서 상품 개별 가격 (incl)
    // -------------------------
    double price = 0.0;

    // ? 유통기한 (meta_data)
    String MHD = "";
    List<Map<String, Object>> metaDataList = (List<Map<String, Object>>) item.get("meta_data");

    if (metaDataList != null) {
        for (Map<String, Object> meta : metaDataList) {
            String key = (String) meta.get("key");

            // 유통기한
            if ("_wcxd_expiry_date".equals(key)) {
                MHD = (String) meta.get("display_value");
            }

            // 개별 가격 incl
            if ("_wcpdf_regular_price".equals(key)) {
                Map<String, Object> priceMap = (Map<String, Object>) meta.get("value");
                if (priceMap != null && priceMap.get("incl") != null) {
                    try {
                        price = Double.parseDouble(String.valueOf(priceMap.get("incl")));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    // ? 날짜 변환 (한국어 → 독일식)
    MHD = convertDateToGerman(MHD);

    // ? WooCommerce 상품 API 호출
    String productUrl = String.format(
            "https://dawayo.de/wp-json/wc/v3/products/%s?consumer_key=%s&consumer_secret=%s",
            productId, consumer_key, consumer_secret);

    HttpResponse<String> productResponse = sendRequest(productUrl);
    String productBody = productResponse.body();
    if (!isJson(productBody)) {
        System.err.println("? product JSON 형식 아님: " + productId);
        return itemNode;
    }

    Map<String, Object> productMap = objectMapper.readValue(productBody, new TypeReference<>() {});


    // ? SKU 가져오기 (meta_data > key = "custom_product_sku")
    String sku = "";
    List<Map<String, Object>> metaList = (List<Map<String, Object>>) productMap.get("meta_data");
    if (metaList != null) {
        for (Map<String, Object> meta : metaList) {
            if ("custom_product_sku".equals(meta.get("key"))) {
                sku = String.valueOf(meta.get("value"));
                break;
            }
        }
    }

    // ? 이미지 URL
    String imageUrl = "";
    List<Map<String, Object>> images = (List<Map<String, Object>>) productMap.get("images");
    if (images != null && !images.isEmpty()) {
        imageUrl = String.valueOf(images.get(0).get("src"));
    }

    // ? 추가 메타데이터 (예: expiredate)
    String expiredate = "";
    if (metaList != null) {
        for (Map<String, Object> meta : metaList) {
            if ("_j79_wcxd_sort_key".equals(meta.get("key"))) {
                expiredate = String.valueOf(meta.get("value"));
            }
        }
    }

    // ? 결과 JSON 구성
    itemNode.put("orderNumber", orderNumber);
    itemNode.put("name", name);
    itemNode.put("quantity", quantity);
    itemNode.put("MHD", MHD);
    itemNode.put("sku", sku);           // ← 수정된 부분
    itemNode.put("expiredate", expiredate);
    itemNode.put("price", price);
    itemNode.put("imageUrl", imageUrl);

    System.err.println(itemNode.toString());
    return itemNode;
}


    /** 날짜 변환 (한국어 → 독일어 형식) */
    private String convertDateToGerman(String MHD) {
        if (MHD == null || MHD.isBlank()) return "";
        try {
            SimpleDateFormat koreanFormat = new SimpleDateFormat("M월 d, yyyy", Locale.KOREAN);
            Date date = koreanFormat.parse(MHD);
            SimpleDateFormat germanFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
            return germanFormat.format(date);
        } catch (Exception e) {
            System.err.println("?? Date parsing fail: " + MHD);
            return MHD;
        }
    }

    private boolean isJson(String body) {
        return body != null && (body.trim().startsWith("{") || body.trim().startsWith("["));
    }

    private HttpResponse<String> sendRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 DawayoPackingClient/2.0")
                .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.err.println("? API 요청: " + response.statusCode() + " (" + url + ")");
        return response;
    }

    @PostMapping("/saveScannedItems")
    public ResponseEntity<String> saveScannedItems(@RequestBody PackingRequestVO request) {
        List<PackingVO> scannedItems = request.getScannedItems();
        List<ScanErrorVO> scannedErrorItems = request.getScannedErrorItems();

        System.out.println("? 정상 아이템 수: " + (scannedItems != null ? scannedItems.size() : 0));
        System.out.println("?? 오류 아이템 수: " + (scannedErrorItems != null ? scannedErrorItems.size() : 0));

        if (scannedItems != null) orderService.saveAll(scannedItems);
        if (scannedErrorItems != null) orderService.saveAllError(scannedErrorItems);

        return ResponseEntity.ok("Scanned items received successfully");
    }

   @GetMapping("/updateProductList")
public void getMethodName() throws IOException, InterruptedException {
    int page = 1; // 페이지 번호 초기화
    int perPage = 100; // 한 페이지에 100개 상품 가져오기
    List<Map<String, Object>> allProducts = new ArrayList<>(); // 모든 상품을 저장할 리스트
System.err.println("테스트");
    // 반복문을 통해 여러 페이지의 데이터를 모두 가져오기
    // while (true) {
    //     String productUrl = String.format(
    //         "https://dawayo.de/wp-json/wc/v3/products/?consumer_key=%s&consumer_secret=%s&per_page=%d&page=%d",
    //         consumer_key, consumer_secret, perPage, page
    //     );
    //     System.err.println("? Produktliste aktualisieren API 요청: " + productUrl); 

    //     // API 요청 및 응답 처리
    //     try {
    //         HttpResponse<String> productResponse = sendRequest(productUrl); // API 요청
    //         String productBody = productResponse.body(); // 응답 본문


    //         // ObjectMapper로 JSON 배열을 List<Map<String, Object>>로 변환
    //         ObjectMapper objectMapper = new ObjectMapper();
    //         List<Map<String, Object>> productList = objectMapper.readValue(productBody, new TypeReference<List<Map<String, Object>>>() {});

    //         // 만약 상품이 없으면 반복 종료
    //         if (productList.isEmpty()) {
    //             break;
    //         }

    //         // 가져온 상품을 모든 리스트에 추가
    //         allProducts.addAll(productList);

    //         // 페이지 번호 증가
    //         page++;
    //     } catch (IOException | InterruptedException e) {
    //         e.printStackTrace();
    //         break;  // 에러가 발생하면 반복 종료
    //     }
    // }

    // // 모든 상품을 출력
    // System.err.println("? 총 상품 목록: " + allProducts.size() + "개 상품");
    // for (Map<String, Object> product : allProducts) {
    //     String productName = (String) product.get("name"); // 상품명
    //     String productPrice = (String) product.get("price"); // 가격 (API에서 가격은 String으로 제공됨)
    //     System.out.println("상품명: " + productName + ", 가격: " + productPrice);
    // }
}
}
