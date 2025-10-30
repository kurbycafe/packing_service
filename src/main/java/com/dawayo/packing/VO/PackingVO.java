package com.dawayo.packing.VO;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Entity
@Table(name = "packing_list")
//패킹 리스트 
public class PackingVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long packerId;

    private String orderNumber;
    private String mhd;

    private String productName;
    private String productSku;
    private String scannedCount;
    private String productQ;


    private boolean isPacked = true;
    //give default date and time
    private String packingDate = LocalDate.now().toString();
    private String packingTime = LocalTime.now().toString();




}