package com.dawayo.packing.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dawayo.packing.VO.ProductVO;
import com.dawayo.packing.VO.ProductBatchVO;
import com.dawayo.packing.Repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public void saveOrUpdateProduct(ProductVO product, List<ProductBatchVO> batches) {
        LocalDateTime now = LocalDateTime.now();

        ProductVO existing = productRepository.findByWooId(product.getWooId()).orElse(null);

        if (existing == null) {
            product.setCreatedAt(now);
            product.setUpdatedAt(now);
        } else {
            existing.setName(product.getName());
            existing.setSku(product.getSku());
            existing.setCustomSku(product.getCustomSku());
            existing.setPrice(product.getPrice());
            existing.setSalePrice(product.getSalePrice());
            existing.setUpdatedAt(now);

            existing.getBatches().clear(); // 기존 batch 삭제
            product = existing;
        }

        for (ProductBatchVO batch : batches) {
            batch.setProduct(product);
            batch.setCreatedAt(now);
            batch.setUpdatedAt(now);
            product.getBatches().add(batch);
        }

        int totalStock = batches.stream().mapToInt(ProductBatchVO::getQuantity).sum();
        product.setTotalStock(totalStock);

        productRepository.save(product); // cascade로 batch도 저장
    }

    public List<Map<String, String>> searchProducts(String query) {

    List<ProductVO> products = productRepository.searchProducts(query);


    return products.stream().map(p -> {

        Map<String, String> m = new HashMap<>();

        m.put("name", p.getName());
        m.put("sku", p.getSku());
        m.put("price", p.getPrice());
        m.put("salePrice", p.getSalePrice());
        m.put("wooId", String.valueOf(p.getWooId()));
        m.put("customSku", p.getCustomSku());
        
        // 가장 빠른 유통기한 하나만
        p.getBatches().stream()
            .map(ProductBatchVO::getExpiryDate)
            .min(LocalDate::compareTo)
            .ifPresent(d -> m.put("expiryDate", d.toString()));

        return m;
    }).toList();
}

}
