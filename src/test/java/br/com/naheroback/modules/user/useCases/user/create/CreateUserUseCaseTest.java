package br.com.naheroback.modules.user.useCases.user.create;

import br.com.naheroback.common.exceptions.custom.DuplicateException;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.user.entities.Address;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import br.com.naheroback.modules.user.repositories.AddressRepository;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CreateUserResponse createUserResponse;

    @InjectMocks
    private CreateUserUseCase createUserUseCase;

    private CreateUserRequest validRequest;
    private User mockUser;
    private Role studentRole;
    private CreateUserResponse mockResponse;

    @BeforeEach
    void setUp() {
        CreateUserRequest.AddressInput addressInput = new CreateUserRequest.AddressInput(
                "12345-678",
                "Test Street",
                "123",
                "Apt 101",
                "Test Neighborhood",
                "Test City",
                "TS",
                "Brazil"
        );

        validRequest = new CreateUserRequest(
                "Test User",
                "test@example.com",
                "123.456.789-09",
                null,
                "Test bio",
                "password123",
                "1234567890",
                "https://example.com/avatar.jpg",
                null,
                null,
                addressInput
        );

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");
        
        Address address = new Address();
        address.setId(1);
        address.setZipCode("12345-678");
        address.setStreet("Test Street");
        mockUser.setAddress(address);

        studentRole = new Role();
        studentRole.setId(1);
        studentRole.setName(RolesEnum.IS_STUDENT.name());

        mockResponse = new CreateUserResponse();
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUserSuccessfully() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByCpf(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(RolesEnum.IS_STUDENT.name())).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(createUserResponse.toPresentation(any(User.class))).thenReturn(mockResponse);

        CreateUserResponse result = createUserUseCase.execute(validRequest);

        assertNotNull(result);
        assertEquals(mockResponse, result);

        verify(userRepository, times(1)).findByEmail(validRequest.email());
        verify(userRepository, times(1)).findByCpf(validRequest.cpf());
        verify(passwordEncoder, times(1)).encode(validRequest.password());
        verify(addressRepository, times(1)).save(any(Address.class));
        verify(roleRepository, times(1)).findByName(RolesEnum.IS_STUDENT.name());
        verify(userRepository, times(1)).save(any(User.class));
        verify(createUserResponse, times(1)).toPresentation(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateException when email already exists")
    void shouldThrowDuplicateExceptionWhenEmailAlreadyExists() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));

        DuplicateException exception = assertThrows(
                DuplicateException.class,
                () -> createUserUseCase.execute(validRequest)
        );

        assertTrue(exception.getMessage().contains("email"));
        assertTrue(exception.getMessage().contains(validRequest.email()));

        verify(userRepository, times(1)).findByEmail(validRequest.email());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateException when CPF already exists")
    void shouldThrowDuplicateExceptionWhenCpfAlreadyExists() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByCpf(anyString())).thenReturn(Optional.of(new User()));

        DuplicateException exception = assertThrows(
                DuplicateException.class,
                () -> createUserUseCase.execute(validRequest)
        );

        String message = exception.getMessage();
        assertTrue(message.contains("cpf"));
        assertTrue(message.contains("User with cpf"));

        verify(userRepository, times(1)).findByEmail(validRequest.email());
        verify(userRepository, times(1)).findByCpf(validRequest.cpf());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when student role not found")
    void shouldThrowNotFoundExceptionWhenStudentRoleNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByCpf(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(RolesEnum.IS_STUDENT.name())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> createUserUseCase.execute(validRequest)
        );

        assertTrue(exception.getMessage().contains("Role"));
        assertTrue(exception.getMessage().contains(RolesEnum.IS_STUDENT.name()));

        verify(userRepository, times(1)).findByEmail(validRequest.email());
        verify(userRepository, times(1)).findByCpf(validRequest.cpf());
        verify(passwordEncoder, times(1)).encode(validRequest.password());
        verify(roleRepository, times(1)).findByName(RolesEnum.IS_STUDENT.name());
        verify(userRepository, never()).save(any(User.class));
    }
}