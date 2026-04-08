package br.com.hanrry.inventory.service;

import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockAlertService {

    private final ProductService productService;
    private final PdfService pdfService;
    private final EmailService emailService;

    @Scheduled(initialDelay = 10000, fixedRate = 36000000)
    public void checkInventoryAndNotify() {
        List<ProductResponseDTO> lowStockProducts = productService.getLowStockProducts();

        if (!lowStockProducts.isEmpty()) {
            List<String> allProductNames = lowStockProducts.stream()
                    .map(ProductResponseDTO::name)
                    .toList();

            byte[] pdfReport = pdfService.generateLowStockReport(lowStockProducts);

            emailService.sendLowStockAlert(allProductNames, pdfReport);

            System.out.println("Alerta de estoque enviado!");
        }
    }
}