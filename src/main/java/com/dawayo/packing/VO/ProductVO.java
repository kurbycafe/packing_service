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

   private Long wooId;
    private String name;
    private String sku;
    private String customSku;
    private Integer totalStock;
    private String price;
    private String salePrice;
    private String imageUrl; 

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
