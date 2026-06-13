<template>
  <div
    class="page my-supervision-page"
    data-agent-id="my-supervision-console"
    :data-page-state="pageState"
  >
    <pre id="agent-page-context" class="agent-json" data-agent-id="agent-page-context">{{ agentPageContext }}</pre>
    <pre id="agent-action-manifest" class="agent-json" data-agent-id="agent-action-manifest">{{ actionManifest }}</pre>

    <section class="summary-strip" data-agent-id="my-supervision-summary">
      <div>
        <span>当前用户</span>
        <strong data-agent-field="current_user">{{ currentUser?.display_name || currentUser?.username || "-" }}</strong>
      </div>
      <div>
        <span>我的任务</span>
        <strong data-agent-field="total_count">{{ items.length }}</strong>
      </div>
      <div>
        <span>待确认</span>
        <strong data-agent-field="pending_confirm_count">{{ pendingCount }}</strong>
      </div>
      <div>
        <span>高风险</span>
        <strong data-agent-field="high_risk_count">{{ highRiskCount }}</strong>
      </div>
    </section>

    <section class="my-work-area">
      <div class="table-wrap">
        <table data-agent-id="my-supervision-list">
          <thead>
            <tr>
              <th>编号</th>
              <th>标题</th>
              <th>状态</th>
              <th>优先级</th>
              <th>分配人</th>
              <th>分配时间</th>
              <th>接收状态</th>
              <th class="actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in items"
              :key="row.assignment.id"
              :data-agent-row-id="`assignment-${row.assignment.id}`"
              :data-item-id="row.item.id"
              :data-risk-level="row.risk_level"
              :data-human-review-required="row.requires_human_review"
              :class="{ selected: selectedItemId === row.item.id }"
            >
              <td class="mono" data-agent-field="item_no">{{ row.item.item_no || "-" }}</td>
              <td data-agent-field="title">{{ row.item.title }}</td>
              <td data-agent-field="status">{{ statusLabel(row.item.status, row.item.status_name) }}</td>
              <td data-agent-field="priority">{{ priorityLabel(row.item.priority, row.item.priority_name) }}</td>
              <td data-agent-field="assigned_by">{{ row.assignment.assigned_by_name || row.assignment.assigned_by_user_id }}</td>
              <td data-agent-field="assigned_at">{{ formatDateTime(row.assignment.assigned_at) }}</td>
              <td data-agent-field="confirm_status">{{ confirmStatusLabel(row.assignment.confirm_status, row.assignment.confirm_status_name) }}</td>
              <td class="actions">
                <button data-agent-action="open_my_item_detail" @click="openDetail(row.item.id)">查看</button>
              </td>
            </tr>
            <tr v-if="items.length === 0">
              <td colspan="8" class="empty">暂无分配给你的督办事项</td>
            </tr>
          </tbody>
        </table>
      </div>

      <aside
        class="my-detail-panel"
        data-agent-id="my-supervision-detail"
        :data-loading-state="detailLoading ? 'loading' : 'ready'"
        :data-risk-level="selectedRiskLevel"
      >
        <div v-if="!detail" class="empty compact-empty">请选择一条任务查看详情</div>
        <template v-else>
          <section class="detail-section">
            <div class="section-title">
              <h3>{{ detail.item.title }}</h3>
              <span>{{ detail.item.item_no }}</span>
            </div>
            <div class="detail-grid">
              <span data-agent-field="assigned_by">分配人：{{ detail.assignment.assigned_by_name }}</span>
              <span data-agent-field="assigned_at">分配时间：{{ formatDateTime(detail.assignment.assigned_at) }}</span>
              <span data-agent-field="role_type">我的角色：{{ roleLabel(detail.assignment.role_type, detail.assignment.role_type_name) }}</span>
              <span data-agent-field="confirm_status">接收状态：{{ confirmStatusLabel(detail.assignment.confirm_status, detail.assignment.confirm_status_name) }}</span>
              <span data-agent-field="deadline_at">截止时间：{{ formatDateTime(detail.item.deadline_at) }}</span>
              <span data-agent-field="item_status">事项状态：{{ statusLabel(detail.item.status, detail.item.status_name) }}</span>
            </div>
            <p class="detail-description" data-agent-field="assignment_note">
              分派说明：{{ detail.assignment.assignment_note || "无" }}
            </p>
            <p class="detail-description" data-agent-field="item_description">
              事项描述：{{ detail.item.description || "暂无描述" }}
            </p>
          </section>

          <section class="detail-section" data-agent-id="my-supervision-actions">
            <div class="section-title">
              <h3>任务操作</h3>
              <span>{{ selectedRiskLevel }} risk</span>
            </div>
            <div class="action-row">
              <button
                class="primary"
                data-agent-action="confirm_receive"
                :disabled="detail.assignment.confirm_status === 'confirmed'"
                @click="confirmReceive"
              >
                确认接收
              </button>
              <button
                data-agent-action="reject_assignment"
                :disabled="detail.assignment.confirm_status === 'confirmed'"
                @click="rejectDialogOpen = true"
              >
                拒绝接收
              </button>
              <button data-agent-action="add_progress_feedback" @click="feedbackOpen = !feedbackOpen">
                添加进度反馈
              </button>
            </div>
          </section>

          <form v-if="feedbackOpen" class="feedback-form" data-agent-id="my-feedback-form" @submit.prevent="submitFeedback">
            <label>
              <span>反馈内容</span>
              <textarea v-model="feedbackForm.content" data-agent-field="feedback_content" rows="3" required></textarea>
            </label>
            <label>
              <span>进度百分比</span>
              <input v-model.number="feedbackForm.progressPercent" data-agent-field="progress_percent" type="number" min="0" max="100" />
            </label>
            <label>
              <span>风险说明</span>
              <input v-model="feedbackForm.riskNote" data-agent-field="risk_note" />
            </label>
            <div class="dialog-actions">
              <button class="primary" data-agent-action="submit_progress_feedback" type="submit">保存进度反馈</button>
            </div>
          </form>

          <section class="detail-section">
            <div class="section-title">
              <h3>责任人</h3>
              <span>{{ detail.assignees.length }} 人</span>
            </div>
            <div class="assignee-list">
              <div v-for="assignee in detail.assignees" :key="assignee.id" class="assignee-item">
                <strong data-agent-field="assignee_name">{{ assignee.assignee_name }}</strong>
                <span data-agent-field="assignee_role">{{ roleLabel(assignee.role_type, assignee.role_type_name) }}</span>
                <span data-agent-field="assignee_status">{{ confirmStatusLabel(assignee.confirm_status, assignee.confirm_status_name) }}</span>
              </div>
            </div>
          </section>

          <section class="detail-section">
            <div class="section-title">
              <h3>进度反馈</h3>
              <span>{{ detail.feedbacks.length }} 条</span>
            </div>
            <div class="feedback-list">
              <article v-for="feedback in detail.feedbacks" :key="feedback.id" class="feedback-item">
                <div class="feedback-meta">
                  <strong>{{ feedback.feedback_user_name || feedback.feedback_user_id }}</strong>
                  <span>{{ feedback.progress_percent ?? 0 }}%</span>
                  <span>{{ formatDateTime(feedback.feedback_at) }}</span>
                </div>
                <p>{{ feedback.content }}</p>
                <small v-if="feedback.risk_note">风险：{{ feedback.risk_note }}</small>
              </article>
              <div v-if="detail.feedbacks.length === 0" class="empty compact-empty">暂无进度反馈</div>
            </div>
          </section>

          <section class="detail-section ai-suggestion-panel" data-agent-id="my-supervision-ai-panel" data-state="provider_unavailable">
            <div class="section-title">
              <h3>AI 督办建议</h3>
              <span>待接入</span>
            </div>
            <p>当前先展示任务上下文和可操作动作；后续接入 Kimi 后，由 AI 基于分派人、截止时间、进度反馈和风险说明生成建议。</p>
          </section>
        </template>
      </aside>
    </section>

    <DialogPanel v-if="rejectDialogOpen" title="拒绝接收任务" @close="rejectDialogOpen = false">
      <form class="form-grid compact" @submit.prevent="rejectAssignment">
        <label>
          <span>拒绝原因</span>
          <textarea v-model="rejectReason" data-agent-field="rejection_reason" rows="4" required></textarea>
        </label>
        <div class="dialog-actions">
          <button type="button" @click="rejectDialogOpen = false">取消</button>
          <button class="primary" data-agent-action="submit_reject_assignment" type="submit">提交拒绝原因</button>
        </div>
      </form>
    </DialogPanel>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import DialogPanel from "./DialogPanel.vue";

const props = defineProps({
  items: { type: Array, default: () => [] },
  currentUser: { type: Object, default: null },
  loadDetail: { type: Function, required: true },
  confirmReceiveAction: { type: Function, required: true },
  rejectAssignmentAction: { type: Function, required: true },
  createFeedback: { type: Function, required: true },
  refreshItems: { type: Function, required: true }
});

const detail = ref(null);
const selectedItemId = ref("");
const detailLoading = ref(false);
const feedbackOpen = ref(false);
const rejectDialogOpen = ref(false);
const rejectReason = ref("");
const feedbackForm = ref(emptyFeedbackForm());

const pendingCount = computed(() => props.items.filter((row) => row.assignment?.confirm_status === "pending").length);
const highRiskCount = computed(() => props.items.filter((row) => row.risk_level === "high").length);
const pageState = computed(() => props.items.length === 0 ? "empty" : "ready");
const selectedRiskLevel = computed(() => {
  const selected = props.items.find((row) => row.item?.id === selectedItemId.value);
  return selected?.risk_level || "low";
});

const agentPageContext = computed(() => JSON.stringify({
  page: "my_supervision_console",
  current_user: props.currentUser,
  task_scope: "assigned_to_me",
  selected_item: detail.value ? {
    item_id: detail.value.item.id,
    item_no: detail.value.item.item_no,
    status: detail.value.item.status,
    status_name: detail.value.item.status_name,
    priority: detail.value.item.priority,
    priority_name: detail.value.item.priority_name,
    assigned_by: detail.value.assignment.assigned_by_user_id,
    confirm_status: detail.value.assignment.confirm_status,
    confirm_status_name: detail.value.assignment.confirm_status_name,
    risk_level: selectedRiskLevel.value
  } : null
}));

const actionManifest = computed(() => JSON.stringify({
  page: "my_supervision_console",
  actions: [
    { id: "confirm_receive", label: "确认接收督办事项", risk_level: "low", requires_human_review: false, enabled: Boolean(detail.value) },
    { id: "reject_assignment", label: "拒绝接收督办事项", risk_level: "medium", requires_human_review: false, enabled: Boolean(detail.value) },
    { id: "add_progress_feedback", label: "添加进度反馈", risk_level: "low", requires_human_review: false, enabled: Boolean(detail.value) },
    { id: "final_complete", label: "最终关闭督办事项", risk_level: "high", requires_human_review: true, enabled: false, disabled_reason: "需要管理员确认" }
  ]
}));

async function openDetail(itemId) {
  detailLoading.value = true;
  selectedItemId.value = itemId;
  const data = await props.loadDetail(itemId);
  if (data) {
    detail.value = data;
    feedbackForm.value = emptyFeedbackForm();
    feedbackOpen.value = false;
  }
  detailLoading.value = false;
}

async function confirmReceive() {
  if (!detail.value?.item?.id) return;
  await props.confirmReceiveAction(detail.value.item.id);
  await props.refreshItems();
  await openDetail(detail.value.item.id);
}

async function rejectAssignment() {
  if (!detail.value?.item?.id) return;
  await props.rejectAssignmentAction(detail.value.item.id, { rejection_reason: rejectReason.value });
  rejectDialogOpen.value = false;
  rejectReason.value = "";
  await props.refreshItems();
  await openDetail(detail.value.item.id);
}

async function submitFeedback() {
  if (!detail.value?.item?.id) return;
  await props.createFeedback({
    item_id: detail.value.item.id,
    feedback_user_id: props.currentUser?.username || "unknown",
    feedback_user_name: props.currentUser?.display_name || props.currentUser?.username || "当前用户",
    progress_percent: feedbackForm.value.progressPercent,
    content: feedbackForm.value.content,
    risk_note: feedbackForm.value.riskNote || null
  });
  feedbackForm.value = emptyFeedbackForm();
  await openDetail(detail.value.item.id);
}

function emptyFeedbackForm() {
  return {
    content: "",
    progressPercent: 0,
    riskNote: ""
  };
}

function statusLabel(status, statusName) {
  if (statusName) return statusName;
  return {
    pending_assign: "待分派",
    in_progress: "进行中",
    blocked: "受阻",
    completed: "已完成",
    cancelled: "已取消"
  }[status] || status || "-";
}

function priorityLabel(priority, priorityName) {
  return priorityName || { low: "低", normal: "普通", high: "高", urgent: "紧急" }[priority] || priority || "-";
}

function roleLabel(role, roleName) {
  return roleName || { owner: "主责人", collaborator: "协办人", reviewer: "审核人" }[role] || role || "-";
}

function confirmStatusLabel(status, statusName) {
  return statusName || { pending: "待确认", confirmed: "已确认", rejected: "已拒绝" }[status] || status || "-";
}

function formatDateTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}
</script>
