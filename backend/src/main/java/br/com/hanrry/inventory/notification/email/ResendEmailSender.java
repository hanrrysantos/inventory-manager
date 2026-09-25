package br.com.hanrry.inventory.notification.email;

import br.com.hanrry.inventory.shared.exception.notification.email.EmailSendException;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.Attachment;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;

@Service
public class ResendEmailSender implements EmailSender {

    private static final String SUBJECT = "ALERTA DE ESTOQUE";
    private static final String ATTACHMENT_NAME = "relatorio_reposicao.pdf";

    private final Resend resend;
    private final String from;
    private final String to;

    public ResendEmailSender(
            Resend resend,
            @Value("${resend.from:}") String from,
            @Value("${resend.to:}") String to
    ) {
        this.resend = resend;
        this.from = from;
        this.to = to;
    }

    @Override
    public void sendLowStockAlert(List<String> productNames, byte[] pdfAttachment) {
        validateConfiguration();

        String productNamesText = String.join(", ", productNames);
        String body = "Olá,\n\n"
                + "Os seguintes produtos atingiram o nível crítico de estoque:\n"
                + productNamesText + ".\n\n"
                + "Segue em anexo o relatório detalhado de reposição para todos os itens em falta.";

        Attachment attachment = Attachment.builder()
                .fileName(ATTACHMENT_NAME)
                .content(Base64.getEncoder().encodeToString(pdfAttachment))
                .contentType("application/pdf")
                .build();

        CreateEmailOptions options = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(SUBJECT)
                .text(body)
                .addAttachment(attachment)
                .build();

        try {
            resend.emails().send(options);
        } catch (ResendException exception) {
            throw new EmailSendException("Falha ao enviar alerta de estoque pelo Resend", exception);
        }
    }

    private void validateConfiguration() {
        if (from.isBlank()) {
            throw new EmailSendException("Configuração ausente: resend.from", null);
        }
        if (to.isBlank()) {
            throw new EmailSendException("Configuração ausente: resend.to", null);
        }
    }
}
