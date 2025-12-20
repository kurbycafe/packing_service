package com.dawayo.packing.VO;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "product_table")
public class ProductVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long wooId;           // WooCommerce 상품 ID
    private String name;          // 상품명
    private String sku;           // WooCommerce SKU
    private String customSku;     // custom_product_sku
    private Integer totalStock;   // 전체 재고 합계
    private String price;         // 가격 - 원래
    private String salePrice;     // 가격 - 세일가

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 연관된 유통기한 재고
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductBatchVO> batches = new ArrayList<>();
}
