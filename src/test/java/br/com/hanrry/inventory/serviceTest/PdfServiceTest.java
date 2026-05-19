package br.com.hanrry.inventory.serviceTest;

import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.service.PdfService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    @Test
    void shouldGenerateLowStockReportSuccessfully() throws Exception {
        ProductResponseDTO product = new ProductResponseDTO(
                1L,
                "Notebook",
                "NOTE-001",
                3L,
                "Eletrônicos",
                10L
        );

        byte[] pdf = pdfService.generateLowStockReport(List.of(product));

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        PdfReader reader = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);

        String text = extractor.getTextFromPage(1);

        String normalizedText = text
                .replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim();

        reader.close();

        assertTrue(normalizedText.contains("Relatório de Reposição de Estoque"));
        assertTrue(normalizedText.contains("Produto"));
        assertTrue(normalizedText.contains("Categoria"));
        assertTrue(normalizedText.contains("Estoque Atual"));
        assertTrue(normalizedText.contains("Estoque Mínimo"));
        assertTrue(normalizedText.contains("Reposição"));
        assertTrue(normalizedText.contains("Necessária"));

        assertTrue(normalizedText.contains("Notebook"));
        assertTrue(normalizedText.contains("Eletrônicos"));
        assertTrue(normalizedText.contains("3"));
        assertTrue(normalizedText.contains("10"));
        assertTrue(normalizedText.contains("7"));
    }
}