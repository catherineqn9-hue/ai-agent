package com.sherry.supervision.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.sherry.supervision.config.JsonTypeHandler;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName(value = "excel_import_template", autoResultMap = true)
public class ExcelImportTemplate extends BaseEntity {

    private String templateCode;
    private String templateName;
    private String description;
    private String handlerCode;

    @TableField(typeHandler = JsonTypeHandler.class)
    private JsonNode sourceColumns;

    @TableField(typeHandler = JsonTypeHandler.class)
    private JsonNode entityFields;

    @TableField(typeHandler = JsonTypeHandler.class)
    private JsonNode mappingConfig;

    private Boolean enabled;
}
