// package com.dawayo.packing.Service;

// import lombok.RequiredArgsConstructor;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import com.dawayo.packing.Repository.CatalogItemRepository;

// @Service
// @RequiredArgsConstructor
// @Transactional
// public class CatalogService {

//     private final CategoryRepository categoryRepository;
//     private final ProductRepository productRepository;
//     private final CatalogItemRepository itemRepository;

//     public List<CatalogCategory> getAllCategories() {
//         // 정렬 순서대로 가져오기
//         return categoryRepository.findAllByOrderBySortOrderAsc();
//     }

//     public CatalogCategory createCategory(String name) {
//         CatalogCategory category = new CatalogCategory();
//         category.setName(name);
//         category.setSortOrder(0); // 필요시 마지막 순서로 계산 logic 추가
//         return categoryRepository.save(category);
//     }

//     public void addProductToCategory(Long categoryId, Long productId) {
//         CatalogCategory category = categoryRepository.findById(categoryId)
//                 .orElseThrow(() -> new RuntimeException("Category not found"));
//         Product product = productRepository.findById(productId)
//                 .orElseThrow(() -> new RuntimeException("Product not found"));

//         CatalogItem item = new CatalogItem();
//         item.setCategory(category);
//         item.setProduct(product);
//         item.setSortOrder(category.getItems().size() + 1); // 리스트 끝에 추가
        
//         itemRepository.save(item);
//     }

//     public void deleteCatalogItem(Long itemId) {
//         itemRepository.deleteById(itemId);
//     }
// }