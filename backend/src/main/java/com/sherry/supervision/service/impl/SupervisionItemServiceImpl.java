package com.sherry.supervision.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sherry.supervision.common.StatusLabels;
import com.sherry.supervision.dto.SupervisionItemRequest;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.exception.BusinessConflictException;
import com.sherry.supervision.exception.ResourceNotFoundException;
import com.sherry.supervision.mapper.SupervisionItemMapper;
import com.sherry.supervision.service.SupervisionItemService;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupervisionItemServiceImpl implements SupervisionItemService {

    private static final String STATUS_PENDING_ASSIGN = "pending_assign";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_BLOCKED = "blocked";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final Set<String> VALID_STATUSES = Set.of(
            STATUS_PENDING_ASSIGN, STATUS_IN_PROGRESS, STATUS_BLOCKED, STATUS_COMPLETED, STATUS_CANCELLED);
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            STATUS_PENDING_ASSIGN, Set.of(STATUS_PENDING_ASSIGN, STATUS_IN_PROGRESS, STATUS_CANCELLED),
            STATUS_IN_PROGRESS, Set.of(STATUS_IN_PROGRESS, STATUS_BLOCKED, STATUS_COMPLETED, STATUS_CANCELLED),
            STATUS_BLOCKED, Set.of(STATUS_BLOCKED, STATUS_IN_PROGRESS, STATUS_CANCELLED),
            STATUS_COMPLETED, Set.of(STATUS_COMPLETED),
            STATUS_CANCELLED, Set.of(STATUS_CANCELLED));

    private final SupervisionItemMapper supervisionItemMapper;

    public SupervisionItemServiceImpl(SupervisionItemMapper supervisionItemMapper) {
        this.supervisionItemMapper = supervisionItemMapper;
    }

    @Override
    public List<SupervisionItem> list(String status, String keyword) {
        String normalizedStatus = StringUtils.hasText(status) ? normalizeStatus(status) : status;
        LambdaQueryWrapper<SupervisionItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(normalizedStatus), SupervisionItem::getStatus, normalizedStatus);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query.like(SupervisionItem::getTitle, keyword)
                    .or()
                    .like(SupervisionItem::getDescription, keyword));
        }
        wrapper.orderByDesc(SupervisionItem::getUpdatedAt, SupervisionItem::getCreatedAt);
        return supervisionItemMapper.selectList(wrapper);
    }

    @Override
    public SupervisionItem get(UUID id) {
        return findById(id);
    }

    @Override
    public SupervisionItem create(SupervisionItemRequest request) {
        String status = normalizeStatus(defaultValue(request.status(), STATUS_PENDING_ASSIGN));
        validateKnownStatus(status);

        SupervisionItem item = new SupervisionItem();
        item.setId(UUID.randomUUID());
        item.setItemNo(StringUtils.hasText(request.itemNo()) ? request.itemNo() : generateItemNo());
        item.setTitle(request.title());
        item.setDescription(request.description());
        item.setPriority(defaultValue(request.priority(), "normal"));
        item.setStatus(status);
        item.setDeadlineAt(request.deadlineAt());
        item.setCreatedBy(defaultValue(request.createdBy(), "admin"));
        item.setCreatedAt(OffsetDateTime.now());
        item.setUpdatedAt(OffsetDateTime.now());
        if (STATUS_COMPLETED.equals(status)) {
            item.setCompletedAt(OffsetDateTime.now());
        }
        supervisionItemMapper.insert(item);
        return item;
    }

    @Override
    public SupervisionItem update(UUID id, SupervisionItemRequest request) {
        SupervisionItem item = findById(id);
        String nextStatus = normalizeStatus(defaultValue(request.status(), item.getStatus()));
        validateTransition(item.getStatus(), nextStatus);

        item.setItemNo(defaultValue(request.itemNo(), item.getItemNo()));
        item.setTitle(defaultValue(request.title(), item.getTitle()));
        item.setDescription(request.description());
        item.setPriority(defaultValue(request.priority(), item.getPriority()));
        item.setStatus(nextStatus);
        if (STATUS_COMPLETED.equals(nextStatus) && item.getCompletedAt() == null) {
            item.setCompletedAt(OffsetDateTime.now());
        }
        item.setDeadlineAt(request.deadlineAt());
        item.setCreatedBy(defaultValue(request.createdBy(), item.getCreatedBy()));
        item.setUpdatedAt(OffsetDateTime.now());
        supervisionItemMapper.updateById(item);
        return item;
    }

    @Override
    public SupervisionItem updateStatus(UUID id, String status) {
        SupervisionItem item = findById(id);
        String nextStatus = normalizeStatus(status);
        validateTransition(item.getStatus(), nextStatus);

        item.setStatus(nextStatus);
        if (STATUS_COMPLETED.equals(nextStatus) && item.getCompletedAt() == null) {
            item.setCompletedAt(OffsetDateTime.now());
        }
        item.setUpdatedAt(OffsetDateTime.now());
        supervisionItemMapper.updateById(item);
        return item;
    }

    @Override
    public void delete(UUID id) {
        supervisionItemMapper.deleteById(id);
    }

    private SupervisionItem findById(UUID id) {
        SupervisionItem item = supervisionItemMapper.selectById(id);
        if (item == null) {
            throw new ResourceNotFoundException("督办事项不存在：" + id);
        }
        return item;
    }

    private String generateItemNo() {
        return "ITEM-" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private String defaultValue(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private void validateTransition(String currentStatus, String nextStatus) {
        validateKnownStatus(nextStatus);
        String current = defaultValue(currentStatus, STATUS_PENDING_ASSIGN);
        validateKnownStatus(current);
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(nextStatus)) {
            throw new BusinessConflictException("状态不允许从 "
                    + StatusLabels.itemStatusLabel(current) + " 流转到 " + StatusLabels.itemStatusLabel(nextStatus));
        }
    }

    private void validateKnownStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessConflictException("未知事项状态：" + StatusLabels.itemStatusLabel(status));
        }
    }

    private String normalizeStatus(String status) {
        return StatusLabels.normalizeItemStatus(status);
    }
}
