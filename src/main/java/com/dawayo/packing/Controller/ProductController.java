package com.dawayo.packing.Controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.*;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.dawayo.packing.VO.ProductVO;
import com.dawayo.packing.VO.ProductBatchVO;
import com.dawayo.packing.Service.ProductService;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    @Value("${woocommerce.base-url}")
    private String baseUrl;

    @Value("${woocommerce.consumer-key}")
    private String consumerKey;

    @Value("${woocommerce.consumer-secret}")
    private String consumerSecret;

    private final ProductService productService;

    @GetMapping("/updateProductList")
    public void updateProductList() throws IOException, InterruptedException {

        int perPage = 100;
        int page = 1;

        ObjectMapper objectMapper = new ObjectMapper();
        HttpClient httpClient = HttpClient.newHttpClient();

        while (true) {

            String url = String.format(
                "%s?consumer_key=%s&consumer_secret=%s&per_page=%d&page=%d",
                baseUrl, consumerKey, consumerSecret, perPage, page
            );

            System.out.println("Fetching page: " + page);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("API ERROR: " + response.statusCode());
                break;
            }

            List<Map<String, Object>> productList =
                    objectMapper.readValue(response.body(), new TypeReference<>() {});

            // 더 이상 데이터가 없으면 종료
            if (productList.isEmpty()) {
                break;
            }

            for (Map<String, Object> productMap : productList) {

                ProductVO product = new ProductVO();
                product.setWooId(Long.valueOf(String.valueOf(productMap.get("id"))));
                product.setName(String.valueOf(productMap.get("name")));
                product.setSku(String.valueOf(productMap.get("sku")));

                // 가격 처리
                String taxClass = String.valueOf(productMap.get("tax_class"));
                double taxRate = getTaxRate(taxClass);

                double regularPrice = parsePrice(productMap.get("regular_price")) * taxRate;
                double salePrice = parsePrice(productMap.get("sale_price")) * taxRate;

                product.setPrice(String.format("%.2f", regularPrice));
                product.setSalePrice(String.format("%.2f", salePrice));

                // batch 처리
                List<ProductBatchVO> batches = new ArrayList<>();

                List<Map<String, Object>> metaData =
                        (List<Map<String, Object>>) productMap.get("meta_data");

                if (metaData != null) {
                    for (Map<String, Object> meta : metaData) {

                        String key = String.valueOf(meta.get("key"));
                        String value = String.valueOf(meta.get("value"));

                        if ("custom_product_sku".equals(key)) {
                            product.setCustomSku(value);
                        }

                        if ("_j79_wcxd_expiries".equals(key) && value != null && !value.isEmpty()) {

                            String[] entries = value.split(",");

                            for (String entry : entries) {
                                String[] parts = entry.trim().split("x");

                                if (parts.length == 2) {
                                    ProductBatchVO batch = new ProductBatchVO();
                                    batch.setExpiryDate(LocalDate.parse(parts[0].trim()));
                                    batch.setQuantity(Integer.parseInt(parts[1].trim()));
                                    batches.add(batch);
                                }
                            }
                        }
                    }
                }

                productService.saveOrUpdateProduct(product, batches);
            }

            page++;
        }

        System.out.println("WooCommerce 상품 동기화 완료");
    }

    private double parsePrice(Object priceObj) {
        try {
            return Double.parseDouble(String.valueOf(priceObj));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getTaxRate(String taxClass) {
        if (taxClass == null || taxClass.isEmpty() || "0".equals(taxClass)) {
            return 1.0;
        }
        return switch (taxClass) {
            case "7" -> 1.07;
            case "19" -> 1.19;
            default -> 1.0;
        };
    }


    // product_table 에서 비동기로 상품 검색 ,ajax 로 

   @PostMapping("/productSearch")
    @ResponseBody
    public List<Map<String, String>> productSearch(@RequestParam("query") String query) {

        List<Map<String, String>> results = productService.searchProducts(query);
System.err.println(results.toString());
 
        return results;
    }

    
}
