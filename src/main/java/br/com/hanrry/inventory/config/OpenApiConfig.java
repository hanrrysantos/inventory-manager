package br.com.hanrry.inventory.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI customOpenAPI(){
        return new OpenAPI()
            .info(new Info()
                    .title("Inventory Manager API")
                    .description("API robusta para controle inteligente de estoque e gestão de lotes. " +
                            "O sistema oferece funcionalidades avançadas como rastreabilidade por data de vencimento, " +
                            "movimentação automatizada de inventário, logs de auditoria e alertas automáticos de estoque crítico. \n" +
                            "Principais Recursos: \n" +
                            "Gestão de produtos e categorias com controle de estoque mínimo; " +
                            "Consumo inteligente de lotes priorizando as datas de vencimento mais próximas; " +
                            "Notificações automáticas via E-mail com relatórios em PDF para reposição de estoque; " +
                            "Autenticação e Autorização via JWT. " +
                            "Desenvolvido para garantir eficiência operacional e evitar perdas de produtos por vencimento.")
                    .version("V2.0")
                    .termsOfService("https://github.com/hanrrysantos")
                    .contact(new Contact()
                            .url("https://github.com/hanrrysantos")
                            .name("Hanrry Santos")
                            .email("hanrry.jsantos@gmail.com")
                    )
                    .license(new License()
                            .name("Apache 2.0")
                            .url("https://www.apache.org/licenses/LICENSE-2.0")
                    )
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                    .addSecuritySchemes("bearerAuth", new SecurityScheme()
                            .name("bearerAuth")
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Insira o Token JWT gerado no endpoint de login (/api/v1/auth/login)")
                    )
            );
    }
}
