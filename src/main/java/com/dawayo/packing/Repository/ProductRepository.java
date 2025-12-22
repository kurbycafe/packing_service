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
    Optional<ProductVO> findByWooId(Long wooId);

    @Query("""
        SELECT DISTINCT p
        FROM ProductVO p
        LEFT JOIN FETCH p.batches b
        WHERE p.name LIKE %:query%
    """)
    List<ProductVO> searchProducts(@Param("query") String query);



}
