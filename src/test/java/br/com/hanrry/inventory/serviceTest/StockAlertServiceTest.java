package br.com.hanrry.inventory.serviceTest;

import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.service.EmailService;
import br.com.hanrry.inventory.service.PdfService;
import br.com.hanrry.inventory.service.ProductService;
import br.com.hanrry.inventory.service.StockAlertService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockAlertServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private PdfService pdfService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private StockAlertService stockAlertService;

    @Test
    void shouldSendLowStockAlertSuccessfully() {

        ProductResponseDTO product =
                new ProductResponseDTO(
                        1L,
                        "Notebook",
                        "NOTE-001",
                        5L,
                        "Eletrônicos",
                        10L
                );

        List<ProductResponseDTO> lowStockProducts = List.of(product);

        byte[] pdfReport = new byte[]{1, 2, 3};

        when(productService.getLowStockProducts())
                .thenReturn(lowStockProducts);

        when(pdfService.generateLowStockReport(lowStockProducts))
                .thenReturn(pdfReport);

        stockAlertService.checkInventoryAndNotify();

        verify(productService).getLowStockProducts();

        verify(pdfService).generateLowStockReport(lowStockProducts);

        verify(emailService).sendLowStockAlert(
                List.of("Notebook"),
                pdfReport
        );
    }

    @Test
    void shouldNotSendEmailWhenNoLowStockProducts() {
        when(productService.getLowStockProducts())
                .thenReturn(List.of());

        stockAlertService.checkInventoryAndNotify();

        verify(productService).getLowStockProducts();
        verifyNoInteractions(pdfService);
        verifyNoInteractions(emailService);
    }
}