package com.sherry.supervision.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.sherry.supervision.config.JsonTypeHandler;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName(value = "progress_feedback", autoResultMap = true)
public class ProgressFeedback extends BaseEntity {

    private UUID itemId;
    private UUID assigneeId;
    private String feedbackUserId;
    private String feedbackUserName;
    private Integer progressPercent;
    private String content;
    private String riskNote;

    @TableField(typeHandler = JsonTypeHandler.class)
    private JsonNode attachmentIds;

    private OffsetDateTime feedbackAt;
}
