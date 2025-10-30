package com.dawayo.packing.Controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    @PostMapping("/getOrderDetail")
    @ResponseBody
    public ResponseEntity<String> getOrderDetail(@RequestParam("orderNumber") String orderNumber) throws IOException, InterruptedException {

        if (orderService.existsByOrderNumber(orderNumber)) {
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("FEHLER: Order number " + orderNumber + " already exists in the database.");
        }

        String orderUrl = "https://dawayo.de/wp-json/wc/v3/orders/" + orderNumber +
                "?consumer_key=" + consumer_key +
                "&consumer_secret=" + consumer_secret;

        System.err.println("📦 주문 API 요청: " + orderUrl);

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        // 주문 정보 요청
        HttpResponse<String> orderResponse = sendRequest(client, orderUrl);
        String orderBody = orderResponse.body();

        // JSON 형식이 아닌 경우 에러 로그 출력
        if (!orderBody.trim().startsWith("{") && !orderBody.trim().startsWith("[")) {
            System.err.println("❌ JSON no (status " + orderResponse.statusCode() + ")");
            System.err.println(orderBody.substring(0, Math.min(orderBody.length(), 500)));
            return ResponseEntity.status(500).body("WooCommerce returned invalid response (not JSON)");
        }

        Map<String, Object> orderMap = objectMapper.readValue(orderBody, new TypeReference<>() {});
        List<Map<String, Object>> lineItems = (List<Map<String, Object>>) orderMap.get("line_items");

        ArrayNode resultArray = objectMapper.createArrayNode();

        for (Map<String, Object> item : lineItems) {

            String name = (String) item.get("name");
            int quantity = (Integer) item.get("quantity");
            String productId = String.valueOf(item.get("product_id"));

            String MHD = "";

            List<Map<String, Object>> metaDataList = (List<Map<String, Object>>) item.get("meta_data");
            if (metaDataList != null) {
                for (Map<String, Object> meta : metaDataList) {
                    if ("_wcxd_expiry_date".equals(meta.get("key"))) {
                        MHD = (String) meta.get("display_value");
                        break;
                    }
                }
            }

            try {
                // 한국어 월 이름 인식
                SimpleDateFormat koreanFormat = new SimpleDateFormat("M월 d, yyyy", Locale.KOREAN);
                Date date = koreanFormat.parse(MHD);

                // 독일식 포맷으로 변환
                SimpleDateFormat germanFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN);
                MHD = germanFormat.format(date);

            } catch (Exception e) {
                System.err.println("⚠️ date parsing fail : " + e.getMessage());
            }

            System.err.println("MHD deusch: " + MHD);

            String productUrl = "https://dawayo.de/wp-json/wc/v3/products/" + productId +
                    "?consumer_key=" + consumer_key +
                    "&consumer_secret=" + consumer_secret;


            HttpResponse<String> productResponse = sendRequest(client, productUrl);
            String productBody = productResponse.body();

            if (!productBody.trim().startsWith("{") && !productBody.trim().startsWith("[")) {

                System.err.println(productBody.substring(0, Math.min(productBody.length(), 500)));
                continue;
            }

            Map<String, Object> productMap = objectMapper.readValue(productBody, new TypeReference<>() {});
            ObjectNode itemNode = objectMapper.createObjectNode();
            itemNode.put("orderNumber", orderNumber);

            // 상품 정보 예외 처리
            if (productMap.containsKey("code") && "woocommerce_rest_product_invalid_id".equals(productMap.get("code"))) {
                itemNode.put("name", "unknown");
                itemNode.put("quantity", "unknown");
                itemNode.put("sku", "unknown");
            } else {
                itemNode.put("name", name);
                itemNode.put("quantity", quantity);
                itemNode.put("MHD", MHD);

                String sku = "";
                String expiredate = "";
                List<Map<String, Object>> metaList = (List<Map<String, Object>>) productMap.get("meta_data");

                
                if (metaList != null) {
                    for (Map<String, Object> meta : metaList) {
                        if ("custom_product_sku".equals(meta.get("key"))) {
                            sku = (String) meta.get("value");
                            ;
                        }
                        if ("_j79_wcxd_sort_key".equals(meta.get("key"))) {
                            expiredate = (String) meta.get("value");
                            
                        }
                    }
                }
            System.err.println("SKU: " + sku + ", Expire Date: " + expiredate);
                itemNode.put("sku", sku);
                itemNode.put("expiredate", expiredate);
            }

            resultArray.add(itemNode);
        }

        String resultJson = objectMapper.writeValueAsString(resultArray);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(resultJson);
    }

    // 공통 요청 메서드 분리 (헤더 포함)
    private HttpResponse<String> sendRequest(HttpClient client, String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                // WooCommerce API가 JSON으로 응답하도록 명시
                .header("Accept", "application/json")
                // 봇 차단 우회 (브라우저 유사 헤더)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) DawayoPackingClient/1.0")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 상태코드 로그로 확인
        System.err.println("🌐 요청 결과: " + response.statusCode() + " (" + url + ")");
        return response;
    }

    @PostMapping("/saveScannedItems")
    public ResponseEntity<String> saveScannedItems(@RequestBody PackingRequestVO request) {

        List<PackingVO> scannedItems = request.getScannedItems();
        System.err.println(scannedItems);
        List<ScanErrorVO> scannedErrorItems = request.getScannedErrorItems();

        System.out.println("✅ 스캔된 정상 아이템 수: " + (scannedItems != null ? scannedItems.size() : 0));
        System.out.println("⚠️ 스캔 오류 아이템 수: " + (scannedErrorItems != null ? scannedErrorItems.size() : 0));

        if (scannedItems != null) {
            for (PackingVO item : scannedItems) {
                System.err.println("📦 Received item: " + item.toString());
            }
            orderService.saveAll(scannedItems);
        }

        if (scannedErrorItems != null) {
            orderService.saveAllError(scannedErrorItems);
        }

        return ResponseEntity.ok("Scanned items received successfully");
    }
}
