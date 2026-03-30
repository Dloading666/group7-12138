package com.rpa.management.security;

import com.rpa.management.common.enums.OverrideMode;
import com.rpa.management.common.enums.PermissionStatus;
import com.rpa.management.common.enums.PermissionType;
import com.rpa.management.common.exception.ForbiddenBusinessException;
import com.rpa.management.common.exception.ResourceNotFoundException;
import com.rpa.management.dto.PermissionNodeDto;
import com.rpa.management.dto.PermissionScopeDto;
import com.rpa.management.entity.Permission;
import com.rpa.management.entity.Role;
import com.rpa.management.entity.RolePermission;
import com.rpa.management.entity.User;
import com.rpa.management.entity.UserPermissionOverride;
import com.rpa.management.repository.PermissionRepository;
import com.rpa.management.repository.RolePermissionRepository;
import com.rpa.management.repository.RoleRepository;
import com.rpa.management.repository.UserPermissionOverrideRepository;
import com.rpa.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionAccessService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionOverrideRepository userPermissionOverrideRepository;

    @Transactional(readOnly = true)
    public UserPrincipal loadPrincipal(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Set<String> codes = resolveEffectivePermissionCodes(user);
        return new UserPrincipal(user.getId(), user.getUsername(), user.getRealName(),
            resolveRoleCode(user.getRoleId()), user.isSuperAdmin(), codes);
    }

    @Transactional(readOnly = true)
    public Set<String> resolveEffectivePermissionCodes(User user) {
        if (user.isSuperAdmin()) {
            return permissionRepository.findAllByStatus(PermissionStatus.ACTIVE).stream()
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        Set<Long> effectiveIds = new LinkedHashSet<>();
        effectiveIds.addAll(resolveRolePermissionIds(user.getRoleId()));

        List<UserPermissionOverride> overrides = userPermissionOverrideRepository.findAllByUserId(user.getId());
        for (UserPermissionOverride override : overrides) {
            if (override.getMode() == OverrideMode.GRANT) {
                effectiveIds.add(override.getPermissionId());
            } else {
                effectiveIds.remove(override.getPermissionId());
            }
        }

        return permissionRepository.findAllByIdInAndStatus(effectiveIds, PermissionStatus.ACTIVE)
            .stream()
            .map(Permission::getCode)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional(readOnly = true)
    public List<PermissionNodeDto> buildMenuTree(User user) {
        Set<String> effectiveCodes = resolveEffectivePermissionCodes(user);
        List<Permission> allMenus = permissionRepository.findAllByStatusAndType(
            PermissionStatus.ACTIVE,
            PermissionType.MENU);
        List<Permission> allPermissions = permissionRepository.findAllByStatus(PermissionStatus.ACTIVE);

        Map<Long, List<Permission>> childrenMap = new LinkedHashMap<>();
        for (Permission permission : allPermissions) {
            Long parentId = permission.getParentId();
            childrenMap.computeIfAbsent(parentId == null ? 0L : parentId, key -> new ArrayList<>()).add(permission);
        }
        childrenMap.values().forEach(children -> children.sort(Comparator.comparingInt(Permission::getSortOrder).thenComparing(Permission::getId)));

        List<Permission> roots = allMenus.stream()
            .filter(permission -> permission.getParentId() == null || permission.getParentId() == 0)
            .sorted(Comparator.comparingInt(Permission::getSortOrder).thenComparing(Permission::getId))
            .toList();

        List<PermissionNodeDto> result = new ArrayList<>();
        for (Permission root : roots) {
            PermissionNodeDto node = buildMenuNode(root, effectiveCodes, childrenMap);
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }

    private PermissionNodeDto buildMenuNode(Permission permission,
                                            Set<String> effectiveCodes,
                                            Map<Long, List<Permission>> childrenMap) {
        boolean allowed = effectiveCodes.contains(permission.getCode());
        List<PermissionNodeDto> children = new ArrayList<>();
        for (Permission child : childrenMap.getOrDefault(permission.getId(), List.of())) {
            if (child.getType() == PermissionType.MENU) {
                PermissionNodeDto childNode = buildMenuNode(child, effectiveCodes, childrenMap);
                if (childNode != null) {
                    children.add(childNode);
                }
            }
        }
        if (!allowed && children.isEmpty()) {
            return null;
        }
        return PermissionNodeDto.from(permission, children);
    }

    @Transactional(readOnly = true)
    public PermissionScopeDto buildPermissionScope(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Role role = roleRepository.findById(user.getRoleId())
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        Set<Long> rolePermissionIds = resolveRolePermissionIds(role.getId());
        Set<Long> granted = new LinkedHashSet<>();
        Set<Long> revoked = new LinkedHashSet<>();
        for (UserPermissionOverride override : userPermissionOverrideRepository.findAllByUserId(userId)) {
            if (override.getMode() == OverrideMode.GRANT) {
                granted.add(override.getPermissionId());
            } else {
                revoked.add(override.getPermissionId());
            }
        }
        Set<Long> effective = new LinkedHashSet<>(rolePermissionIds);
        effective.addAll(granted);
        effective.removeAll(revoked);

        List<String> effectiveCodes = permissionRepository.findAllByIdInAndStatus(effective, PermissionStatus.ACTIVE)
            .stream().map(Permission::getCode).toList();

        return new PermissionScopeDto(
            user.getId(),
            rolePermissionIds.stream().toList(),
            granted.stream().toList(),
            revoked.stream().toList(),
            effective.stream().toList(),
            effectiveCodes,
            buildMenuTree(user)
        );
    }

    @Transactional(readOnly = true)
    public Set<Long> resolveRolePermissionIds(Long roleId) {
        return rolePermissionRepository.findAllByRoleId(roleId).stream()
            .map(RolePermission::getPermissionId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional(readOnly = true)
    public String resolveRoleCode(Long roleId) {
        return roleRepository.findById(roleId).map(Role::getCode)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    @Transactional
    public void replaceUserOverrides(Long userId, Collection<Long> grantIds, Collection<Long> revokeIds) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.isSuperAdmin()) {
            throw new ForbiddenBusinessException("Super admin permissions cannot be changed");
        }

        userPermissionOverrideRepository.deleteAllByUserId(userId);
        Set<Long> uniqueGrantIds = new LinkedHashSet<>(grantIds == null ? List.of() : grantIds);
        Set<Long> uniqueRevokeIds = new LinkedHashSet<>(revokeIds == null ? List.of() : revokeIds);

        for (Long permissionId : uniqueGrantIds) {
            assertPermissionExists(permissionId);
            userPermissionOverrideRepository.save(UserPermissionOverride.grant(userId, permissionId));
        }
        for (Long permissionId : uniqueRevokeIds) {
            assertPermissionExists(permissionId);
            userPermissionOverrideRepository.save(UserPermissionOverride.revoke(userId, permissionId));
        }
    }

    private void assertPermissionExists(Long permissionId) {
        if (!permissionRepository.existsById(permissionId)) {
            throw new ResourceNotFoundException("Permission not found: " + permissionId);
        }
    }

    @Transactional(readOnly = true)
    public UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenBusinessException("Not authenticated");
        }
        return principal;
    }
}
