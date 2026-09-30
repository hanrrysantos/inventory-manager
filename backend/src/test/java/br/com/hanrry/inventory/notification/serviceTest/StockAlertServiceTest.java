package br.com.hanrry.inventory.notification.serviceTest;

import br.com.hanrry.inventory.product.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.notification.email.EmailSender;
import br.com.hanrry.inventory.notification.document.PdfService;
import br.com.hanrry.inventory.product.service.ProductService;
import br.com.hanrry.inventory.notification.service.StockAlertService;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import br.com.hanrry.inventory.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void shouldSendCurrentPdfAlertWithLowStockProductsOfGivenOwner() {
        User owner = new User();
        owner.setId(7L);

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

        when(productService.findLowStockProducts(owner, Pageable.unpaged()))
                .thenReturn(PageResponse.from(new PageImpl<>(lowStockProducts)));

        when(pdfService.generateLowStockReport(lowStockProducts))
                .thenReturn(pdfReport);

        stockAlertService.checkInventoryAndNotify(owner);

        verify(pdfService).generateLowStockReport(lowStockProducts);
        verify(emailSender).sendLowStockAlert(
                List.of("Notebook"),
                pdfReport
        );
        verify(productService, never()).getLowStockProducts();
    }

    @Test
    void shouldNotSendEmailWhenGivenOwnerHasNoLowStockProducts() {
        User owner = new User();
        owner.setId(7L);

        when(productService.findLowStockProducts(owner, Pageable.unpaged()))
                .thenReturn(PageResponse.from(new PageImpl<>(List.<ProductResponseDTO>of())));

        stockAlertService.checkInventoryAndNotify(owner);

        verifyNoInteractions(pdfService);
        verifyNoInteractions(emailSender);
    }

    @Test
    void shouldKeepScheduledJobInProcessWithoutQueuePublisher() throws NoSuchMethodException {
        assertNotNull(StockAlertService.class
                .getMethod("checkInventoryAndNotify")
                .getAnnotation(Scheduled.class));

        assertThat(StockAlertService.class.getDeclaredFields())
                .extracting(Field::getType)
                .noneMatch(type -> AmqpTemplate.class.isAssignableFrom(type)
                        || ApplicationEventPublisher.class.isAssignableFrom(type));
    }
}
