package com.dawayo.packing.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dawayo.packing.VO.ProductVO;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductVO, Long> {
    Optional<ProductVO> findByWooId(Long wooId);
}
