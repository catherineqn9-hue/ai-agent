package com.sherry.supervision.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("app_user")
public class AppUser extends BaseEntity {

    private String username;
    private String displayName;
    private String departmentId;
    private String departmentName;
    private String roleKey;
    private String roleName;
    private String passwordHash;
    private String passwordSalt;
    private String sessionTokenHash;
    private OffsetDateTime sessionExpiresAt;
    private Boolean enabled;
}
