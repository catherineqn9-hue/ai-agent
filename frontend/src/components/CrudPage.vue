<template>
  <div class="page work-page">
    <div class="toolbar">
      <button v-if="!isReadOnly" class="primary" @click="openCreate">新增</button>
      <button @click="$emit('refresh')">刷新</button>
    </div>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th v-for="field in tableFields" :key="field">{{ fieldLabel(field) }}</th>
            <th v-if="!isReadOnly" class="actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in items" :key="item.id">
            <td v-for="field in tableFields" :key="field" :class="{ mono: isMono(field) }">
              {{ formatCell(item[field]) }}
            </td>
            <td v-if="!isReadOnly" class="actions">
              <button @click="openEdit(item)">编辑</button>
              <button class="danger" @click="remove(item.id)">删除</button>
            </td>
          </tr>
          <tr v-if="items.length === 0">
            <td :colspan="tableFields.length + (isReadOnly ? 0 : 1)" class="empty">暂无数据</td>
          </tr>
        </tbody>
      </table>
    </div>

    <DialogPanel v-if="dialogOpen" :title="form.id ? '编辑配置' : '新增配置'" @close="dialogOpen = false">
      <form class="form-grid" @submit.prevent="submit">
        <label v-for="field in editableFields" :key="field.name">
          <span>{{ field.label || field.name }}</span>
          <textarea v-if="field.type === 'textarea' || field.type === 'json'" v-model="form.values[field.name]" rows="4" />
          <select v-else-if="field.type === 'boolean'" v-model="form.values[field.name]">
            <option :value="true">启用</option>
            <option :value="false">停用</option>
          </select>
          <input v-else-if="field.type === 'number'" v-model.number="form.values[field.name]" type="number" />
          <input v-else v-model="form.values[field.name]" :required="field.required" />
        </label>
        <div class="dialog-actions">
          <button type="button" @click="dialogOpen = false">取消</button>
          <button class="primary" type="submit">保存</button>
        </div>
      </form>
    </DialogPanel>
  </div>
</template>

<script setup>
import { computed, ref, watch } from "vue";
import DialogPanel from "./DialogPanel.vue";

const props = defineProps({
  menu: { type: Object, required: true },
  items: { type: Array, default: () => [] }
});
const emit = defineEmits(["refresh", "save", "delete", "notify"]);
const dialogOpen = ref(false);
const form = ref({ id: "", values: {} });
const fallbackLabels = {
  agent_key: "Agent Key",
  agent_name: "Agent 名称",
  enabled: "状态",
  required: "必填",
  sort_order: "排序",
  request_id: "请求 ID",
  thread_id: "会话 ID",
  agent_key: "Agent",
  tool_name: "工具名称",
  input_payload: "入参",
  output_payload: "出参",
  status: "状态",
  error_message: "错误信息",
  duration_ms: "耗时(ms)",
  created_at: "创建时间"
};

const editableFields = computed(() => props.menu.fields || []);
const isReadOnly = computed(() => props.menu.readOnly || props.menu.read_only);
const tableFields = computed(() => props.menu.tableFields?.length
  ? props.menu.tableFields
  : editableFields.value.slice(0, 5).map((field) => field.name));

watch(() => props.menu.id, () => {
  dialogOpen.value = false;
  clearForm();
});

function openCreate() {
  clearForm();
  dialogOpen.value = true;
}

function openEdit(item) {
  const values = {};
  editableFields.value.forEach((field) => {
    const value = item[field.name];
    values[field.name] = field.type === "json"
      ? JSON.stringify(value ?? defaultJsonValue(field), null, 2)
      : value ?? defaultFieldValue(field);
  });
  form.value = { id: item.id, values };
  dialogOpen.value = true;
}

function submit() {
  const payload = {};
  for (const field of editableFields.value) {
    const value = form.value.values[field.name];
    if (field.type === "json") {
      try {
        payload[field.name] = JSON.parse(value || field.default || "{}");
      } catch (error) {
        emit("notify", {
          type: "error",
          title: "格式错误",
          message: `${field.label || field.name} JSON 格式不正确`
        });
        return;
      }
    } else {
      payload[field.name] = value;
    }
  }
  emit("save", { id: form.value.id, payload });
  dialogOpen.value = false;
}

function remove(id) {
  if (confirm("确认删除这条配置？")) {
    emit("delete", id);
  }
}

function clearForm() {
  const values = {};
  editableFields.value.forEach((field) => {
    values[field.name] = defaultFieldValue(field);
  });
  form.value = { id: "", values };
}

function defaultFieldValue(field) {
  if (field.default !== undefined) return field.default;
  if (field.type === "boolean") return false;
  if (field.type === "number") return 0;
  if (field.type === "json") return field.default || "{}";
  return "";
}

function defaultJsonValue(field) {
  try {
    return JSON.parse(field.default || "{}");
  } catch (error) {
    return {};
  }
}

function fieldLabel(field) {
  const match = editableFields.value.find((item) => item.name === field);
  return match?.label || fallbackLabels[field] || field;
}

function isMono(field) {
  return field.endsWith("_key")
    || field.endsWith("_id")
    || field === "provider"
    || field === "base_url"
    || field.includes("payload")
    || field === "error_message";
}

function formatCell(value) {
  if (value === null || value === undefined || value === "") return "-";
  if (typeof value === "boolean") return value ? "启用" : "停用";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}
</script>
