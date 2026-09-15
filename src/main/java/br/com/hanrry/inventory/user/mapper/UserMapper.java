package br.com.hanrry.inventory.user.mapper;

import br.com.hanrry.inventory.user.dto.UserRequestDTO;
import br.com.hanrry.inventory.user.dto.UserResponseDTO;
import br.com.hanrry.inventory.user.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDTO toDTO(User user);

    User toEntity(UserRequestDTO request);

    List<UserResponseDTO> toDTOList(List<User> userList);

}
