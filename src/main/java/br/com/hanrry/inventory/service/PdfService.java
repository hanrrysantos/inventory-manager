package br.com.hanrry.inventory.service;

import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
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

            // Título
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Relatório de Reposição de Estoque", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // Espaço em branco

            // Criando a Tabela (4 colunas)
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            // Cabeçalho da Tabela
            table.addCell("Produto");
            table.addCell("Categoria");
            table.addCell("Estoque Atual");
            table.addCell("Estoque Mínimo");

            // Preenchendo com os dados
            // Preenchendo com os dados corrigidos
            for (ProductResponseDTO p : products) {
                table.addCell(p.name());
                table.addCell(p.categoryName());
                table.addCell(String.valueOf(p.totalQuantity())); // Corrigido para totalQuantity
                table.addCell(String.valueOf(p.minStock()));      // Corrigido para minStock

                long necessidade = p.minStock() - p.totalQuantity();
                table.addCell(String.valueOf(necessidade));
            }

            document.add(table);
            document.close();

        } catch (DocumentException e) {
            throw new RuntimeException("Erro ao gerar PDF", e);
        }

        return out.toByteArray();
    }
}