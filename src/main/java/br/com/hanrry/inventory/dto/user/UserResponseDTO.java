package br.com.hanrry.inventory.dto.user;

import java.time.LocalDateTime;

public record UserResponseDTO (
        Long id,
        String name,
        String email,
        LocalDateTime createdAt
){
}
