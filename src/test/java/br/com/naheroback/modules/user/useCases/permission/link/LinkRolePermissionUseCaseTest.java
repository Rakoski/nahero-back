package br.com.naheroback.modules.user.useCases.permission.link;

import br.com.naheroback.common.exceptions.custom.DuplicateException;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.modules.user.entities.Permission;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.repositories.PermissionRepository;
import br.com.naheroback.modules.user.repositories.RoleRepository;
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
class LinkRolePermissionUseCaseTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @InjectMocks
    private LinkRolePermissionUseCase linkRolePermissionUseCase;

    private Role mockRole;
    private Permission permission1;
    private Permission permission2;
    private Permission permission3;
    private LinkRolePermissionRequest request;

    @BeforeEach
    void setUp() {
        mockRole = new Role();
        mockRole.setId(1);
        mockRole.setName("ADMIN");
        mockRole.setPermissions(new HashSet<>());

        permission1 = new Permission();
        permission1.setId(1);
        permission1.setDescription("READ_USER");

        permission2 = new Permission();
        permission2.setId(2);
        permission2.setDescription("WRITE_USER");

        permission3 = new Permission();
        permission3.setId(3);
        permission3.setDescription("DELETE_USER");

        mockRole.getPermissions().add(permission1);

        request = new LinkRolePermissionRequest(1, List.of(2, 3));
    }

    @Test
    @DisplayName("Should link permissions to role successfully")
    void shouldLinkPermissionsToRoleSuccessfully() {
        when(roleRepository.findById(request.roleId())).thenReturn(Optional.of(mockRole));
        when(permissionRepository.findById(2)).thenReturn(Optional.of(permission2));
        when(permissionRepository.findById(3)).thenReturn(Optional.of(permission3));

        linkRolePermissionUseCase.execute(request);

        assertEquals(3, mockRole.getPermissions().size());
        assertTrue(mockRole.getPermissions().contains(permission1));
        assertTrue(mockRole.getPermissions().contains(permission2));
        assertTrue(mockRole.getPermissions().contains(permission3));

        verify(roleRepository, times(1)).findById(request.roleId());
        verify(permissionRepository, times(1)).findById(2);
        verify(permissionRepository, times(1)).findById(3);
        verify(roleRepository, times(1)).save(mockRole);
    }

    @Test
    @DisplayName("Should throw NotFoundException when role not found")
    void shouldThrowNotFoundExceptionWhenRoleNotFound() {
        when(roleRepository.findById(request.roleId())).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> linkRolePermissionUseCase.execute(request)
        );

        assertTrue(exception.getMessage().contains("Role"));
        assertTrue(exception.getMessage().contains(request.roleId().toString()));

        verify(roleRepository, times(1)).findById(request.roleId());
        verify(permissionRepository, never()).findById(anyInt());
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when permission not found")
    void shouldThrowNotFoundExceptionWhenPermissionNotFound() {
        when(roleRepository.findById(request.roleId())).thenReturn(Optional.of(mockRole));
        when(permissionRepository.findById(2)).thenReturn(Optional.of(permission2));
        when(permissionRepository.findById(3)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> linkRolePermissionUseCase.execute(request)
        );

        assertTrue(exception.getMessage().contains("Permission"));
        assertTrue(exception.getMessage().contains("3"));

        verify(roleRepository, times(1)).findById(request.roleId());
        verify(permissionRepository, times(1)).findById(2);
        verify(permissionRepository, times(1)).findById(3);
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw DuplicateException when permission already linked to role")
    void shouldThrowDuplicateExceptionWhenPermissionAlreadyLinkedToRole() {
        LinkRolePermissionRequest duplicateRequest = new LinkRolePermissionRequest(1, List.of(1));

        when(roleRepository.findById(duplicateRequest.roleId())).thenReturn(Optional.of(mockRole));

        DuplicateException exception = assertThrows(
                DuplicateException.class,
                () -> linkRolePermissionUseCase.execute(duplicateRequest)
        );

        assertTrue(exception.getMessage().contains("roleId and permissionId"));
        assertTrue(exception.getMessage().contains("1 and 1"));

        verify(roleRepository, times(1)).findById(duplicateRequest.roleId());
        verify(permissionRepository, never()).findById(anyInt());
        verify(roleRepository, never()).save(any(Role.class));
    }
}