package com.sherry.supervision.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sherry.supervision.common.StatusLabels;
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

    @JsonProperty("status_name")
    public String getStatusName() {
        return StatusLabels.itemStatusLabel(status);
    }

    @JsonProperty("priority_name")
    public String getPriorityName() {
        return StatusLabels.priorityLabel(priority);
    }
}
