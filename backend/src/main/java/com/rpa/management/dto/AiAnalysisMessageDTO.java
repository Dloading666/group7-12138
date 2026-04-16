package com.rpa.management.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiAnalysisMessageDTO {

    private Long id;

    private Long analysisTaskId;

    private String role;

    private String content;

    private LocalDateTime createTime;
}
