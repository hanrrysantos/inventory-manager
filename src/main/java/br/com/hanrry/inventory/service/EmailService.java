package br.com.hanrry.inventory.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendLowStockAlert(List<String> productNames, byte[] pdfAttachment) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo("sheinhanrry@gmail.com");
            helper.setSubject("ALERTA DE ESTOQUE");

            String nomesFormatados = String.join(", ", productNames);

            StringBuilder body = new StringBuilder();
            body.append("Olá,\n\n");
            body.append("Os seguintes produtos atingiram o nível crítico de estoque:\n");
            body.append(nomesFormatados).append(".\n\n");
            body.append("Segue em anexo o relatório detalhado de reposição para todos os itens em falta.");

            helper.setText(body.toString());

            helper.addAttachment("relatorio_reposicao.pdf", new ByteArrayResource(pdfAttachment));

            mailSender.send(message);
            System.out.println("E-mail enviado com sucesso!");

        } catch (MessagingException e) {
            throw new RuntimeException("Falha ao enviar e-mail com anexo", e);
        }
    }
}