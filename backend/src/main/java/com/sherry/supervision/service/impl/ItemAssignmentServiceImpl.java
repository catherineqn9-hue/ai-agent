package com.sherry.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sherry.supervision.dto.AssignItemRequest;
import com.sherry.supervision.dto.RejectAssignmentRequest;
import com.sherry.supervision.entity.AppUser;
import com.sherry.supervision.entity.ItemAssignee;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.exception.BusinessConflictException;
import com.sherry.supervision.exception.ResourceNotFoundException;
import com.sherry.supervision.mapper.ItemAssigneeMapper;
import com.sherry.supervision.service.ItemAssignmentService;
import com.sherry.supervision.service.SupervisionItemService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemAssignmentServiceImpl implements ItemAssignmentService {

    private static final String ROLE_OWNER = "owner";
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String STATUS_REJECTED = "rejected";
    private static final Set<String> ROLE_TYPES = Set.of(ROLE_OWNER, "collaborator", "reviewer");

    private final ItemAssigneeMapper itemAssigneeMapper;
    private final SupervisionItemService supervisionItemService;

    public ItemAssignmentServiceImpl(
            ItemAssigneeMapper itemAssigneeMapper,
            SupervisionItemService supervisionItemService) {
        this.itemAssigneeMapper = itemAssigneeMapper;
        this.supervisionItemService = supervisionItemService;
    }

    @Override
    public ItemAssignee assign(UUID itemId, AssignItemRequest request, AppUser assignedBy) {
        supervisionItemService.get(itemId);
        String roleType = defaultValue(request.roleType(), ROLE_OWNER);
        if (!ROLE_TYPES.contains(roleType)) {
            throw new BusinessConflictException("未知分派角色：" + roleType);
        }
        ItemAssignee assignee = new ItemAssignee();
        assignee.setId(UUID.randomUUID());
        assignee.setItemId(itemId);
        assignee.setAssignedByUserId(assignedBy.getUsername());
        assignee.setAssignedByName(assignedBy.getDisplayName());
        assignee.setAssigneeUserId(request.assigneeUserId());
        assignee.setAssigneeName(request.assigneeName());
        assignee.setDepartmentId(request.departmentId());
        assignee.setDepartmentName(request.departmentName());
        assignee.setRoleType(roleType);
        assignee.setConfirmStatus(STATUS_PENDING);
        assignee.setAssignmentNote(request.assignmentNote());
        assignee.setAssignedAt(OffsetDateTime.now());
        assignee.setCreatedAt(OffsetDateTime.now());
        assignee.setUpdatedAt(OffsetDateTime.now());
        itemAssigneeMapper.insert(assignee);
        return assignee;
    }

    @Override
    public List<ItemAssignee> listByItem(UUID itemId) {
        return itemAssigneeMapper.selectList(new LambdaQueryWrapper<ItemAssignee>()
                .eq(ItemAssignee::getItemId, itemId)
                .orderByDesc(ItemAssignee::getAssignedAt, ItemAssignee::getCreatedAt));
    }

    @Override
    public List<ItemAssignee> listByAssignee(String username) {
        return itemAssigneeMapper.selectList(new LambdaQueryWrapper<ItemAssignee>()
                .eq(ItemAssignee::getAssigneeUserId, username)
                .orderByDesc(ItemAssignee::getAssignedAt, ItemAssignee::getCreatedAt));
    }

    @Override
    public ItemAssignee getCurrentAssignment(UUID itemId, String username) {
        ItemAssignee assignment = itemAssigneeMapper.selectOne(new LambdaQueryWrapper<ItemAssignee>()
                .eq(ItemAssignee::getItemId, itemId)
                .eq(ItemAssignee::getAssigneeUserId, username)
                .last("LIMIT 1"));
        if (assignment == null) {
            throw new ResourceNotFoundException("当前用户没有该督办事项的分派记录");
        }
        return assignment;
    }

    @Override
    public ItemAssignee confirmReceive(UUID itemId, AppUser currentUser) {
        ItemAssignee assignment = getCurrentAssignment(itemId, currentUser.getUsername());
        if (STATUS_REJECTED.equals(assignment.getConfirmStatus())) {
            throw new BusinessConflictException("已拒绝的分派不能确认接收");
        }
        assignment.setConfirmStatus(STATUS_CONFIRMED);
        assignment.setConfirmedAt(OffsetDateTime.now());
        assignment.setUpdatedAt(OffsetDateTime.now());
        itemAssigneeMapper.updateById(assignment);

        SupervisionItem item = supervisionItemService.get(itemId);
        if ("pending_assign".equals(item.getStatus())) {
            supervisionItemService.updateStatus(itemId, "in_progress");
        }
        return assignment;
    }

    @Override
    public ItemAssignee reject(UUID itemId, RejectAssignmentRequest request, AppUser currentUser) {
        ItemAssignee assignment = getCurrentAssignment(itemId, currentUser.getUsername());
        if (STATUS_CONFIRMED.equals(assignment.getConfirmStatus())) {
            throw new BusinessConflictException("已确认接收的分派不能拒绝");
        }
        itemAssigneeMapper.update(null, new UpdateWrapper<ItemAssignee>()
                .eq("id", assignment.getId())
                .set("confirm_status", STATUS_REJECTED)
                .set("rejection_reason", request.rejectionReason())
                .set("updated_at", OffsetDateTime.now()));
        assignment.setConfirmStatus(STATUS_REJECTED);
        assignment.setRejectionReason(request.rejectionReason());
        assignment.setUpdatedAt(OffsetDateTime.now());
        return assignment;
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
