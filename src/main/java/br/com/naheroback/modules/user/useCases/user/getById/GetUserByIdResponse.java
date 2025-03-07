package br.com.naheroback.modules.user.useCases.user.getById;

import br.com.naheroback.modules.user.entities.User;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
public class GetUserByIdResponse {
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
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
    public GetUserAddress address;
    public Set<String> roles;

    @Autowired
    private ModelMapper modelMapper;

    public static class GetUserAddress {
        public String cep;
        public String street;
        public String number;
        public String complement;
        public String neighborhood;
        public String city;
        public String state;
        public String country;
    }

    public GetUserByIdResponse toPresentation(User user) {
        return modelMapper.map(user, GetUserByIdResponse.class);
    }
}
