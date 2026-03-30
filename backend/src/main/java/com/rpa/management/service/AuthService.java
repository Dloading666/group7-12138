package com.rpa.management.service;

import com.rpa.management.common.exception.BadRequestBusinessException;
import com.rpa.management.common.exception.ForbiddenBusinessException;
import com.rpa.management.common.exception.ResourceNotFoundException;
import com.rpa.management.common.enums.UserStatus;
import com.rpa.management.dto.LoginRequest;
import com.rpa.management.dto.LoginResponse;
import com.rpa.management.dto.RoleDto;
import com.rpa.management.dto.UserDto;
import com.rpa.management.entity.Role;
import com.rpa.management.entity.User;
import com.rpa.management.repository.PermissionRepository;
import com.rpa.management.repository.RoleRepository;
import com.rpa.management.repository.UserRepository;
import com.rpa.management.security.JwtProperties;
import com.rpa.management.security.JwtService;
import com.rpa.management.security.PermissionAccessService;
import com.rpa.management.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final PermissionAccessService permissionAccessService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
            .orElseThrow(() -> new BadRequestBusinessException("Invalid username or password"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ForbiddenBusinessException("User is disabled");
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadRequestBusinessException("Invalid username or password");
        }
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        return buildSessionResponse(user, jwtService.generateToken(user.getId(), user.getUsername()));
    }

    @Transactional(readOnly = true)
    public LoginResponse me(UserPrincipal principal) {
        User user = userRepository.findById(principal.id())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return buildSessionResponse(user, null);
    }

    public void logout() {
        // JWT is stateless; client removes the token.
    }

    private LoginResponse buildSessionResponse(User user, String token) {
        Role role = roleRepository.findById(user.getRoleId())
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        List<String> permissionCodes = permissionAccessService.resolveEffectivePermissionCodes(user).stream().toList();
        return new LoginResponse(
            token,
            token,
            "Bearer",
            jwtProperties.expiration(),
            UserDto.from(user, role),
            RoleDto.from(role),
            permissionCodes,
            permissionAccessService.buildMenuTree(user)
        );
    }
}
