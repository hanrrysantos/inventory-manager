package br.com.hanrry.inventory.notification.serviceTest;

import br.com.hanrry.inventory.product.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.notification.email.EmailSender;
import br.com.hanrry.inventory.notification.document.PdfService;
import br.com.hanrry.inventory.product.service.ProductService;
import br.com.hanrry.inventory.notification.service.StockAlertService;
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
    private EmailSender emailSender;

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

        verify(emailSender).sendLowStockAlert(
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
        verifyNoInteractions(emailSender);
    }
}
