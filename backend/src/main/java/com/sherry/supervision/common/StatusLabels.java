package com.sherry.supervision.common;

import java.util.Map;

public final class StatusLabels {

    private static final Map<String, String> ITEM_STATUS_LABELS = Map.of(
            "pending_assign", "待分派",
            "in_progress", "进行中",
            "blocked", "受阻",
            "completed", "已完成",
            "cancelled", "已取消");

    private static final Map<String, String> ITEM_STATUS_CODES = Map.of(
            "待分派", "pending_assign",
            "进行中", "in_progress",
            "受阻", "blocked",
            "已完成", "completed",
            "已取消", "cancelled");

    private static final Map<String, String> ASSIGNMENT_STATUS_LABELS = Map.of(
            "pending", "待确认",
            "confirmed", "已确认",
            "rejected", "已拒绝");

    private static final Map<String, String> PRIORITY_LABELS = Map.of(
            "low", "低",
            "normal", "普通",
            "high", "高",
            "urgent", "紧急");

    private static final Map<String, String> ROLE_LABELS = Map.of(
            "owner", "主责人",
            "collaborator", "协办人",
            "reviewer", "审核人");

    private StatusLabels() {
    }

    public static String itemStatusLabel(String statusCode) {
        if (statusCode == null) {
            return null;
        }
        return ITEM_STATUS_LABELS.getOrDefault(statusCode, statusCode);
    }

    public static String normalizeItemStatus(String status) {
        if (status == null) {
            return null;
        }
        return ITEM_STATUS_CODES.getOrDefault(status, status);
    }

    public static String assignmentStatusLabel(String statusCode) {
        if (statusCode == null) {
            return null;
        }
        return ASSIGNMENT_STATUS_LABELS.getOrDefault(statusCode, statusCode);
    }

    public static String priorityLabel(String priorityCode) {
        if (priorityCode == null) {
            return null;
        }
        return PRIORITY_LABELS.getOrDefault(priorityCode, priorityCode);
    }

    public static String roleLabel(String roleCode) {
        if (roleCode == null) {
            return null;
        }
        return ROLE_LABELS.getOrDefault(roleCode, roleCode);
    }
}
