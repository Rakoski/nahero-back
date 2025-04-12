package br.com.naheroback.modules.user.useCases.user.getById;

import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetUserByIdUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GetUserByIdResponse getUserByIdResponse;

    @InjectMocks
    private GetUserByIdUseCase getUserByIdUseCase;

    private User mockUser;
    private GetUserByIdResponse mockResponse;
    private final Integer userId = 1;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockResponse = new GetUserByIdResponse();
    }

    @Test
    @DisplayName("Should return user when user exists")
    void shouldReturnUserWhenUserExists() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(getUserByIdResponse.toPresentation(mockUser)).thenReturn(mockResponse);

        GetUserByIdResponse result = getUserByIdUseCase.execute(userId);

        assertNotNull(result);
        assertEquals(mockResponse, result);
        verify(userRepository, times(1)).findById(userId);
        verify(getUserByIdResponse, times(1)).toPresentation(mockUser);
    }

    @Test
    @DisplayName("Should throw NotFoundException when user does not exist")
    void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> getUserByIdUseCase.execute(userId)
        );

        assertTrue(exception.getMessage().contains("User"));
        assertTrue(exception.getMessage().contains("id"));
        assertTrue(exception.getMessage().contains(userId.toString()));

        verify(userRepository, times(1)).findById(userId);
        verify(getUserByIdResponse, never()).toPresentation(any());
    }
}