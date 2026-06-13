package com.sherry.supervision.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.sherry.supervision.config.JsonTypeHandler;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName(value = "supervision_import_error", autoResultMap = true)
public class SupervisionImportError extends BaseEntity {

    private UUID batchId;
    private Integer rowNo;
    private String itemNo;
    private String title;
    private String errorMessage;

    @TableField(typeHandler = JsonTypeHandler.class)
    private JsonNode rawData;
}
