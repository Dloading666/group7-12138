package com.rpa.management.service;

import com.rpa.management.common.enums.UserStatus;
import com.rpa.management.common.exception.BadRequestBusinessException;
import com.rpa.management.common.exception.ForbiddenBusinessException;
import com.rpa.management.common.exception.ResourceNotFoundException;
import com.rpa.management.dto.ChangePasswordRequest;
import com.rpa.management.dto.PermissionScopeDto;
import com.rpa.management.dto.RoleDto;
import com.rpa.management.dto.UserDto;
import com.rpa.management.dto.UserPasswordRequest;
import com.rpa.management.dto.UserPermissionOverrideRequest;
import com.rpa.management.dto.UserProfileUpdateRequest;
import com.rpa.management.dto.UserStatusRequest;
import com.rpa.management.dto.UserUpsertRequest;
import com.rpa.management.entity.Role;
import com.rpa.management.entity.User;
import com.rpa.management.repository.RoleRepository;
import com.rpa.management.repository.UserRepository;
import com.rpa.management.security.PermissionAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionAccessService permissionAccessService;

    @Transactional(readOnly = true)
    public List<UserDto> listAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
            .map(user -> UserDto.from(user, findRole(user.getRoleId())))
            .toList();
    }

    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        User user = getUser(id);
        return UserDto.from(user, findRole(user.getRoleId()));
    }

    @Transactional
    public UserDto create(UserUpsertRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestBusinessException("Username already exists");
        }
        Role role = findRole(request.roleId());
        if ("ADMIN".equals(role.getCode())) {
            throw new ForbiddenBusinessException("Only the fixed super admin can use the ADMIN role");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestBusinessException("Password is required");
        }
        User user = new User()
            .setUsername(request.username())
            .setPassword(passwordEncoder.encode(request.password()))
            .setRealName(request.realName())
            .setEmail(request.email())
            .setPhone(request.phone())
            .setAvatar(request.avatar())
            .setRoleId(role.getId())
            .setStatus(request.status())
            .setSuperAdmin(false);
        return UserDto.from(userRepository.save(user), role);
    }

    @Transactional
    public UserDto update(Long id, UserUpsertRequest request) {
        User user = getUser(id);
        Role role = findRole(request.roleId());
        if (user.isSuperAdmin() && !"ADMIN".equals(role.getCode())) {
            throw new ForbiddenBusinessException("Super admin must keep the ADMIN role");
        }
        if (!user.isSuperAdmin() && "ADMIN".equals(role.getCode())) {
            throw new ForbiddenBusinessException("Only the fixed super admin can use the ADMIN role");
        }
        userRepository.findByUsername(request.username())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> { throw new BadRequestBusinessException("Username already exists"); });
        user.setUsername(request.username())
            .setRealName(request.realName())
            .setEmail(request.email())
            .setPhone(request.phone())
            .setAvatar(request.avatar())
            .setRoleId(role.getId())
            .setStatus(request.status());
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
        return UserDto.from(userRepository.save(user), role);
    }

    @Transactional
    public void delete(Long id) {
        User user = getUser(id);
        if (user.isSuperAdmin()) {
            throw new ForbiddenBusinessException("Super admin cannot be deleted");
        }
        userRepository.delete(user);
    }

    @Transactional
    public UserDto updateStatus(Long id, UserStatusRequest request) {
        User user = getUser(id);
        if (user.isSuperAdmin() && request.status() != UserStatus.ACTIVE) {
            throw new ForbiddenBusinessException("Super admin cannot be disabled");
        }
        user.setStatus(request.status());
        return UserDto.from(userRepository.save(user), findRole(user.getRoleId()));
    }

    @Transactional
    public UserDto updatePassword(Long id, UserPasswordRequest request) {
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestBusinessException("Password is required");
        }
        User user = getUser(id);
        user.setPassword(passwordEncoder.encode(request.password()));
        return UserDto.from(userRepository.save(user), findRole(user.getRoleId()));
    }

    @Transactional
    public UserDto updateProfile(Long id, UserProfileUpdateRequest request) {
        User user = getUser(id);
        user.setRealName(request.realName())
            .setEmail(request.email())
            .setPhone(request.phone())
            .setAvatar(request.avatar());
        return UserDto.from(userRepository.save(user), findRole(user.getRoleId()));
    }

    @Transactional
    public UserDto updateAvatar(Long id, String avatarUrl) {
        User user = getUser(id);
        user.setAvatar(avatarUrl);
        return UserDto.from(userRepository.save(user), findRole(user.getRoleId()));
    }

    @Transactional
    public UserDto changeOwnPassword(Long id, ChangePasswordRequest request) {
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new BadRequestBusinessException("New password is required");
        }
        User user = getUser(id);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            throw new BadRequestBusinessException("Original password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        return UserDto.from(userRepository.save(user), findRole(user.getRoleId()));
    }

    @Transactional(readOnly = true)
    public PermissionScopeDto getEffectivePermissions(Long userId) {
        getUser(userId);
        return permissionAccessService.buildPermissionScope(userId);
    }

    @Transactional(readOnly = true)
    public PermissionScopeDto getPermissionOverrides(Long userId) {
        getUser(userId);
        return permissionAccessService.buildPermissionScope(userId);
    }

    @Transactional
    public PermissionScopeDto replacePermissionOverrides(Long userId, UserPermissionOverrideRequest request) {
        getUser(userId);
        permissionAccessService.replaceUserOverrides(userId, request.grants(), request.revokes());
        return permissionAccessService.buildPermissionScope(userId);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Role findRole(Long id) {
        return roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }
}
