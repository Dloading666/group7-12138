package com.rpa.management.service;

import com.rpa.management.common.enums.RoleStatus;
import com.rpa.management.common.exception.BadRequestBusinessException;
import com.rpa.management.common.exception.ForbiddenBusinessException;
import com.rpa.management.common.exception.ResourceNotFoundException;
import com.rpa.management.dto.PermissionNodeDto;
import com.rpa.management.dto.RoleDto;
import com.rpa.management.dto.RolePermissionAssignmentDto;
import com.rpa.management.dto.RolePermissionUpdateRequest;
import com.rpa.management.dto.RoleUpsertRequest;
import com.rpa.management.entity.Role;
import com.rpa.management.entity.RolePermission;
import com.rpa.management.repository.PermissionRepository;
import com.rpa.management.repository.RolePermissionRepository;
import com.rpa.management.repository.RoleRepository;
import com.rpa.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<RoleDto> listAll() {
        return roleRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
            .map(RoleDto::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public RoleDto getById(Long id) {
        return roleRepository.findById(id)
            .map(RoleDto::from)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }

    @Transactional
    public RoleDto create(RoleUpsertRequest request) {
        if (roleRepository.existsByCode(request.code())) {
            throw new BadRequestBusinessException("Role code already exists");
        }
        Role role = new Role()
            .setName(request.name())
            .setCode(request.code())
            .setDescription(request.description())
            .setStatus(request.status())
            .setBuiltIn(Boolean.TRUE.equals(request.builtIn()));
        return RoleDto.from(roleRepository.save(role));
    }

    @Transactional
    public RoleDto update(Long id, RoleUpsertRequest request) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        roleRepository.findByCode(request.code())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> { throw new BadRequestBusinessException("Role code already exists"); });
        role.setName(request.name())
            .setCode(request.code())
            .setDescription(request.description())
            .setStatus(request.status());
        if (request.builtIn() != null) {
            role.setBuiltIn(request.builtIn());
        }
        return RoleDto.from(roleRepository.save(role));
    }

    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        if (role.isBuiltIn()) {
            throw new ForbiddenBusinessException("Built-in role cannot be deleted");
        }
        if (!userRepository.findAllByRoleId(id).isEmpty()) {
            throw new ForbiddenBusinessException("Role is assigned to users");
        }
        rolePermissionRepository.deleteAllByRoleId(id);
        roleRepository.delete(role);
    }

    @Transactional
    public RoleDto updateStatus(Long id, RoleStatus status) {
        Role role = roleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        if (role.isBuiltIn()) {
            throw new ForbiddenBusinessException("Built-in role cannot be disabled");
        }
        if (!userRepository.findAllByRoleId(id).isEmpty()) {
            throw new ForbiddenBusinessException("Role is assigned to users");
        }
        role.setStatus(status);
        return RoleDto.from(roleRepository.save(role));
    }

    @Transactional(readOnly = true)
    public RolePermissionAssignmentDto getPermissions(Long roleId) {
        Role role = roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        Set<Long> permissionIds = resolvePermissionIds(roleId);
        List<PermissionNodeDto> tree = PermissionTreeBuilder.buildFullTree(permissionRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder", "id")));
        return new RolePermissionAssignmentDto(role.getId(), permissionIds.stream().toList(), tree);
    }

    @Transactional
    public RolePermissionAssignmentDto updatePermissions(Long roleId, RolePermissionUpdateRequest request) {
        roleRepository.findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        rolePermissionRepository.deleteAllByRoleId(roleId);
        Set<Long> permissionIds = new LinkedHashSet<>(request.permissionIds() == null ? List.of() : request.permissionIds());
        for (Long permissionId : permissionIds) {
            if (!permissionRepository.existsById(permissionId)) {
                throw new ResourceNotFoundException("Permission not found: " + permissionId);
            }
            rolePermissionRepository.save(RolePermission.of(roleId, permissionId));
        }
        return getPermissions(roleId);
    }

    @Transactional(readOnly = true)
    public Set<Long> resolvePermissionIds(Long roleId) {
        return rolePermissionRepository.findAllByRoleId(roleId).stream()
            .map(RolePermission::getPermissionId)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
