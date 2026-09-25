package br.com.hanrry.inventory.notification.email;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResendConfiguration {

    @Bean
    public Resend resend(@Value("${resend.api-key:}") String apiKey) {
        return new Resend(apiKey);
    }
}
