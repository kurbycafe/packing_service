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

    @Query(value = "SELECT p.name, p.sku FROM product_table p WHERE p.name LIKE CONCAT('%', :query, '%') LIMIT 10", nativeQuery = true)
    List<Object[]> searchProductsNative(@Param("query") String query);



}
