package br.com.naheroback.modules.auth.useCases.login;

import br.com.naheroback.modules.user.entities.User;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
public class LoginResponse {
    public String accessToken;
    public String refreshToken;
    public GetLoginUser user;

    @Autowired
    private ModelMapper modelMapper;

    public static class GetLoginUser {
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
        public GetLoginAddress address;
        public Set<GetLoginRole> roles;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
    }

    public static class GetLoginRole {
        public Integer id;
        public String name;
    }

    public static class GetLoginAddress {
        public String zipCode;
        public String street;
        public String number;
        public String complement;
        public String neighborhood;
        public String city;
        public String state;
        public String country;
    }

    public LoginResponse toPresentation(String accessToken, String refreshToken, User user) {
        LoginResponse response = new LoginResponse();
        response.accessToken = accessToken;
        response.refreshToken = refreshToken;
        response.user = modelMapper.map(user, GetLoginUser.class);
        return response;
    }
}