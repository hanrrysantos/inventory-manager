package br.com.hanrry.inventory.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendLowStockAlert(String productName, byte[] pdfAttachment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            // true indica que teremos anexos (multipart)
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo("seu-email-de-destino@gmail.com");
            helper.setSubject("⚠️ ALERTA DE ESTOQUE: " + productName);
            helper.setText("Olá,\n\nO estoque do produto " + productName +
                    " atingiu o nível crítico. Segue em anexo o relatório de reposição.");

            // Adicionando o PDF como anexo
            helper.addAttachment("relatorio_reposicao.pdf", new ByteArrayResource(pdfAttachment));

            mailSender.send(message);
            System.out.println("E-mail enviado com sucesso!");

        } catch (MessagingException e) {
            throw new RuntimeException("Falha ao enviar e-mail com anexo", e);
        }
    }
}