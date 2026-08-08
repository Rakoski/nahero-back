package br.com.naheroback.modules.user.useCases.user.create;

import br.com.naheroback.modules.user.entities.User;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CreateUserResponse {
    public Integer id;
    public String name;
    public String email;

    @Autowired
    private ModelMapper modelMapper;

    public CreateUserResponse toPresentation(User user) {
        return modelMapper.map(user, CreateUserResponse.class);
    }
}
