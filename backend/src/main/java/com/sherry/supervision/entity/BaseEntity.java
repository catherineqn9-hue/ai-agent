package com.sherry.supervision.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseEntity {

    @TableId(type = IdType.INPUT)
    private UUID id;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
