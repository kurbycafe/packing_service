package com.dawayo.packing.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dawayo.packing.VO.ProductVO;
import com.dawayo.packing.VO.ProductBatchVO;
import com.dawayo.packing.Repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public void saveOrUpdateProduct(ProductVO product, List<ProductBatchVO> batches) {
        LocalDateTime now = LocalDateTime.now();

        ProductVO existing = productRepository.findByWooId(product.getWooId()).orElse(null);

        if (existing == null) {
            product.setCreatedAt(now);
            product.setUpdatedAt(now);
        } else {
            existing.setName(product.getName());
            existing.setSku(product.getSku());
            existing.setCustomSku(product.getCustomSku());
            existing.setPrice(product.getPrice());
            existing.setSalePrice(product.getSalePrice());
            // [추가] 이미지 URL 업데이트
            existing.setImageUrl(product.getImageUrl());
            existing.setUpdatedAt(now);


            product = existing;
        }

        for (ProductBatchVO batch : batches) {
            batch.setProduct(product);
            batch.setCreatedAt(now);
            batch.setUpdatedAt(now);
   
        }

        int totalStock = batches.stream().mapToInt(ProductBatchVO::getQuantity).sum();
        product.setTotalStock(totalStock);

        productRepository.save(product); // cascade로 batch도 저장
    }

    // DB의 모든 상품 조회 (엑셀용)
    public List<ProductVO> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Map<String, String>> searchProducts(String query) {
        List<ProductVO> products = productRepository.searchProducts(query);

        return products.stream().map(p -> {
            Map<String, String> m = new HashMap<>();
            m.put("name", p.getName());
            m.put("sku", p.getSku());
            m.put("price", p.getPrice());
            m.put("salePrice", p.getSalePrice());
            m.put("wooId", String.valueOf(p.getWooId()));
            // 검색 결과에도 이미지 URL 포함 가능
            m.put("imageUrl", p.getImageUrl());

            return m;
        }).toList();
    }

    /**
     * [추가] 엑셀 내보내기 로직 (실제 이미지 포함)
     */
    public void exportToExcelWithImages(List<ProductVO> products, HttpServletResponse response) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("상품 리스트");

        // 컬럼 너비 설정
        sheet.setColumnWidth(0, 4500); // 이미지 열
        sheet.setColumnWidth(1, 12000); // 상품명 열
        sheet.setColumnWidth(2, 5000); // SKU 열

        // 헤더 스타일
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        Row header = sheet.createRow(0);
        String[] headers = {"이미지", "상품명", "SKU"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (ProductVO p : products) {
            Row row = sheet.createRow(rowIdx);
            row.setHeightInPoints(80); // 이미지 크기를 고려한 높이

            row.createCell(1).setCellValue(p.getName());
            row.createCell(2).setCellValue(p.getSku());

            // 이미지 삽입
            if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
                try {
                    URL url = new URL(p.getImageUrl());
                    try (InputStream is = url.openStream()) {
                        byte[] bytes = IOUtils.toByteArray(is);
                        int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG);

                        CreationHelper helper = workbook.getCreationHelper();
                        Drawing<?> drawing = sheet.createDrawingPatriarch();
                        ClientAnchor anchor = helper.createClientAnchor();

                        // 0번 열(이미지)에 배치
                        anchor.setCol1(0);
                        anchor.setRow1(rowIdx);
                        anchor.setCol2(1);
                        anchor.setRow2(rowIdx + 1);

                        Picture pict = drawing.createPicture(anchor, pictureIdx);
                        
                        // 셀 크기에 맞춰 이미지 크기 미세 조정 (비율 유지)
                        double scale = 0.9; 
                        pict.resize(scale);
                    }
                } catch (Exception e) {
                    row.createCell(0).setCellValue("이미지 없음");
                }
            }
            rowIdx++;
        }

        // 파일명 설정
        String fileName = "product_list_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        workbook.write(response.getOutputStream());
        workbook.close();
    }
}