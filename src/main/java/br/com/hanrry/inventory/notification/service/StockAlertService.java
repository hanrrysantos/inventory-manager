package br.com.hanrry.inventory.notification.service;

import br.com.hanrry.inventory.notification.document.PdfService;
import br.com.hanrry.inventory.notification.email.EmailSender;
import br.com.hanrry.inventory.product.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockAlertService {

    private final ProductService productService;
    private final PdfService pdfService;
    private final EmailSender emailSender;

    @Scheduled(initialDelay = 10000, fixedRate = 36000000)
    public void checkInventoryAndNotify() {
        List<ProductResponseDTO> lowStockProducts = productService.getLowStockProducts();

        if (!lowStockProducts.isEmpty()) {
            List<String> allProductNames = lowStockProducts.stream()
                    .map(ProductResponseDTO::name)
                    .toList();

            byte[] pdfReport = pdfService.generateLowStockReport(lowStockProducts);

            emailSender.sendLowStockAlert(allProductNames, pdfReport);

            log.info("Alerta de estoque enviado para {} produto(s)", allProductNames.size());
        }
    }
}
