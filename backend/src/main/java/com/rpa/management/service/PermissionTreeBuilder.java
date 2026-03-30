package com.rpa.management.service;

import com.rpa.management.common.enums.PermissionType;
import com.rpa.management.dto.PermissionNodeDto;
import com.rpa.management.entity.Permission;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PermissionTreeBuilder {

    private PermissionTreeBuilder() {
    }

    public static List<PermissionNodeDto> buildFullTree(List<Permission> permissions) {
        Map<Long, List<Permission>> childrenMap = buildChildrenMap(permissions);
        List<PermissionNodeDto> result = new ArrayList<>();
        permissions.stream()
            .filter(permission -> isRoot(permission))
            .sorted(Comparator.comparingInt(Permission::getSortOrder).thenComparing(Permission::getId))
            .forEach(permission -> result.add(buildNode(permission, childrenMap)));
        return result;
    }

    public static List<PermissionNodeDto> buildMenuTree(List<Permission> permissions, Set<String> effectiveCodes) {
        Map<Long, List<Permission>> childrenMap = buildChildrenMap(permissions);
        List<PermissionNodeDto> result = new ArrayList<>();
        permissions.stream()
            .filter(permission -> permission.getType() == PermissionType.MENU)
            .filter(PermissionTreeBuilder::isRoot)
            .sorted(Comparator.comparingInt(Permission::getSortOrder).thenComparing(Permission::getId))
            .forEach(permission -> {
                PermissionNodeDto node = buildVisibleMenuNode(permission, childrenMap, effectiveCodes);
                if (node != null) {
                    result.add(node);
                }
            });
        return result;
    }

    private static Map<Long, List<Permission>> buildChildrenMap(List<Permission> permissions) {
        Map<Long, List<Permission>> childrenMap = new LinkedHashMap<>();
        for (Permission permission : permissions) {
            Long parentId = permission.getParentId();
            childrenMap.computeIfAbsent(parentId == null ? 0L : parentId, key -> new ArrayList<>()).add(permission);
        }
        childrenMap.values().forEach(children -> children.sort(Comparator.comparingInt(Permission::getSortOrder).thenComparing(Permission::getId)));
        return childrenMap;
    }

    private static PermissionNodeDto buildNode(Permission permission, Map<Long, List<Permission>> childrenMap) {
        List<PermissionNodeDto> children = new ArrayList<>();
        for (Permission child : childrenMap.getOrDefault(permission.getId(), List.of())) {
            children.add(buildNode(child, childrenMap));
        }
        return PermissionNodeDto.from(permission, children);
    }

    private static PermissionNodeDto buildVisibleMenuNode(Permission permission,
                                                          Map<Long, List<Permission>> childrenMap,
                                                          Set<String> effectiveCodes) {
        boolean allowed = effectiveCodes.contains(permission.getCode());
        List<PermissionNodeDto> children = new ArrayList<>();
        for (Permission child : childrenMap.getOrDefault(permission.getId(), List.of())) {
            if (child.getType() == PermissionType.MENU) {
                PermissionNodeDto node = buildVisibleMenuNode(child, childrenMap, effectiveCodes);
                if (node != null) {
                    children.add(node);
                }
            }
        }
        if (!allowed && children.isEmpty()) {
            return null;
        }
        return PermissionNodeDto.from(permission, children);
    }

    private static boolean isRoot(Permission permission) {
        Long parentId = permission.getParentId();
        return parentId == null || parentId == 0L;
    }
}
