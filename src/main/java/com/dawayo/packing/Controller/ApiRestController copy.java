package com.dawayo.packing.Controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.aspectj.weaver.ast.Or;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.dawayo.packing.Service.OrderService;
import com.dawayo.packing.VO.PackingRequestVO;
import com.dawayo.packing.VO.PackingVO;
import com.dawayo.packing.VO.ScanErrorVO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
 
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiRestController {
    private final OrderService orderService;
    private final String consumer_key="ck_b2f69874352c6c35c49dff10d254a36986a2cc26";
    private final String consumer_secret="cs_e36d3ff7a86398ceb6885cac27b921c6b6707ce7";

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

    HttpClient client = HttpClient.newHttpClient();
    ObjectMapper objectMapper = new ObjectMapper();

    // 주문 정보 요청
    HttpResponse<String> orderResponse = sendRequest(client, orderUrl);
    Map<String, Object> orderMap = objectMapper.readValue(orderResponse.body(), new TypeReference<>() {});
    
    List<Map<String, Object>> lineItems = (List<Map<String, Object>>) orderMap.get("line_items");

    ArrayNode resultArray = objectMapper.createArrayNode();

    for (Map<String, Object> item : lineItems) {
        String name = (String) item.get("name");
        int quantity = (Integer) item.get("quantity");
        String productId = String.valueOf(item.get("product_id"));

        String productUrl = "https://dawayo.de/wp-json/wc/v3/products/" + productId +
                "?consumer_key=" + consumer_key +
                "&consumer_secret=" + consumer_secret;

        HttpResponse<String> productResponse = sendRequest(client, productUrl);
        Map<String, Object> productMap = objectMapper.readValue(productResponse.body(), new TypeReference<>() {});

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

            String sku = "";
            List<Map<String, Object>> metaList = (List<Map<String, Object>>) productMap.get("meta_data");
            for (Map<String, Object> meta : metaList) {
                if ("custom_product_sku".equals(meta.get("key"))) {
                    sku = (String) meta.get("value");
                    break;
                }
            }
            itemNode.put("sku", sku);
        }

        resultArray.add(itemNode);
    }

    String resultJson = objectMapper.writeValueAsString(resultArray);
    return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(resultJson);
}

// 공통 요청 메서드 분리
private HttpResponse<String> sendRequest(HttpClient client, String url) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
}

    @PostMapping("/saveScannedItems")
public ResponseEntity<String> saveScannedItems(@RequestBody PackingRequestVO request) {

    List<PackingVO> scannedItems = request.getScannedItems();
    List<ScanErrorVO> scannedErrorItems = request.getScannedErrorItems();

    System.out.println("✅ 스캔된 정상 아이템 수: " + scannedItems);
    System.out.println("⚠️ 스캔 오류 아이템 수: " + scannedErrorItems);

   
    for (PackingVO item : scannedItems) {
        System.err.println("Received item: " + item.toString());
    }
    //save the scanned items to the database
    orderService.saveAll(scannedItems);
    //save the scanned error items to the database
    orderService.saveAllError(scannedErrorItems);
    return ResponseEntity.ok("Scanned items received successfully");
}

}