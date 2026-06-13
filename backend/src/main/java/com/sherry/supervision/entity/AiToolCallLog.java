package com.sherry.supervision.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.sherry.supervision.config.JsonTypeHandler;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName(value = "ai_tool_call_log", autoResultMap = true)
public class AiToolCallLog {

    @TableId
    private UUID id;

    private String requestId;
    private String threadId;
    private String agentKey;
    private String toolName;

    @TableField(typeHandler = JsonTypeHandler.class)
    private JsonNode inputPayload;

    @TableField(typeHandler = JsonTypeHandler.class)
    private JsonNode outputPayload;

    private String status;
    private String errorMessage;
    private Integer durationMs;
    private OffsetDateTime createdAt;
}
