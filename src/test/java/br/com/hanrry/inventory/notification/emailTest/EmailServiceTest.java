package br.com.hanrry.inventory.notification.emailTest;

import br.com.hanrry.inventory.notification.email.EmailService;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void shouldSendLowStockAlertSuccessfully() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(
                Session.getInstance(new Properties())
        );

        byte[] pdfAttachment = new byte[]{1, 2, 3};

        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);

        emailService.sendLowStockAlert(
                List.of("Notebook", "Mouse"),
                pdfAttachment
        );

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);

        assertEquals("ALERTA DE ESTOQUE", mimeMessage.getSubject());

        assertEquals(
                "sheinhanrry@gmail.com",
                mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString()
        );

        MimeMultipart multipart = (MimeMultipart) mimeMessage.getContent();
        assertEquals(2, multipart.getCount());
        MimeMultipart textPart = (MimeMultipart) multipart.getBodyPart(0).getContent();
        assertEquals(
                "Olá,\n\nOs seguintes produtos atingiram o nível crítico de estoque:\nNotebook, Mouse.\n\n"
                        + "Segue em anexo o relatório detalhado de reposição para todos os itens em falta.",
                textPart.getBodyPart(0).getContent()
        );

        BodyPart attachment = multipart.getBodyPart(1);
        assertEquals("relatorio_reposicao.pdf", attachment.getFileName());
        assertArrayEquals(pdfAttachment, attachment.getInputStream().readAllBytes());
    }

    @Test
    void shouldThrowExceptionWhenEmailSendingFails() {
        MimeMessage mimeMessage = new MimeMessage(
                Session.getInstance(new Properties())
        );

        byte[] pdfAttachment = new byte[]{1, 2, 3};

        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);

        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender)
                .send(mimeMessage);

        assertThrows(
                RuntimeException.class,
                () -> emailService.sendLowStockAlert(
                        List.of("Notebook"),
                        pdfAttachment
                )
        );

        verify(mailSender).send(mimeMessage);
    }
}
