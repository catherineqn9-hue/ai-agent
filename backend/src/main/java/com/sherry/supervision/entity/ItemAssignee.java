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
@TableName("item_assignee")
public class ItemAssignee extends BaseEntity {

    private UUID itemId;
    private String assignedByUserId;
    private String assignedByName;
    private String assigneeUserId;
    private String assigneeName;
    private String departmentId;
    private String departmentName;
    private String roleType;
    private String confirmStatus;
    private String assignmentNote;
    private String rejectionReason;
    private OffsetDateTime assignedAt;
    private OffsetDateTime confirmedAt;

    @JsonProperty("confirm_status_name")
    public String getConfirmStatusName() {
        return StatusLabels.assignmentStatusLabel(confirmStatus);
    }

    @JsonProperty("role_type_name")
    public String getRoleTypeName() {
        return StatusLabels.roleLabel(roleType);
    }
}
