<template>
  <div class="page work-page user-management-page" data-agent-id="user-management-console">
    <section class="summary-strip" data-agent-id="user-management-summary">
      <div>
        <span>用户总数</span>
        <strong data-agent-field="total_count">{{ users.length }}</strong>
      </div>
      <div>
        <span>当前部门</span>
        <strong data-agent-field="department_name">运营部</strong>
      </div>
      <div>
        <span>可分配角色</span>
        <strong data-agent-field="role_count">{{ roleOptions.length }}</strong>
      </div>
    </section>

    <div class="table-wrap">
      <table data-agent-id="user-management-list">
        <thead>
          <tr>
            <th>用户名</th>
            <th>显示名称</th>
            <th>部门</th>
            <th>角色</th>
            <th>状态</th>
            <th class="actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id" :data-agent-row-id="`user-${user.username}`">
            <td class="mono" data-agent-field="username">{{ user.username }}</td>
            <td data-agent-field="display_name">{{ user.display_name }}</td>
            <td data-agent-field="department_name">{{ user.department_name || "-" }}</td>
            <td data-agent-field="role_name">{{ user.role_name || roleLabel(user.role_key) }}</td>
            <td data-agent-field="enabled">{{ user.enabled ? "启用" : "停用" }}</td>
            <td class="actions">
              <button data-agent-action="edit_user_role" @click="openEdit(user)">分配角色</button>
            </td>
          </tr>
          <tr v-if="users.length === 0">
            <td colspan="6" class="empty">暂无用户</td>
          </tr>
        </tbody>
      </table>
    </div>

    <DialogPanel v-if="dialogOpen" title="用户角色分配" @close="dialogOpen = false">
      <form class="form-grid" data-agent-id="user-role-form" @submit.prevent="submit">
        <label>
          <span>显示名称</span>
          <input v-model.trim="form.display_name" data-agent-field="display_name" required />
        </label>
        <label>
          <span>部门</span>
          <select v-model="form.department_id" data-agent-field="department_id" @change="syncDepartmentName">
            <option v-for="department in departmentOptions" :key="department.id" :value="department.id">
              {{ department.name }}
            </option>
          </select>
        </label>
        <label>
          <span>角色</span>
          <select v-model="form.role_key" data-agent-field="role_key" @change="syncRoleName">
            <option v-for="role in roleOptions" :key="role.key" :value="role.key">{{ role.name }}</option>
          </select>
        </label>
        <label>
          <span>账号状态</span>
          <select v-model="form.enabled" data-agent-field="enabled">
            <option :value="true">启用</option>
            <option :value="false">停用</option>
          </select>
        </label>
        <div class="dialog-actions">
          <button type="button" @click="dialogOpen = false">取消</button>
          <button class="primary" data-agent-action="save_user_role" type="submit">保存</button>
        </div>
      </form>
    </DialogPanel>
  </div>
</template>

<script setup>
import { ref } from "vue";
import DialogPanel from "./DialogPanel.vue";

const props = defineProps({
  users: { type: Array, default: () => [] },
  saveUser: { type: Function, required: true }
});

const departmentOptions = [{ id: "operations", name: "运营部" }];
const roleOptions = [
  { key: "owner", name: "主责人" },
  { key: "collaborator", name: "协办人" },
  { key: "reviewer", name: "审核人" },
  { key: "member", name: "成员" }
];
const dialogOpen = ref(false);
const editingId = ref("");
const form = ref(emptyForm());

function openEdit(user) {
  editingId.value = user.id;
  form.value = {
    display_name: user.display_name || user.username,
    department_id: user.department_id || "operations",
    department_name: user.department_name || "运营部",
    role_key: user.role_key || "member",
    role_name: user.role_name || roleLabel(user.role_key || "member"),
    enabled: user.enabled !== false
  };
  syncDepartmentName();
  syncRoleName();
  dialogOpen.value = true;
}

async function submit() {
  await props.saveUser(editingId.value, form.value);
  dialogOpen.value = false;
}

function syncDepartmentName() {
  form.value.department_name = departmentOptions.find((item) => item.id === form.value.department_id)?.name || "运营部";
}

function syncRoleName() {
  form.value.role_name = roleLabel(form.value.role_key);
}

function roleLabel(roleKey) {
  return roleOptions.find((item) => item.key === roleKey)?.name || roleKey || "-";
}

function emptyForm() {
  return {
    display_name: "",
    department_id: "operations",
    department_name: "运营部",
    role_key: "member",
    role_name: "成员",
    enabled: true
  };
}
</script>
