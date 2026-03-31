package com.rpa.management.repository;

import com.rpa.management.entity.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long>, JpaSpecificationExecutor<ExecutionLog> {
}
