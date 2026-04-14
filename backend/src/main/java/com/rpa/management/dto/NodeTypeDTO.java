package com.rpa.management.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 节点类型DTO
 */
@Data
public class NodeTypeDTO {
    private Long id;
    private String type;
    private String name;
    private String icon;
    private String color;
    private String category;
    private Integer sortOrder;
    private Boolean enabled;
    private String defaultConfig;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
