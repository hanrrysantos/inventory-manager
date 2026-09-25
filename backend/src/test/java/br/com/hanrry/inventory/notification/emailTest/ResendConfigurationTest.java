package br.com.hanrry.inventory.notification.emailTest;

import br.com.hanrry.inventory.notification.email.ResendConfiguration;
import com.resend.Resend;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = ResendConfiguration.class)
@TestPropertySource(properties = {
        "resend.api-key=re_test",
        "resend.from=alerts@example.com",
        "resend.to=owner@example.com"
})
class ResendConfigurationTest {

    @Autowired
    private Resend resend;

    @Test
    void shouldCreateResendClientFromConfiguredApiKey() {
        assertNotNull(resend);
    }
}
