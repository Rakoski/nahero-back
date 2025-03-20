package br.com.naheroback.modules.user.useCases.role.link;

import br.com.naheroback.common.exceptions.custom.DuplicateException;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.entities.enums.RolesEnum;
import br.com.naheroback.modules.user.repositories.RoleRepository;
import br.com.naheroback.modules.user.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkUserRoleUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private LinkUserRoleUseCase linkUserRoleUseCase;

    private User mockUser;
    private Role studentRole;
    private Role teacherRole;
    private Role adminRole;
    private LinkUserRoleRequest request;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1);
        mockUser.setName("Test User");
        mockUser.setEmail("test@example.com");
        mockUser.setRoles(new HashSet<>());

        studentRole = new Role();
        studentRole.setId(1);
        studentRole.setName(RolesEnum.IS_STUDENT.name());

        teacherRole = new Role();
        teacherRole.setId(2);
        teacherRole.setName(RolesEnum.IS_TEACHER.name());

        adminRole = new Role();
        adminRole.setId(3);
        adminRole.setName(RolesEnum.IS_ADMIN.name());

        mockUser.getRoles().add(studentRole);

        request = new LinkUserRoleRequest(1, List.of(2, 3));
    }

    @Test
    @DisplayName("Should link roles to user successfully")
    void shouldLinkRolesToUserSuccessfully() {
        when(userRepository.findById(request.userId())).thenReturn(Optional.of(mockUser));
        when(roleRepository.findById(2)).thenReturn(Optional.of(teacherRole));
        when(roleRepository.findById(3)).thenReturn(Optional.of(adminRole));

        linkUserRoleUseCase.execute(request);

        assertEquals(3, mockUser.getRoles().size());
        assertTrue(mockUser.getRoles().contains(studentRole));
        assertTrue(mockUser.getRoles().contains(teacherRole));
        assertTrue(mockUser.getRoles().contains(adminRole));

        verify(userRepository, times(1)).findById(request.userId());
        verify(roleRepository, times(1)).findById(2);
        verify(roleRepository, times(1)).findById(3);
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    @DisplayName("Should throw NotFoundException when user not found")
    void shouldThrowNotFoundExceptionWhenUserNotFound() {
        when(userRepository.findById(request.userId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> linkUserRoleUseCase.execute(request)
        );

        assertTrue(exception.getMessage().contains("User"));
        assertTrue(exception.getMessage().contains(request.userId().toString()));

        verify(userRepository, times(1)).findById(request.userId());
        verify(roleRepository, never()).findById(anyInt());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when role not found")
    void shouldThrowNotFoundExceptionWhenRoleNotFound() {
        when(userRepository.findById(request.userId())).thenReturn(Optional.of(mockUser));
        when(roleRepository.findById(2)).thenReturn(Optional.of(teacherRole));
        when(roleRepository.findById(3)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> linkUserRoleUseCase.execute(request)
        );

        assertTrue(exception.getMessage().contains("Role"));
        assertTrue(exception.getMessage().contains("3"));

        verify(userRepository, times(1)).findById(request.userId());
        verify(roleRepository, times(1)).findById(2);
        verify(roleRepository, times(1)).findById(3);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw DuplicateException when role already linked to user")
    void shouldThrowDuplicateExceptionWhenRoleAlreadyLinkedToUser() {
        LinkUserRoleRequest duplicateRequest = new LinkUserRoleRequest(1, List.of(1));

        when(userRepository.findById(duplicateRequest.userId())).thenReturn(Optional.of(mockUser));

        DuplicateException exception = assertThrows(
                DuplicateException.class,
                () -> linkUserRoleUseCase.execute(duplicateRequest)
        );

        assertTrue(exception.getMessage().contains("userId and roleId"));
        assertTrue(exception.getMessage().contains("1 and 1"));

        verify(userRepository, times(1)).findById(duplicateRequest.userId());
        verify(roleRepository, never()).findById(anyInt());
        verify(userRepository, never()).save(any(User.class));
    }
}