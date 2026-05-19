package br.com.hanrry.inventory.serviceTest;

import br.com.hanrry.inventory.service.EmailService;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
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