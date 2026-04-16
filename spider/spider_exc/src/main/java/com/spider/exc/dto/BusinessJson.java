package com.spider.exc.dto;

import lombok.Data;

/**
 * 业务JSON（最终结果）
 */
@Data
public class BusinessJson {
    private Long collectionId;
    private Long parsingId;
    private Long processingId;
    private String taxNo;
    private String uscCode;
    private String appDate;
    
    // 各步骤的结果
    private CollectedPayload collectedData;
    private ParsedData parsedData;
    private ProcessedResult processedResult;
}
