package com.rpa.management.dto;

import lombok.Data;

@Data
public class WorkflowAssistantDraftRequest {

    private String prompt;

    private String currentGraph;

    private String currentInputSchema;
}
