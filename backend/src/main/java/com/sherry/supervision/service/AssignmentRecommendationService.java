package com.sherry.supervision.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sherry.supervision.dto.AssignmentCandidateResponse;
import com.sherry.supervision.entity.AppUser;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.mapper.AppUserMapper;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AssignmentRecommendationService {

    private static final String DEFAULT_DEPARTMENT_ID = "operations";
    private static final String DEFAULT_ROLE_TYPE = "owner";

    private final AppUserMapper appUserMapper;
    private final SupervisionItemService supervisionItemService;

    public AssignmentRecommendationService(AppUserMapper appUserMapper, SupervisionItemService supervisionItemService) {
        this.appUserMapper = appUserMapper;
        this.supervisionItemService = supervisionItemService;
    }

    public List<AssignmentCandidateResponse> recommend(java.util.UUID itemId, String roleType, String departmentId) {
        SupervisionItem item = supervisionItemService.get(itemId);
        String expectedRole = StringUtils.hasText(roleType) ? roleType : DEFAULT_ROLE_TYPE;
        String expectedDepartment = StringUtils.hasText(departmentId) ? departmentId : DEFAULT_DEPARTMENT_ID;
        return appUserMapper.selectList(new LambdaQueryWrapper<AppUser>()
                        .eq(AppUser::getEnabled, true)
                        .eq(AppUser::getDepartmentId, expectedDepartment))
                .stream()
                .sorted(Comparator.comparing((AppUser user) -> roleScore(user, expectedRole)).reversed()
                        .thenComparing(AppUser::getUsername))
                .limit(5)
                .map(user -> toCandidate(item, user, expectedRole, expectedDepartment))
                .toList();
    }

    private AssignmentCandidateResponse toCandidate(
            SupervisionItem item, AppUser user, String expectedRole, String expectedDepartment) {
        boolean roleMatched = expectedRole.equals(user.getRoleKey());
        double confidence = roleMatched ? 0.82 : 0.62;
        String reason = roleMatched
                ? "用户角色匹配" + roleLabel(expectedRole) + "，且属于当前默认部门，可作为督办分派候选。"
                : "用户属于当前默认部门，但角色不是首选角色，仅作为备选候选。";
        if ("urgent".equals(item.getPriority())) {
            confidence = Math.min(0.9, confidence + 0.04);
            reason = reason + " 事项优先级为紧急，仍需人工确认负责人。";
        }
        return new AssignmentCandidateResponse(
                user.getUsername(),
                user.getDisplayName(),
                user.getDepartmentId(),
                user.getDepartmentName(),
                expectedRole,
                roleLabel(expectedRole),
                confidence,
                reason,
                true);
    }

    private int roleScore(AppUser user, String expectedRole) {
        return expectedRole.equals(user.getRoleKey()) ? 10 : 0;
    }

    private String roleLabel(String roleType) {
        return switch (roleType) {
            case "owner" -> "主责人";
            case "collaborator" -> "协办人";
            case "reviewer" -> "审核人";
            default -> roleType;
        };
    }
}
