package com.dawayo.packing.Controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


import com.dawayo.packing.Service.UserService;
import com.dawayo.packing.VO.PackingVO;
import com.dawayo.packing.VO.UserVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final UserService userService;

    @GetMapping("/")
    public String home(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(true); 
        if (session.getAttribute("id") != null) {
            System.err.println("Session ID: " + session.getAttribute("id"));
            model.addAttribute("userId", session.getAttribute("id"));
        }
        return "home"; 
    }

    
    @GetMapping("/login")
    public String login(Model model){
        model.addAttribute("userVO", new UserVO());

        return "login"; 
    }

    @PostMapping("/loginProcess")
public String loginPost(UserVO userVO, HttpServletRequest request, HttpServletResponse response) {
    System.err.println("Login attempt with user: " + userVO.getUserid());
    UserVO foundUser = userService.login(userVO);
    if (foundUser != null) {
        System.err.println("Login successful for user: " + foundUser.getUserid());
        System.err.println("User details: " + foundUser.toString());
        HttpSession session = request.getSession(true); 
        if(foundUser.getUserid()=="admin" && foundUser.getPassword()=="admin") {
            System.err.println("Admin user logged in");
            session.setAttribute("id", foundUser.getId());
            return "redirect:/admin"; // Redirect to admin page if admin credentials are used
        }
       else{

        session.setAttribute("id", foundUser.getId()); 
        return "redirect:/"; 
       }
        

    }
    System.err.println("Login failed for user: " + userVO.getUserid());

    
    return "redirect:/";
}

@PostMapping("/logout")
public String logout(HttpServletRequest request) {
    HttpSession session = request.getSession(false); 
    if (session != null) {
        session.invalidate(); 
        System.err.println("Session invalidated, user logged out.");
    }
    return "redirect:/"; 
}


    @GetMapping("/mypage")
    public String myPage(HttpServletRequest req, Model model) {
    HttpSession session = req.getSession(false);
    String sessionId = session.getAttribute("id").toString();
    System.err.println(sessionId);

    List<PackingVO> packingList = userService.getPackingList(sessionId);

    if (!packingList.isEmpty()) {
        

        //날짜 데이터만
        // 날짜별로 그룹화 데이터는 날짜랑 주문번호로만

        Map<Object, List<Object>> groupedByDate = packingList.stream()
        .collect(Collectors.groupingBy(p -> p.getPackingDate(), Collectors.mapping(p -> p.getOrderNumber(), Collectors.toList())));
        System.err.println("groupedByDate: " + groupedByDate);
        model.addAttribute("groupedByDate", groupedByDate);
    model.addAttribute("resultList", packingList);
    }

    
    return "mypage";
}

} 