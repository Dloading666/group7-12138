package com.rpa.management.service;

import com.rpa.management.common.enums.PermissionStatus;
import com.rpa.management.common.exception.BadRequestBusinessException;
import com.rpa.management.common.exception.ForbiddenBusinessException;
import com.rpa.management.common.exception.ResourceNotFoundException;
import com.rpa.management.dto.PermissionNodeDto;
import com.rpa.management.dto.PermissionUpsertRequest;
import com.rpa.management.entity.Permission;
import com.rpa.management.repository.PermissionRepository;
import com.rpa.management.repository.RolePermissionRepository;
import com.rpa.management.repository.UserPermissionOverrideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionOverrideRepository userPermissionOverrideRepository;

    @Transactional(readOnly = true)
    public List<PermissionNodeDto> listAll() {
        return PermissionTreeBuilder.buildFullTree(permissionRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder", "id")));
    }

    @Transactional(readOnly = true)
    public List<PermissionNodeDto> tree() {
        return listAll();
    }

    @Transactional(readOnly = true)
    public PermissionNodeDto getById(Long id) {
        return permissionRepository.findById(id)
            .map(permission -> PermissionNodeDto.from(permission, List.of()))
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
    }

    @Transactional
    public PermissionNodeDto create(PermissionUpsertRequest request) {
        if (permissionRepository.existsByCode(request.code())) {
            throw new BadRequestBusinessException("Permission code already exists");
        }
        Permission permission = new Permission()
            .setName(request.name())
            .setCode(request.code())
            .setType(request.type())
            .setParentId(request.parentId())
            .setPath(request.path())
            .setComponent(request.component())
            .setIcon(request.icon())
            .setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
            .setStatus(request.status());
        return PermissionNodeDto.from(permissionRepository.save(permission), List.of());
    }

    @Transactional
    public PermissionNodeDto update(Long id, PermissionUpsertRequest request) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        permissionRepository.findByCode(request.code())
            .filter(existing -> !existing.getId().equals(id))
            .ifPresent(existing -> { throw new BadRequestBusinessException("Permission code already exists"); });
        permission.setName(request.name())
            .setCode(request.code())
            .setType(request.type())
            .setParentId(request.parentId())
            .setPath(request.path())
            .setComponent(request.component())
            .setIcon(request.icon())
            .setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
            .setStatus(request.status());
        return PermissionNodeDto.from(permissionRepository.save(permission), List.of());
    }

    @Transactional
    public PermissionNodeDto updateStatus(Long id, PermissionStatus status) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        permission.setStatus(status);
        return PermissionNodeDto.from(permissionRepository.save(permission), List.of());
    }

    @Transactional
    public void delete(Long id) {
        Permission permission = permissionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        if (permissionRepository.existsByParentId(id)) {
            throw new ForbiddenBusinessException("Permission has child nodes");
        }
        if (rolePermissionRepository.existsByPermissionId(id)) {
            throw new ForbiddenBusinessException("Permission is assigned to roles");
        }
        if (userPermissionOverrideRepository.existsByPermissionId(id)) {
            throw new ForbiddenBusinessException("Permission is assigned to users");
        }
        permissionRepository.delete(permission);
    }
}
