package com.sherry.supervision.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("supervision_batch")
public class SupervisionBatch extends BaseEntity {

    private String batchNo;
    private String batchName;
    private String sourceType;
    private String sourceFileId;
    private String importStatus;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private String createdBy;
}
