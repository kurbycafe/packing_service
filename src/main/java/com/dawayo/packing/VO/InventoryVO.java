package com.dawayo.packing.VO;

import lombok.Data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Data
@Getter
@Setter
@ToString

@Entity
@Table(name = "inventory")
public class InventoryVO {
	

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
private String productId; //woocommerce id 
    private String productName;
    
    private String productSku;
    private String productBarcode;
    private String boxBarcode;
    private String quantityperSingle; //개당 수량
    private String quantityperBox;//박스당 수량
    private String totalQuantity; //총 수량
    private String locationMain; //재고 위치
    private String locationSub; //재고 위치 세부
    private String mhd; //유통기한

}
