package com.dawayo.packing.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.dawayo.packing.VO.ProductBatchVO;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatchVO, Long> {
}