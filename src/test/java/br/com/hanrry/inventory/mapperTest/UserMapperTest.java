package br.com.hanrry.inventory.mapperTest;

import br.com.hanrry.inventory.dto.user.UserRequestDTO;
import br.com.hanrry.inventory.dto.user.UserResponseDTO;
import br.com.hanrry.inventory.entity.User;
import br.com.hanrry.inventory.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper userMapper =
            Mappers.getMapper(UserMapper.class);

    @Test
    void shouldMapRequestDtoToEntity() {
        UserRequestDTO request = new UserRequestDTO(
                "Hanrry",
                "hanrry@email.com",
                "123456"
        );

        User user = userMapper.toEntity(request);

        assertNotNull(user);
        assertNull(user.getId());
        assertEquals("Hanrry", user.getName());
        assertEquals("hanrry@email.com", user.getEmail());
        assertEquals("123456", user.getPassword());
    }

    @Test
    void shouldMapEntityToResponseDto() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 19, 8, 0);

        User user = new User();
        user.setId(1L);
        user.setName("Hanrry");
        user.setEmail("hanrry@email.com");
        user.setCreatedAt(createdAt);

        UserResponseDTO response = userMapper.toDTO(user);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Hanrry", response.name());
        assertEquals("hanrry@email.com", response.email());
        assertEquals(createdAt, response.createdAt());
    }

    @Test
    void shouldMapEntityListToResponseDtoList() {
        User user = new User();
        user.setId(1L);
        user.setName("Hanrry");
        user.setEmail("hanrry@email.com");

        List<UserResponseDTO> result = userMapper.toDTOList(List.of(user));

        assertEquals(1, result.size());
        assertEquals("Hanrry", result.get(0).name());
    }
}