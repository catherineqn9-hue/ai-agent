<template>
  <div class="page work-page">
    <div class="toolbar">
      <button class="primary" @click="openCreate">新增事项</button>
      <button @click="openImport">Excel 导入</button>
      <button @click="openBatchList">导入批次</button>
      <button @click="$emit('refresh')">刷新</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>编号</th>
            <th>标题</th>
            <th>优先级</th>
            <th>状态</th>
            <th>截止时间</th>
            <th>创建人</th>
            <th class="actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td class="mono">{{ item.item_no || "-" }}</td>
            <td>{{ item.title }}</td>
            <td>{{ priorityLabel(item.priority, item.priority_name) }}</td>
            <td><span class="status-pill" :class="statusClass(item.status)">{{ statusLabel(item.status, item.status_name) }}</span></td>
            <td>{{ formatDateTime(item.deadline_at) }}</td>
            <td>{{ item.created_by || "-" }}</td>
            <td class="actions">
              <button @click="openDetail(item.id)">详情</button>
              <button @click="openEdit(item)">编辑</button>
              <button class="danger" @click="remove(item.id)">删除</button>
            </td>
          </tr>
          <tr v-if="items.length === 0">
            <td colspan="7" class="empty">暂无督办事项</td>
          </tr>
        </tbody>
      </table>
    </div>

    <DialogPanel v-if="detailDialogOpen" title="督办事项详情" @close="detailDialogOpen = false">
      <div v-if="detail" class="dialog-body detail-body">
        <section class="detail-section">
          <h3>{{ detail.item.title }}</h3>
          <div class="detail-grid">
            <span>编号：{{ detail.item.item_no || "-" }}</span>
            <span>状态：{{ statusLabel(detail.item.status, detail.item.status_name) }}</span>
            <span>优先级：{{ priorityLabel(detail.item.priority, detail.item.priority_name) }}</span>
            <span>截止时间：{{ formatDateTime(detail.item.deadline_at) }}</span>
            <span>创建人：{{ detail.item.created_by || "-" }}</span>
            <span>完成时间：{{ formatDateTime(detail.item.completed_at) }}</span>
          </div>
          <p class="detail-description">{{ detail.item.description || "暂无描述" }}</p>
        </section>

        <section class="detail-section">
          <div class="section-title">
            <h3>进度反馈</h3>
            <span>{{ detail.feedbacks.length }} 条</span>
          </div>
          <div class="feedback-list">
            <article v-for="feedback in detail.feedbacks" :key="feedback.id" class="feedback-item">
              <div class="feedback-meta">
                <strong>{{ feedback.feedback_user_name || feedback.feedback_user_id || "未知用户" }}</strong>
                <span>{{ feedback.progress_percent ?? 0 }}%</span>
                <span>{{ formatDateTime(feedback.feedback_at) }}</span>
              </div>
              <p>{{ feedback.content }}</p>
              <small v-if="feedback.risk_note">风险：{{ feedback.risk_note }}</small>
            </article>
            <div v-if="detail.feedbacks.length === 0" class="empty compact-empty">暂无进度反馈</div>
          </div>
        </section>

        <section class="detail-section" data-agent-id="assignment-console">
          <div class="section-title">
            <h3>智能分配督办</h3>
            <span data-agent-field="assignment_policy">AI 仅生成建议，确认后才分配</span>
          </div>
          <form class="assignment-form" @submit.prevent="submitAssignment">
            <label>
              <span>期望角色</span>
              <select v-model="assignmentForm.role_type" data-agent-field="role_type">
                <option value="owner">主责人</option>
                <option value="collaborator">协办人</option>
                <option value="reviewer">审核人</option>
              </select>
            </label>
            <label>
              <span>部门</span>
              <select v-model="assignmentForm.department_id" data-agent-field="department_id">
                <option value="operations">运营部</option>
              </select>
            </label>
            <label>
              <span>分配给</span>
              <select v-model="assignmentForm.assignee_user_id" data-agent-field="assignee_user_id" required @change="applySelectedUser">
                <option value="">请选择用户</option>
                <option v-for="user in assignableUsers" :key="user.username" :value="user.username">
                  {{ user.display_name }} / {{ user.role_name || roleLabel(user.role_key) }}
                </option>
              </select>
            </label>
            <label class="wide">
              <span>分派说明</span>
              <input v-model="assignmentForm.assignment_note" data-agent-field="assignment_note" />
            </label>
            <div class="dialog-actions wide-actions">
              <button type="button" data-agent-action="generate_assignment_recommendations" @click="loadRecommendations">
                生成 AI 建议
              </button>
              <button class="primary" data-agent-action="confirm_assignment" type="submit">确认分配</button>
            </div>
          </form>

          <div class="candidate-list" data-agent-id="assignment-candidates">
            <article v-for="candidate in assignmentCandidates" :key="candidate.assignee_user_id" class="candidate-item">
              <div>
                <strong data-agent-field="candidate_name">{{ candidate.assignee_name }}</strong>
                <span data-agent-field="candidate_role">{{ candidate.role_name }}</span>
                <span data-agent-field="candidate_confidence">置信度：{{ Math.round(candidate.confidence * 100) }}%</span>
              </div>
              <p data-agent-field="candidate_reason">{{ candidate.reason }}</p>
              <button type="button" data-agent-action="apply_assignment_candidate" @click="applyCandidate(candidate)">
                采用此建议
              </button>
            </article>
            <div v-if="assignmentCandidates.length === 0" class="empty compact-empty">暂无 AI 分配建议</div>
          </div>
        </section>

        <section class="detail-section">
          <div class="section-title">
            <h3>责任人</h3>
            <span>{{ detail.assignees?.length || 0 }} 人</span>
          </div>
          <div class="assignee-list">
            <div v-for="assignee in detail.assignees" :key="assignee.id" class="assignee-item">
              <strong>{{ assignee.assignee_name }}</strong>
              <span>{{ assignee.department_name || "-" }}</span>
              <span>{{ assignee.role_type_name || roleLabel(assignee.role_type) }}</span>
              <span>{{ assignee.confirm_status_name || confirmStatusLabel(assignee.confirm_status) }}</span>
            </div>
            <div v-if="!detail.assignees?.length" class="empty compact-empty">暂无责任人</div>
          </div>
        </section>

        <form class="feedback-form" @submit.prevent="submitFeedback">
          <label>
            <span>反馈内容</span>
            <textarea v-model="feedbackForm.content" rows="3" required></textarea>
          </label>
          <label>
            <span>进度百分比</span>
            <input v-model.number="feedbackForm.progressPercent" type="number" min="0" max="100" />
          </label>
          <label>
            <span>风险说明</span>
            <input v-model="feedbackForm.riskNote" />
          </label>
          <div class="dialog-actions">
            <button class="primary" type="submit">添加反馈</button>
          </div>
        </form>
      </div>
    </DialogPanel>

    <DialogPanel v-if="itemDialogOpen" :title="form.id ? '编辑督办事项' : '新增督办事项'" @close="itemDialogOpen = false">
      <form class="form-grid" @submit.prevent="submitItem">
        <label>
          <span>事项编号</span>
          <input v-model="form.values.item_no" placeholder="留空自动生成" />
        </label>
        <label>
          <span>标题</span>
          <input v-model="form.values.title" required />
        </label>
        <label class="wide">
          <span>描述</span>
          <textarea v-model="form.values.description" rows="4"></textarea>
        </label>
        <label>
          <span>优先级</span>
          <select v-model="form.values.priority">
            <option value="low">低</option>
            <option value="normal">普通</option>
            <option value="high">高</option>
            <option value="urgent">紧急</option>
          </select>
        </label>
        <label>
          <span>状态</span>
          <select v-model="form.values.status">
            <option v-for="option in statusOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
        <label>
          <span>截止时间</span>
          <input v-model="form.values.deadline_at" type="datetime-local" />
        </label>
        <label>
          <span>创建人</span>
          <input v-model="form.values.created_by" />
        </label>
        <div class="dialog-actions">
          <button type="button" @click="itemDialogOpen = false">取消</button>
          <button class="primary" type="submit">保存</button>
        </div>
      </form>
    </DialogPanel>

    <DialogPanel v-if="importDialogOpen" title="Excel 导入督办事项" @close="importDialogOpen = false">
      <form class="form-grid compact" @submit.prevent="submitImport">
        <label>
          <span>导入模板</span>
          <select v-model="importForm.templateCode" required>
            <option v-for="template in templates" :key="template.template_code" :value="template.template_code">
              {{ template.template_name }}
            </option>
          </select>
        </label>
        <label>
          <span>Excel 文件</span>
          <input type="file" accept=".xlsx,.xls" required @change="handleFile" />
        </label>
        <div class="template-preview">
          <strong>字段映射</strong>
          <p v-if="selectedTemplate">
            {{ selectedTemplate.source_columns?.join("、") }} -> {{ selectedTemplate.entity_fields?.join("、") }}
          </p>
          <p v-else>请选择模板</p>
        </div>
        <div class="dialog-actions">
          <button type="button" @click="importDialogOpen = false">取消</button>
          <button class="primary" type="submit">导入</button>
        </div>
      </form>
    </DialogPanel>

    <DialogPanel v-if="batchDialogOpen" title="Excel 导入批次" @close="batchDialogOpen = false">
      <div class="dialog-body">
        <div class="table-wrap compact-table">
          <table>
            <thead>
              <tr>
                <th>批次号</th>
                <th>批次名称</th>
                <th>状态</th>
                <th>总数</th>
                <th>成功</th>
                <th>失败</th>
                <th class="actions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="batch in importBatches" :key="batch.id">
                <td class="mono">{{ batch.batch_no }}</td>
                <td>{{ batch.batch_name }}</td>
                <td>{{ importStatusLabel(batch.import_status, batch.import_status_name) }}</td>
                <td>{{ batch.total_count }}</td>
                <td>{{ batch.success_count }}</td>
                <td>{{ batch.failed_count }}</td>
                <td class="actions"><button @click="openBatchDetail(batch.id)">详情</button></td>
              </tr>
              <tr v-if="importBatches.length === 0">
                <td colspan="7" class="empty">暂无导入批次</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </DialogPanel>

    <DialogPanel v-if="batchDetailOpen" title="导入批次详情" @close="batchDetailOpen = false">
      <div v-if="batchDetail" class="dialog-body batch-detail">
        <div class="batch-summary">
          <span>批次：{{ batchDetail.batch.batch_no }}</span>
          <span>总数：{{ batchDetail.batch.total_count }}</span>
          <span>成功：{{ batchDetail.batch.success_count }}</span>
          <span>失败：{{ batchDetail.batch.failed_count }}</span>
        </div>
        <h3>失败行明细</h3>
        <div class="table-wrap compact-table">
          <table>
            <thead>
              <tr>
                <th>行号</th>
                <th>编号</th>
                <th>标题</th>
                <th>错误原因</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="error in batchDetail.errors" :key="error.id">
                <td>{{ error.row_no || "-" }}</td>
                <td class="mono">{{ error.item_no || "-" }}</td>
                <td>{{ error.title || "-" }}</td>
                <td>{{ error.error_message }}</td>
              </tr>
              <tr v-if="batchDetail.errors.length === 0">
                <td colspan="4" class="empty">暂无失败行</td>
              </tr>
            </tbody>
          </table>
        </div>
        <h3>成功落库事项</h3>
        <div class="table-wrap compact-table">
          <table>
            <thead>
              <tr>
                <th>行号</th>
                <th>编号</th>
                <th>标题</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in batchDetail.items" :key="item.id">
                <td>{{ item.source_row_no || "-" }}</td>
                <td class="mono">{{ item.item_no }}</td>
                <td>{{ item.title }}</td>
                <td>{{ statusLabel(item.status, item.status_name) }}</td>
              </tr>
              <tr v-if="batchDetail.items.length === 0">
                <td colspan="4" class="empty">暂无成功事项</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </DialogPanel>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import DialogPanel from "./DialogPanel.vue";

const props = defineProps({
  items: { type: Array, default: () => [] },
  templates: { type: Array, default: () => [] },
  users: { type: Array, default: () => [] },
  loadTemplates: { type: Function, required: true },
  loadBatches: { type: Function, required: true },
  loadBatchDetail: { type: Function, required: true },
  loadDetail: { type: Function, required: true },
  loadUsers: { type: Function, required: true },
  loadAssignmentRecommendations: { type: Function, required: true },
  assignItem: { type: Function, required: true },
  createFeedback: { type: Function, required: true },
  importExcel: { type: Function, required: true },
  currentUser: { type: Object, default: null }
});
const emit = defineEmits(["refresh", "save", "delete", "notify"]);
const itemDialogOpen = ref(false);
const importDialogOpen = ref(false);
const batchDialogOpen = ref(false);
const batchDetailOpen = ref(false);
const detailDialogOpen = ref(false);
const form = ref(emptyForm());
const importForm = ref({ templateCode: "", file: null });
const importBatches = ref([]);
const batchDetail = ref(null);
const detail = ref(null);
const feedbackForm = ref(emptyFeedbackForm());
const assignmentCandidates = ref([]);
const assignmentForm = ref(emptyAssignmentForm());

const statusOptions = [
  { value: "pending_assign", label: "待分派" },
  { value: "in_progress", label: "进行中" },
  { value: "blocked", label: "受阻" },
  { value: "completed", label: "已完成" },
  { value: "cancelled", label: "已取消" }
];

const selectedTemplate = computed(() => props.templates.find((item) => item.template_code === importForm.value.templateCode));
const assignableUsers = computed(() => props.users.filter((user) => user.enabled !== false && user.department_id === assignmentForm.value.department_id));

async function openDetail(itemId) {
  const data = await props.loadDetail(itemId);
  if (!data) return;
  detail.value = data;
  feedbackForm.value = emptyFeedbackForm();
  assignmentForm.value = emptyAssignmentForm();
  assignmentCandidates.value = [];
  detailDialogOpen.value = true;
}

function openCreate() {
  form.value = emptyForm();
  itemDialogOpen.value = true;
}

function openEdit(item) {
  form.value = {
    id: item.id,
    values: {
      item_no: item.item_no || "",
      title: item.title || "",
      description: item.description || "",
      priority: item.priority || "normal",
      status: item.status || "pending_assign",
      deadline_at: toDatetimeLocal(item.deadline_at),
      created_by: item.created_by || props.currentUser?.username || "admin"
    }
  };
  itemDialogOpen.value = true;
}

function submitItem() {
  const values = form.value.values;
  emit("save", {
    id: form.value.id,
    payload: {
      item_no: values.item_no || null,
      title: values.title,
      description: values.description || null,
      priority: values.priority || "normal",
      status: values.status || "pending_assign",
      deadline_at: values.deadline_at || null,
      created_by: values.created_by || "admin"
    }
  });
  itemDialogOpen.value = false;
}

async function submitFeedback() {
  if (!detail.value?.item?.id) return;
  await props.createFeedback({
    item_id: detail.value.item.id,
    feedback_user_id: props.currentUser?.username || "admin_user",
    feedback_user_name: props.currentUser?.display_name || props.currentUser?.username || "管理员",
    progress_percent: feedbackForm.value.progressPercent,
    content: feedbackForm.value.content,
    risk_note: feedbackForm.value.riskNote || null
  });
  const data = await props.loadDetail(detail.value.item.id);
  if (data) {
    detail.value = data;
  }
  feedbackForm.value = emptyFeedbackForm();
}

async function loadRecommendations() {
  if (!detail.value?.item?.id) return;
  await props.loadUsers();
  const data = await props.loadAssignmentRecommendations(detail.value.item.id, {
    role_type: assignmentForm.value.role_type,
    department_id: assignmentForm.value.department_id
  });
  assignmentCandidates.value = data?.candidates || [];
}

function applyCandidate(candidate) {
  assignmentForm.value = {
    ...assignmentForm.value,
    assignee_user_id: candidate.assignee_user_id,
    assignee_name: candidate.assignee_name,
    department_id: candidate.department_id || "operations",
    department_name: candidate.department_name || "运营部",
    role_type: candidate.role_type || assignmentForm.value.role_type,
    assignment_note: candidate.reason || assignmentForm.value.assignment_note
  };
}

function applySelectedUser() {
  const user = props.users.find((item) => item.username === assignmentForm.value.assignee_user_id);
  if (!user) return;
  assignmentForm.value.assignee_name = user.display_name || user.username;
  assignmentForm.value.department_id = user.department_id || "operations";
  assignmentForm.value.department_name = user.department_name || "运营部";
}

async function submitAssignment() {
  if (!detail.value?.item?.id || !assignmentForm.value.assignee_user_id) return;
  applySelectedUser();
  await props.assignItem(detail.value.item.id, assignmentForm.value);
  const data = await props.loadDetail(detail.value.item.id);
  if (data) detail.value = data;
  assignmentForm.value = emptyAssignmentForm();
  assignmentCandidates.value = [];
}

async function openImport() {
  await props.loadTemplates();
  importForm.value = {
    templateCode: props.templates[0]?.template_code || "",
    file: null
  };
  importDialogOpen.value = true;
}

async function openBatchList() {
  const data = await props.loadBatches();
  importBatches.value = data?.batches || [];
  batchDialogOpen.value = true;
}

async function openBatchDetail(batchId) {
  const data = await props.loadBatchDetail(batchId);
  if (!data) return;
  batchDetail.value = data;
  batchDetailOpen.value = true;
}

function handleFile(event) {
  importForm.value.file = event.target.files?.[0] || null;
}

async function submitImport() {
  if (!importForm.value.file) {
    emit("notify", { type: "error", title: "请选择文件", message: "请先选择 Excel 文件" });
    return;
  }
  const result = await props.importExcel({
    templateCode: importForm.value.templateCode,
    file: importForm.value.file
  });
  importDialogOpen.value = false;
  if (result) {
    emit("notify", {
      type: "success",
      title: "导入完成",
      message: `成功 ${result.success_count} 条，失败 ${result.failed_count} 条`
    });
  }
}

function remove(id) {
  if (confirm("确认删除这条督办事项？")) {
    emit("delete", id);
  }
}

function emptyForm() {
  return {
    id: "",
    values: {
      item_no: "",
      title: "",
      description: "",
      priority: "normal",
      status: "pending_assign",
      deadline_at: "",
      created_by: props.currentUser?.username || "admin"
    }
  };
}

function emptyFeedbackForm() {
  return {
    content: "",
    progressPercent: 0,
    riskNote: ""
  };
}

function statusLabel(status, statusName) {
  return statusName || statusOptions.find((item) => item.value === status)?.label || status || "-";
}

function emptyAssignmentForm() {
  return {
    assignee_user_id: "",
    assignee_name: "",
    department_id: "operations",
    department_name: "运营部",
    role_type: "owner",
    assignment_note: ""
  };
}

function statusClass(status) {
  return String(status || "").replaceAll("_", "-");
}

function priorityLabel(priority, priorityName) {
  return priorityName || { low: "低", normal: "普通", high: "高", urgent: "紧急" }[priority] || priority || "-";
}

function roleLabel(role) {
  return { owner: "主责人", collaborator: "协办人", reviewer: "审核人", member: "成员" }[role] || role || "-";
}

function confirmStatusLabel(status) {
  return { pending: "待确认", confirmed: "已确认", rejected: "已拒绝" }[status] || status || "-";
}

function importStatusLabel(status, statusName) {
  return statusName || { pending: "待导入", parsing: "解析中", completed: "已完成", failed: "失败" }[status] || status || "-";
}

function formatDateTime(value) {
  if (!value) return "-";
  return String(value).replace("T", " ").slice(0, 16);
}

function toDatetimeLocal(value) {
  if (!value) return "";
  return String(value).slice(0, 16);
}
</script>
