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
@Table(name = "operation_logs", indexes = {
    @Index(name = "idx_operation_logs_user_id", columnList = "userId")
})
public class OperationLog extends BaseEntity {

    private Long userId;

    @Column(length = 64)
    private String username;

    @Column(length = 128)
    private String operation;

    @Column(length = 255)
    private String method;

    @Column(columnDefinition = "LONGTEXT")
    private String params;

    @Column(length = 64)
    private String ip;

    @Column(length = 32)
    private String status;

    @Column(columnDefinition = "LONGTEXT")
    private String errorMsg;

    private Long duration;
}
