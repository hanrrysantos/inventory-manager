package br.com.hanrry.inventory.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequestDTO (
        @Size(min = 6, message = "Password must have at least 6 characters")
        String password
){
}
