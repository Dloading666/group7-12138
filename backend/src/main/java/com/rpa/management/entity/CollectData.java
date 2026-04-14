package com.rpa.management.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 采集结果数据实体
 */
@Data
@Entity
@Table(name = "sys_collect_data")
public class CollectData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 采集配置ID
     */
    @Column(name = "config_id", nullable = false)
    private Long configId;
    
    /**
     * 任务ID
     */
    @Column(name = "task_id")
    private Long taskId;
    
    /**
     * 机器人ID
     */
    @Column(name = "robot_id")
    private Long robotId;
    
    /**
     * 数据来源URL
     */
    @Column(name = "source_url", length = 500)
    private String sourceUrl;
    
    /**
     * 数据唯一标识（用于去重）
     */
    @Column(name = "data_hash", length = 64)
    private String dataHash;
    
    /**
     * 采集的数据内容（JSON格式）
     */
    @Lob
    @Column(name = "data_content")
    private String dataContent;

    /**
     * 原始HTML（可选保存）
     */
    @Lob
    @Column(name = "raw_html")
    private String rawHtml;
    
    /**
     * 采集时间
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime collectTime;
    
    /**
     * 数据状态：valid-有效, invalid-无效, duplicate-重复
     */
    @Column(length = 20)
    private String status = "valid";
    
    /**
     * 备注
     */
    @Column(length = 500)
    private String remark;
}
