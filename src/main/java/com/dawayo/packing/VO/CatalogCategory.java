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
@Table(name = "catalog_cat_table")

public class CatalogCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 예: "여름 신상품", "베스트셀러"
    private Integer sortOrder; // 아코디언에서 보여질 카테고리 순서

    // 카테고리가 삭제되면 안에 맵핑된 아이템들도 삭제되도록 Cascade 설정
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CatalogItem> items = new ArrayList<>();
}
