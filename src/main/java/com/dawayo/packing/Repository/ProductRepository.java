package com.dawayo.packing.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dawayo.packing.VO.ProductVO;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductVO, Long> {
    
    // WooCommerce ID로 상품 조회 (동기화 시 사용)
    Optional<ProductVO> findByWooId(Long wooId);

    /**
     * 상품명(name) 또는 SKU로 검색하는 쿼리
     * FETCH JOIN을 사용하여 N+1 문제를 방지하고 성능을 최적화합니다.
     */
    @Query("""
        SELECT DISTINCT p
        FROM ProductVO p
        WHERE p.name LIKE %:query% 
        OR p.sku LIKE %:query%
        OR p.customSku LIKE %:query%
    """)
    List<ProductVO> searchProducts(@Param("query") String query);

    /**
     * 엑셀 출력 등을 위해 전체 데이터를 가져올 때, 
     * Batch 정보까지 한 번에 긁어오도록 정의 (성능 최적화용)
     */
    @Query("SELECT DISTINCT p FROM ProductVO p")
    List<ProductVO> findAllWithBatches();
}