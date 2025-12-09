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

        System.err.println("? �ֹ� ��ȣ ��û: " + orderNumber);
        if (orderService.existsByOrderNumber(orderNumber)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("FEHLER: Order number " + orderNumber + " already exists in the database.");
        }

        String orderUrl = String.format(
                "https://dawayo.de/wp-json/wc/v3/orders/%s?consumer_key=%s&consumer_secret=%s",
                orderNumber, consumer_key, consumer_secret);

        System.err.println("? �ֹ� API ��û: " + orderUrl);

        HttpResponse<String> orderResponse = sendRequest(orderUrl);
        String orderBody = orderResponse.body();

        if (!isJson(orderBody)) {
            System.err.println("? JSON ���� �ƴ� (" + orderResponse.statusCode() + ")");
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
                    System.err.println("?? Line item ó�� �� ����: " + e.getMessage());
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        String resultJson = objectMapper.writeValueAsString(resultArray);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(resultJson);
    }

    /** �� ��ǰ�� �� ������ ó�� */
private ObjectNode processLineItem(Map<String, Object> item, String orderNumber)
        throws IOException, InterruptedException {

    ObjectNode itemNode = objectMapper.createObjectNode();
    String name = (String) item.get("name");
    int quantity = (int) item.getOrDefault("quantity", 0);
    String productId = String.valueOf(item.get("product_id"));

    // -------------------------
    // WooCommerce���� ��ǰ ���� ���� (incl)
    // -------------------------
    double price = 0.0;

    // ? ������� (meta_data)
    String MHD = "";
    List<Map<String, Object>> metaDataList = (List<Map<String, Object>>) item.get("meta_data");

    if (metaDataList != null) {
        for (Map<String, Object> meta : metaDataList) {
            String key = (String) meta.get("key");

            // �������
            if ("_wcxd_expiry_date".equals(key)) {
                MHD = (String) meta.get("display_value");
            }

            // ���� ���� incl
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

    // ? ��¥ ��ȯ (�ѱ��� �� ���Ͻ�)
    MHD = convertDateToGerman(MHD);

    // ? WooCommerce ��ǰ API ȣ��
    String productUrl = String.format(
            "https://dawayo.de/wp-json/wc/v3/products/%s?consumer_key=%s&consumer_secret=%s",
            productId, consumer_key, consumer_secret);

    HttpResponse<String> productResponse = sendRequest(productUrl);
    String productBody = productResponse.body();
    if (!isJson(productBody)) {
        System.err.println("? product JSON ���� �ƴ�: " + productId);
        return itemNode;
    }

    Map<String, Object> productMap = objectMapper.readValue(productBody, new TypeReference<>() {});


    // ? SKU �������� (meta_data > key = "custom_product_sku")
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

    // ? �̹��� URL
    String imageUrl = "";
    List<Map<String, Object>> images = (List<Map<String, Object>>) productMap.get("images");
    if (images != null && !images.isEmpty()) {
        imageUrl = String.valueOf(images.get(0).get("src"));
    }

    // ? �߰� ��Ÿ������ (��: expiredate)
    String expiredate = "";
    if (metaList != null) {
        for (Map<String, Object> meta : metaList) {
            if ("_j79_wcxd_sort_key".equals(meta.get("key"))) {
                expiredate = String.valueOf(meta.get("value"));
            }
        }
    }

    // ? ��� JSON ����
    itemNode.put("orderNumber", orderNumber);
    itemNode.put("name", name);
    itemNode.put("quantity", quantity);
    itemNode.put("MHD", MHD);
    itemNode.put("sku", sku);           // �� ������ �κ�
    itemNode.put("expiredate", expiredate);
    itemNode.put("price", price);
    itemNode.put("imageUrl", imageUrl);

    System.err.println(itemNode.toString());
    return itemNode;
}


    /** ��¥ ��ȯ (�ѱ��� �� ���Ͼ� ����) */
    private String convertDateToGerman(String MHD) {
        if (MHD == null || MHD.isBlank()) return "";
        try {
            SimpleDateFormat koreanFormat = new SimpleDateFormat("M�� d, yyyy", Locale.KOREAN);
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
        System.err.println("? API ��û: " + response.statusCode() + " (" + url + ")");
        return response;
    }

    @PostMapping("/saveScannedItems")
    public ResponseEntity<String> saveScannedItems(@RequestBody PackingRequestVO request) {
        List<PackingVO> scannedItems = request.getScannedItems();
        List<ScanErrorVO> scannedErrorItems = request.getScannedErrorItems();

        System.out.println("? ���� ������ ��: " + (scannedItems != null ? scannedItems.size() : 0));
        System.out.println("?? ���� ������ ��: " + (scannedErrorItems != null ? scannedErrorItems.size() : 0));

        if (scannedItems != null) orderService.saveAll(scannedItems);
        if (scannedErrorItems != null) orderService.saveAllError(scannedErrorItems);

        return ResponseEntity.ok("Scanned items received successfully");
    }

   @GetMapping("/updateProductList")
public void getMethodName() throws IOException, InterruptedException {
    int page = 1; // ������ ��ȣ �ʱ�ȭ
    int perPage = 100; // �� �������� 100�� ��ǰ ��������
    List<Map<String, Object>> allProducts = new ArrayList<>(); // ��� ��ǰ�� ������ ����Ʈ
System.err.println("�׽�Ʈ");
    // �ݺ����� ���� ���� �������� �����͸� ��� ��������
    // while (true) {
    //     String productUrl = String.format(
    //         "https://dawayo.de/wp-json/wc/v3/products/?consumer_key=%s&consumer_secret=%s&per_page=%d&page=%d",
    //         consumer_key, consumer_secret, perPage, page
    //     );
    //     System.err.println("? Produktliste aktualisieren API ��û: " + productUrl); 

    //     // API ��û �� ���� ó��
    //     try {
    //         HttpResponse<String> productResponse = sendRequest(productUrl); // API ��û
    //         String productBody = productResponse.body(); // ���� ����


    //         // ObjectMapper�� JSON �迭�� List<Map<String, Object>>�� ��ȯ
    //         ObjectMapper objectMapper = new ObjectMapper();
    //         List<Map<String, Object>> productList = objectMapper.readValue(productBody, new TypeReference<List<Map<String, Object>>>() {});

    //         // ���� ��ǰ�� ������ �ݺ� ����
    //         if (productList.isEmpty()) {
    //             break;
    //         }

    //         // ������ ��ǰ�� ��� ����Ʈ�� �߰�
    //         allProducts.addAll(productList);

    //         // ������ ��ȣ ����
    //         page++;
    //     } catch (IOException | InterruptedException e) {
    //         e.printStackTrace();
    //         break;  // ������ �߻��ϸ� �ݺ� ����
    //     }
    // }

    // // ��� ��ǰ�� ���
    // System.err.println("? �� ��ǰ ���: " + allProducts.size() + "�� ��ǰ");
    // for (Map<String, Object> product : allProducts) {
    //     String productName = (String) product.get("name"); // ��ǰ��
    //     String productPrice = (String) product.get("price"); // ���� (API���� ������ String���� ������)
    //     System.out.println("��ǰ��: " + productName + ", ����: " + productPrice);
    // }
}

@PostMapping("/productSearch")
@ResponseBody
public String productSearch(@RequestParam("query") String query)
        throws IOException, InterruptedException {

    System.err.println("request received: " + query);

    String url = String.format(
            "https://dawayo.de/wp-json/wc/v3/products?search=%s&consumer_key=%s&consumer_secret=%s",
            query, consumer_key, consumer_secret);

    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Basic " + Base64.getEncoder()
                    .encodeToString((consumer_key + ":" + consumer_secret).getBytes()))
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    System.err.println("API Response: " + response.body());

    return response.body();
}


}
