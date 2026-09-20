package br.com.hanrry.inventory.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleAuthRequestDTO(
        @NotBlank(message = "O ID Token do Google é obrigatório")
        String idToken
) {}