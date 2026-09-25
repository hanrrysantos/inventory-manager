package br.com.hanrry.inventory.notification.emailTest;

import br.com.hanrry.inventory.notification.email.ResendEmailSender;
import br.com.hanrry.inventory.shared.exception.notification.email.EmailSendException;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResendEmailSenderTest {

    @Mock
    private Resend resend;

    @Mock
    private Emails emails;

    private ResendEmailSender emailSender;

    @BeforeEach
    void setUp() {
        emailSender = new ResendEmailSender(resend, "alerts@example.com", "owner@example.com");
    }

    @Test
    void shouldSendLowStockAlertWithConfiguredRecipientsAndPdfAttachment() throws Exception {
        byte[] pdfAttachment = new byte[]{1, 2, 3};
        when(resend.emails()).thenReturn(emails);

        emailSender.sendLowStockAlert(List.of("Notebook", "Mouse"), pdfAttachment);

        ArgumentCaptor<CreateEmailOptions> optionsCaptor = ArgumentCaptor.forClass(CreateEmailOptions.class);
        verify(emails).send(optionsCaptor.capture());

        CreateEmailOptions options = optionsCaptor.getValue();
        assertEquals("alerts@example.com", options.getFrom());
        assertEquals(List.of("owner@example.com"), options.getTo());
        assertEquals("ALERTA DE ESTOQUE", options.getSubject());
        assertEquals("Olá,\n\nOs seguintes produtos atingiram o nível crítico de estoque:\n"
                        + "Notebook, Mouse.\n\nSegue em anexo o relatório detalhado de reposição para todos os itens em falta.",
                options.getText());
        assertEquals("relatorio_reposicao.pdf", options.getAttachments().get(0).getFileName());
        assertArrayEquals(pdfAttachment,
                Base64.getDecoder().decode(options.getAttachments().get(0).getContent()));
    }

    @Test
    void shouldConvertResendFailureToEmailSendException() throws Exception {
        when(resend.emails()).thenReturn(emails);
        when(emails.send(org.mockito.ArgumentMatchers.any(CreateEmailOptions.class)))
                .thenThrow(new ResendException("Resend API error"));

        EmailSendException exception = assertThrows(EmailSendException.class,
                () -> emailSender.sendLowStockAlert(List.of("Notebook"), new byte[]{1}));

        assertEquals("Falha ao enviar alerta de estoque pelo Resend", exception.getMessage());
        verify(emails).send(org.mockito.ArgumentMatchers.any(CreateEmailOptions.class));
    }

    @Test
    void shouldRejectMissingSenderConfigurationWithoutExposingApiKey() {
        ResendEmailSender senderWithoutFrom = new ResendEmailSender(resend, "", "owner@example.com");

        EmailSendException exception = assertThrows(EmailSendException.class,
                () -> senderWithoutFrom.sendLowStockAlert(List.of("Notebook"), "secret".getBytes(StandardCharsets.UTF_8)));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("resend.from"));
        org.junit.jupiter.api.Assertions.assertFalse(exception.getMessage().contains("secret"));
    }
}
