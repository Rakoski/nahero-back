package br.com.naheroback.modules.user.useCases.user.create;

import br.com.naheroback.common.exceptions.custom.DuplicateException;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import br.com.naheroback.modules.user.services.EmailVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CreateUserResponse createUserResponse;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private CreateUserUseCase createUserUseCase;

    private CreateUserRequest validRequest;
    private User mockUser;
    private Role studentRole;
    private CreateUserResponse mockResponse;

    @BeforeEach
    void setUp() {
        validRequest = new CreateUserRequest(
                "Test User",
                "test@example.com",
                "password123",
                null
        );

        mockUser = new User();
        mockUser.setId(1);
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");

        studentRole = new Role();
        studentRole.setId(1);
        studentRole.setName(RolesEnum.IS_STUDENT.name());

        mockResponse = new CreateUserResponse();
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUserSuccessfully() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(RolesEnum.IS_STUDENT.name())).thenReturn(Optional.of(studentRole));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(createUserResponse.toPresentation(any(User.class))).thenReturn(mockResponse);

        CreateUserResponse result = createUserUseCase.execute(validRequest);

        assertNotNull(result);
        assertEquals(mockResponse, result);

        verify(userRepository, times(1)).findByEmail(validRequest.email());
        verify(passwordEncoder, times(1)).encode(validRequest.password());
        verify(roleRepository, times(1)).findByName(RolesEnum.IS_STUDENT.name());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailVerificationService, times(1)).issueAndSendQuietly(any(User.class));
        verify(createUserResponse, times(1)).toPresentation(any(User.class));
    }

    @Test
    @DisplayName("Should send the verification email to the created user")
    void shouldSendVerificationEmailToCreatedUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(RolesEnum.IS_STUDENT.name())).thenReturn(Optional.of(studentRole));
        when(createUserResponse.toPresentation(any(User.class))).thenReturn(mockResponse);

        createUserUseCase.execute(validRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(emailVerificationService, times(1)).issueAndSendQuietly(userCaptor.capture());

        assertEquals(validRequest.email(), userCaptor.getValue().getEmail());
        assertNull(userCaptor.getValue().getEmailConfirmedAt());
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
    @DisplayName("Should persist the acquisition tags when the request carries them")
    void shouldPersistUtmTagsFromRequest() {
        CreateUserRequest taggedRequest = new CreateUserRequest(
                "Test User",
                "test@example.com",
                "password123",
                new CreateUserRequest.Utm("google", "cpc", "clf")
        );

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(RolesEnum.IS_STUDENT.name())).thenReturn(Optional.of(studentRole));
        when(createUserResponse.toPresentation(any(User.class))).thenReturn(mockResponse);

        createUserUseCase.execute(taggedRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        assertEquals("google", userCaptor.getValue().getUtmSource());
        assertEquals("cpc", userCaptor.getValue().getUtmMedium());
        assertEquals("clf", userCaptor.getValue().getUtmCampaign());
    }

    @Test
    @DisplayName("Should leave the acquisition tags null for organic sign-ups")
    void shouldLeaveUtmTagsNullWhenRequestHasNone() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(RolesEnum.IS_STUDENT.name())).thenReturn(Optional.of(studentRole));
        when(createUserResponse.toPresentation(any(User.class))).thenReturn(mockResponse);

        createUserUseCase.execute(validRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        assertNull(userCaptor.getValue().getUtmSource());
        assertNull(userCaptor.getValue().getUtmMedium());
        assertNull(userCaptor.getValue().getUtmCampaign());
    }

    @Test
    @DisplayName("Should clamp acquisition tags longer than the column allows")
    void shouldClampOversizedUtmTags() {
        String oversized = "x".repeat(200);
        CreateUserRequest taggedRequest = new CreateUserRequest(
                "Test User",
                "test@example.com",
                "password123",
                new CreateUserRequest.Utm(oversized, null, null)
        );

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(RolesEnum.IS_STUDENT.name())).thenReturn(Optional.of(studentRole));
        when(createUserResponse.toPresentation(any(User.class))).thenReturn(mockResponse);

        createUserUseCase.execute(taggedRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        assertEquals(64, userCaptor.getValue().getUtmSource().length());
    }

    @Test
    @DisplayName("Should throw NotFoundException when student role not found")
    void shouldThrowNotFoundExceptionWhenStudentRoleNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(RolesEnum.IS_STUDENT.name())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> createUserUseCase.execute(validRequest)
        );

        assertTrue(exception.getMessage().contains("Role"));
        assertTrue(exception.getMessage().contains(RolesEnum.IS_STUDENT.name()));

        verify(userRepository, times(1)).findByEmail(validRequest.email());
        verify(passwordEncoder, times(1)).encode(validRequest.password());
        verify(roleRepository, times(1)).findByName(RolesEnum.IS_STUDENT.name());
        verify(userRepository, never()).save(any(User.class));
        verify(emailVerificationService, never()).issueAndSendQuietly(any(User.class));
    }
}
