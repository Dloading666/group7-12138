package com.rpa.management.repository;

import com.rpa.management.entity.CrawlResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CrawlResultRepository extends JpaRepository<CrawlResult, Long> {

    Optional<CrawlResult> findByTaskId(String taskId);

    Optional<CrawlResult> findByTaskRecordId(Long taskRecordId);

    void deleteByTaskId(String taskId);

    @Query("""
            SELECT c FROM CrawlResult c
            WHERE (:keyword IS NULL OR c.title LIKE %:keyword% OR c.summaryText LIKE %:keyword% OR c.finalUrl LIKE %:keyword%)
              AND (:taskId IS NULL OR c.taskId LIKE %:taskId%)
              AND (:status IS NULL OR c.status = :status)
              AND (:startTime IS NULL OR c.createTime >= :startTime)
              AND (:endTime IS NULL OR c.createTime <= :endTime)
            """)
    Page<CrawlResult> findByConditions(
            @Param("keyword") String keyword,
            @Param("taskId") String taskId,
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );
}
