package com.dawayo.packing.VO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "product_batch_table")
public class ProductBatchVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private ProductVO product;

    private LocalDate expiryDate; // 유통기한
    private Integer quantity;     // 해당 유통기한 재고

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
