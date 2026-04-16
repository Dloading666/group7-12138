package com.rpa.management.repository;

import com.rpa.management.entity.AiAnalysisMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiAnalysisMessageRepository extends JpaRepository<AiAnalysisMessage, Long> {

    List<AiAnalysisMessage> findByAnalysisTaskIdOrderByCreateTimeAsc(Long analysisTaskId);

    long countByAnalysisTaskId(Long analysisTaskId);
}
