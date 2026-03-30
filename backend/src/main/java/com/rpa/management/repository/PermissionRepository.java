package com.rpa.management.repository;

import com.rpa.management.common.enums.PermissionStatus;
import com.rpa.management.common.enums.PermissionType;
import com.rpa.management.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);

    List<Permission> findAllByStatus(PermissionStatus status);

    List<Permission> findAllByStatusAndType(PermissionStatus status, PermissionType type);

    List<Permission> findAllByIdInAndStatus(Collection<Long> ids, PermissionStatus status);

    List<Permission> findAllByIdIn(Collection<Long> ids);

    boolean existsByParentId(Long parentId);
}
