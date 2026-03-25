package br.com.hanrry.inventory.mapper;

import br.com.hanrry.inventory.dto.user.UserRequestDTO;
import br.com.hanrry.inventory.dto.user.UserResponseDTO;
import br.com.hanrry.inventory.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDTO toDTO(User user);

    User toEntity(UserRequestDTO request);

}
