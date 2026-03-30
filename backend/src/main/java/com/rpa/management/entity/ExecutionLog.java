package com.rpa.management.entity;

import com.rpa.management.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
@Entity
@Table(name = "execution_logs", indexes = {
    @Index(name = "idx_execution_logs_task_id", columnList = "taskId")
})
public class ExecutionLog extends BaseEntity {

    private Long taskId;
    private Long robotId;

    @Column(length = 32)
    private String level;

    @Column(columnDefinition = "LONGTEXT")
    private String message;
}
