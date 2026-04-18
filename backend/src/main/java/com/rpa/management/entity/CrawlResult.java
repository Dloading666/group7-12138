package com.rpa.management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 真实网站采集结果
 */
@Data
@Entity
@Table(name = "sys_crawl_result")
public class CrawlResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_record_id")
    private Long taskRecordId;

    @Column(name = "task_run_id")
    private Long taskRunId;

    @Column(name = "task_id", nullable = false, unique = true, length = 50)
    private String taskId;

    @Column(name = "task_name", length = 100)
    private String taskName;

    @Column(name = "final_url", length = 1000)
    private String finalUrl;

    @Column(length = 500)
    private String title;

    @Lob
    @Column(name = "summary_text", columnDefinition = "LONGTEXT")
    private String summaryText;

    @Lob
    @Column(name = "raw_html", columnDefinition = "LONGTEXT")
    private String rawHtml;

    @Lob
    @Column(name = "structured_data", columnDefinition = "LONGTEXT")
    private String structuredData;

    @Column(name = "total_count")
    private Integer totalCount = 0;

    @Column(name = "crawled_pages")
    private Integer crawledPages = 0;

    @Column(length = 20)
    private String status = "pending";

    @Lob
    @Column(name = "error_message", columnDefinition = "LONGTEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;
}
