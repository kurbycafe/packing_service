package com.dawayo.packing.Controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.dawayo.packing.Service.HistoryService;
import com.dawayo.packing.VO.PackingVO;

import org.springframework.ui.Model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Controller
@RequiredArgsConstructor
@RequestMapping("/history")
public class HistoryController {
    //get mapping for /history/list and return list of packing list records

    private final HistoryService historyService;
    
    @GetMapping("/list")
    public String getPackingList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<PackingVO> packingList = historyService.getPackingList(pageable);

        model.addAttribute("packingList", packingList);

        return "history/list";
    }
    
    @GetMapping("/view")
    public String detail(@RequestParam String id, Model model) {
        List<PackingVO> packing = historyService.getPackingById(id);
        System.err.println(packing
    );
        model.addAttribute("packing", packing);

        System.err.println(id);
        return "history/view";
    }
    
}
