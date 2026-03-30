package com.rpa.management.repository;

import com.rpa.management.entity.UserPermissionOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPermissionOverrideRepository extends JpaRepository<UserPermissionOverride, Long> {

    List<UserPermissionOverride> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);

    boolean existsByPermissionId(Long permissionId);
}
