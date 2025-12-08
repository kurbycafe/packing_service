package com.dawayo.packing.Service;


import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.dawayo.packing.Repository.HistoryRepository;
import com.dawayo.packing.VO.PackingVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final HistoryRepository historyRepository;

    public  Page<PackingVO> getPackingList(Pageable pageable) {
       return historyRepository.findPackingList(pageable);
    }

    public List<PackingVO> getPackingById(String id) {
        return historyRepository.getPackingById(id);
    }
    


}
