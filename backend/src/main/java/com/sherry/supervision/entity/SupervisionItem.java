package com.sherry.supervision.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("supervision_item")
public class SupervisionItem extends BaseEntity {

    private UUID batchId;
    private String itemNo;
    private String title;
    private String description;
    private Integer sourceRowNo;
    private String priority;
    private String status;
    private OffsetDateTime deadlineAt;
    private OffsetDateTime completedAt;
    private String createdBy;
}
