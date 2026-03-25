// package com.dawayo.packing.Controller;

// import lombok.RequiredArgsConstructor;

// @RestController
// @RequestMapping("/api/catalog/export")
// @RequiredArgsConstructor
// public class ExportController {

//     private final CatalogService catalogService;
//     private final ExcelExportService excelService;
//     private final PdfExportService pdfService;

//     @GetMapping("/excel")
//     public void downloadExcel(HttpServletResponse response) throws IOException {
//         List<CatalogCategory> categories = catalogService.getAllCategories();
//         excelService.exportCatalogToExcel(categories, response);
//     }

//     @GetMapping("/pdf")
//     public void downloadPdf(HttpServletResponse response) throws Exception {
//         List<CatalogCategory> categories = catalogService.getAllCategories();
//         pdfService.exportCatalogToPdf(categories, response);
//     }
// }