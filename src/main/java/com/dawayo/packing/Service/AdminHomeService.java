package com.dawayo.packing.Service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.dawayo.packing.Repository.AdminHomeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminHomeService {

    private final AdminHomeRepository adminHomeRepository;

    public Map<String, Long> getPackingStats() {
        Long todayCount = adminHomeRepository.countPackedItemsToday();
        Long weekCount = adminHomeRepository.countPackedItemsThisWeek();
        Long monthCount = adminHomeRepository.countPackedItemsThisMonth();

        return Map.of(
                "today", todayCount,
                "week", weekCount,
                "month", monthCount
        );
    }

    
}
