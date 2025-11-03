package com.dawayo.packing.Controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.dawayo.packing.Service.AdminHomeService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomeController {

    private final AdminHomeService adminHomeService;
    // 관리자 홈 페이지로 이동
    @RequestMapping("/home")
    public String adminHome() {
        //get number of packed items by today, this week, this month and return to the view
        Map<String, Long> packingStats = adminHomeService.getPackingStats();
System.err.println("📊 Packing Stats: " + packingStats);

        // 관리자 홈 페이지로 이동
        return "admin/adminHome"; // adminHome.html로 이동
    }
    
}
