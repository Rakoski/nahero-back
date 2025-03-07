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
    public String cpf;
    public String passportNumber;
    public String bio;
    public String phone;
    public String avatarUrl;
    public LocalDateTime emailConfirmedAt;
    public String externalCustomerId;
    public CreateUserAddress address;

    @Autowired
    private ModelMapper modelMapper;

    public static class CreateUserAddress {
        public String cep;
        public String street;
        public String number;
        public String complement;
        public String neighborhood;
        public String city;
        public String state;
        public String country;
    }

    public CreateUserResponse toPresentation(User user) {
        return modelMapper.map(user, CreateUserResponse.class);
    }
}
