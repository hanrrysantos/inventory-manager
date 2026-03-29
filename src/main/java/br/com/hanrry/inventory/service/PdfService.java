package br.com.hanrry.inventory.service;

import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.exception.pdf.WritePdfException;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class PdfService {

    public byte[] generateLowStockReport(List<ProductResponseDTO> products) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font fontTitle = FontFactory.getFont(FontFactory.COURIER_BOLD, 18);
            Paragraph title = new Paragraph("Relatório de Reposição de Estoque", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            table.addCell("Produto");
            table.addCell("Categoria");
            table.addCell("Estoque Atual");
            table.addCell("Estoque Mínimo");
            table.addCell("Reposição Necessária");

            for (ProductResponseDTO productsLowStock : products) {
                table.addCell(productsLowStock.name());
                table.addCell(productsLowStock.categoryName());
                table.addCell(String.valueOf(productsLowStock.totalQuantity()));
                table.addCell(String.valueOf(productsLowStock.minStock()));

                long necessidade = productsLowStock.minStock() - productsLowStock.totalQuantity();
                table.addCell(String.valueOf(necessidade));
            }

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            throw new WritePdfException("Error generating PDF");
        }

        return out.toByteArray();
    }
}