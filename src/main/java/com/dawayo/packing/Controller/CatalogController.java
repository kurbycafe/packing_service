// package com.dawayo.packing.Controller;

// import org.springframework.stereotype.Controller;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.DeleteMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.ResponseBody;
// import org.springframework.http.ResponseEntity;
// import org.springframework.ui.Model;
// import lombok.RequiredArgsConstructor;

// import java.util.List;
// import java.util.Map;
// import com.dawayo.packing.Service.CatalogService;
// import com.dawayo.packing.Repository.ProductRepository;
// import com.dawayo.packing.VO.CatalogCategory;
// import com.dawayo.packing.VO.ProductVO;

// @Controller

// @RequestMapping("/catalog")
// @RequiredArgsConstructor
// public class CatalogController {

//     private final CatalogService catalogService;
//     private final ProductRepository productRepository;

//     // 1. 카탈로그 관리 메인 페이지
//     @GetMapping("/manager")
//     public String managerPage(Model model) {
//         //model.addAttribute("categories", catalogService.getAllCategories());
//         return "catalog-manager"; // 앞서 만든 타임리프 파일명
//     }

//     // 2. [API] 카테고리 생성
//     @PostMapping("/api/category")
//     @ResponseBody
//     public ResponseEntity<CatalogCategory> createCategory(@RequestBody Map<String, String> params) {
//         String name = params.get("name");
//         //return ResponseEntity.ok(catalogService.createCategory(name));
//         return null;
//     }

//     // 3. [API] 전체 상품 목록 조회 (모달창 검색용)
//     @GetMapping("/api/products")
//     @ResponseBody
//     public List<Product> getAllProducts() {
//         return productRepository.findAll();
//     }

//     // 4. [API] 카테고리에 상품 추가
//     @PostMapping("/api/{categoryId}/product/{productId}")
//     @ResponseBody
//     public ResponseEntity<Void> addProductToCategory(
//             @PathVariable Long categoryId, 
//             @PathVariable Long productId) {
//         catalogService.addProductToCategory(categoryId, productId);
//         return ResponseEntity.ok().build();
//     }

//     // 5. [API] 매핑된 아이템 삭제
//     @DeleteMapping("/api/item/{itemId}")
//     @ResponseBody
//     public ResponseEntity<Void> removeItem(@PathVariable Long itemId) {
//         catalogService.deleteCatalogItem(itemId);
//         return ResponseEntity.ok().build();
//     }
// }