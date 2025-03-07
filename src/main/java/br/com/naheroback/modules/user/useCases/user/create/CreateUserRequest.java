package br.com.naheroback.modules.user.useCases.user.create;

import br.com.naheroback.modules.user.entities.Address;
import br.com.naheroback.modules.user.entities.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDateTime;
import java.util.Objects;

public record CreateUserRequest(
    @NotBlank(message = "Name is required") String name,
    @Email @NotBlank(message = "Email is required") String email,
    @CPF @Pattern(regexp = "^$|.+", message = "CPF cannot be blank") String cpf,
    String passportNumber,
    String bio,
    @NotBlank(message = "Password is required") String password,
    @NotBlank(message = "Phone is required") String phone,
    String avatarUrl,
    LocalDateTime emailConfirmedAt,
    String externalCustomerId,
    AddressInput address
    ) {

    public record AddressInput(
        @NotBlank(message = "Zip code is required") String zipCode,
        @NotBlank(message = "Street is required") String street,
        @Positive @NotBlank(message = "Number is required") String number,
        String complement,
        @NotBlank(message = "Neighborhood is required") String neighborhood,
        @NotBlank(message = "City is required") String city,
        @NotBlank(message = "State is required") String state,
        String country
    ) {
    }

    public static User toDomain(CreateUserRequest input) {
        final var user = new User();
        user.setName(input.name);
        user.setEmail(input.email);
        user.setCpf(input.cpf.replaceAll("\\D", ""));
        user.setPassportNumber(input.passportNumber);
        user.setBio(input.bio);
        user.setPassword(input.password);
        user.setPhone(input.phone);
        user.setAvatarUrl(input.avatarUrl);
        user.setEmailConfirmedAt(input.emailConfirmedAt);
        user.setExternalCustomerId(input.externalCustomerId);

        if (Objects.nonNull(input.address)) {
            final var address = new Address();
            address.setZipCode(input.address.zipCode);
            address.setStreet(input.address.street);
            address.setNumber(input.address.number);
            address.setComplement(input.address.complement);
            address.setNeighborhood(input.address.neighborhood);
            address.setCity(input.address.city);
            address.setState(input.address.state);
            address.setCountry(Objects.isNull(input.address.country) ? "Brasil" : input.address.country);

            user.setAddress(address);
        }

        return user;
    }
}

