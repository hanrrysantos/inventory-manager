package br.com.hanrry.inventory.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record AuthRequestDTO (

        @Schema(example = "admin@email.com")
        String email,

        @Schema(example = "admin123")
        String password
){}

