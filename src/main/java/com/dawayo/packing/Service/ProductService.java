package com.dawayo.packing.Service;

import java.time.LocalDateTime;
import java.util.List;

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
}
