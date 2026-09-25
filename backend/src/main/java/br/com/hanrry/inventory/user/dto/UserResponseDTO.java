package br.com.hanrry.inventory.user.dto;

import java.time.LocalDateTime;

public record UserResponseDTO (
        Long id,
        String name,
        String email,
        String role,
        LocalDateTime createdAt
){
    public UserResponseDTO(Long id, String name, String email, LocalDateTime createdAt) {
        this(id, name, email, null, createdAt);
    }
}
