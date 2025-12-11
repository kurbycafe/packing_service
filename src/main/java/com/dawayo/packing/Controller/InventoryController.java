package com.dawayo.packing.Controller;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
@RequiredArgsConstructor
@RequestMapping("/inventory")
public class InventoryController {

   @GetMapping("/list")
   public String inventoryList() {
    System.err.println("parmas received");
    return "inventory/list";
   }
   
   @GetMapping("/add")
   public String getMethodName() {
       return "inventory/add";
   }
   

} 