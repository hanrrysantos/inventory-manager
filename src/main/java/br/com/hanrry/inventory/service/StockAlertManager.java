package br.com.hanrry.inventory.service;

import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.entity.Batch;
import br.com.hanrry.inventory.entity.Product;
import br.com.hanrry.inventory.exception.product.ProductNotFoundException;
import br.com.hanrry.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockAlertManager {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final PdfService pdfService;
    private final EmailService emailService;

    public void checkLowStockAndNotify(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        long currentStock = product.getBatches().stream()
                .mapToLong(Batch::getQuantity)
                .sum();

        if (currentStock <= product.getMinStock()) {
            this.sendNotificationWithReport(product.getName());
        }
    }

    private void sendNotificationWithReport(String productName) {

        List<ProductResponseDTO> lowStockProducts = productService.getLowStockProducts();

        if (!lowStockProducts.isEmpty()) {
            List<String> allProductNames = lowStockProducts.stream()
                    .map(ProductResponseDTO::name)
                    .toList();

            byte[] pdfReport = pdfService.generateLowStockReport(lowStockProducts);

            emailService.sendLowStockAlert(allProductNames, pdfReport);
        }
    }
}