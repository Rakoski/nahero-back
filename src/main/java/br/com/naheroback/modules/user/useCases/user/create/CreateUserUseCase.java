package br.com.naheroback.modules.user.useCases.user.create;

import br.com.naheroback.common.exceptions.custom.DuplicateException;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreateUserUseCase {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final CreateUserResponse createUserResponse;

    @Transactional
    public CreateUserResponse execute(CreateUserRequest input) {
        validateDependencies(input);
        User user = CreateUserRequest.toDomain(input);
        user.setPassword(passwordEncoder.encode(input.password()));

        Role isStudent = roleRepository.findByName(RolesEnum.IS_STUDENT.name()).orElseThrow(() -> NotFoundException.with(Role.class, "name", RolesEnum.IS_STUDENT.name()));
        user.getRoles().add(isStudent);

        userRepository.save(user);

        return createUserResponse.toPresentation(user);
    }

    private void validateDependencies(CreateUserRequest input) {
        validateUniqueEmail(input.email());
    }

    private void validateUniqueEmail(String email) {
        if (this.userRepository.findByEmail(email).isPresent()) {
            throw DuplicateException.with(User.class, "email", email);
        }
    }
}
