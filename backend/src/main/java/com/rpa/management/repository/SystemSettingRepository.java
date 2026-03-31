package com.rpa.management.repository;

import com.rpa.management.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    List<SystemSetting> findAllBySettingGroupOrderBySettingKeyAsc(String settingGroup);

    Optional<SystemSetting> findBySettingGroupAndSettingKey(String settingGroup, String settingKey);

    boolean existsBySettingGroup(String settingGroup);
}
