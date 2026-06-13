<template>
  <AuthPage
    v-if="!currentUser"
    :loading="authLoading"
    @login="handleLogin"
    @register="handleRegister"
  />

  <div v-else class="shell">
    <ToastHost :toasts="toasts" />

    <aside class="sidebar">
      <div class="brand">
        <strong>Sherry 督办后台</strong>
        <span>AI + Spring Boot + Vue</span>
      </div>

      <nav>
        <template v-for="menu in menus" :key="menu.id">
          <div v-if="menu.groupLabel" class="nav-group">{{ menu.groupLabel }}</div>
          <button class="nav-btn" :class="{ active: menu.id === currentMenuId }" @click="switchMenu(menu.id)">
            <span class="nav-icon">{{ menu.icon || menu.title?.slice(0, 1) }}</span>
            <span>{{ menu.title }}</span>
          </button>
        </template>
      </nav>

      <div class="aside-foot">当前主栈：Spring Boot + Vue</div>
    </aside>

    <main>
      <header>
        <div>
          <h1>{{ currentMenu?.title || "AI 助手问答" }}</h1>
          <div class="header-subtitle">{{ currentMenu?.hint || "通过接口完成督办业务操作" }}</div>
        </div>
        <div class="health">
          <span class="dot" :class="{ ok: healthOk, err: healthOk === false }"></span>
          <span>{{ healthText }}</span>
          <span v-if="currentUser" class="user-chip">{{ currentUser.display_name || currentUser.username }}</span>
          <button v-if="currentUser" class="logout-btn" @click="handleLogout">退出</button>
        </div>
      </header>

      <section class="content">
        <AssistantPage
          v-if="currentMenu?.type === 'assistant'"
          :messages="chatMessages"
          :sending="chatSending"
          @send="sendChat"
        />

        <SupervisionPage
          v-else-if="currentMenu?.type === 'supervision'"
          :items="supervisionItems"
          :templates="importTemplates"
          :users="managedUsers"
          :load-templates="loadImportTemplates"
          :load-batches="loadImportBatches"
          :load-batch-detail="loadImportBatchDetail"
          :load-detail="loadSupervisionDetail"
          :load-users="loadUsers"
          :load-assignment-recommendations="loadAssignmentRecommendations"
          :assign-item="assignItem"
          :create-feedback="createSupervisionFeedback"
          :import-excel="handleExcelImport"
          :current-user="currentUser"
          @refresh="loadSupervisionItems"
          @save="saveSupervision"
          @delete="removeSupervision"
          @notify="showToast"
        />

        <MySupervisionPage
          v-else-if="currentMenu?.type === 'my-supervision'"
          :items="mySupervisionItems"
          :current-user="currentUser"
          :load-detail="loadMySupervisionDetail"
          :confirm-receive-action="confirmMySupervisionReceive"
          :reject-assignment-action="rejectMySupervisionAssignment"
          :create-feedback="createSupervisionFeedback"
          :refresh-items="loadMySupervisionItems"
        />

        <CrudPage
          v-else-if="currentMenu?.type === 'crud'"
          :menu="currentMenu"
          :items="configItems"
          @refresh="loadConfigs"
          @save="saveConfig"
          @delete="removeConfig"
          @notify="showToast"
        />

        <UserManagementPage
          v-else-if="currentMenu?.type === 'user-management'"
          :users="managedUsers"
          :save-user="saveManagedUser"
        />

        <div v-else class="empty-page">请选择左侧菜单</div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import {
  checkHealth,
  confirmSupervisionReceive,
  createProgressFeedback,
  deleteBasicConfig,
  deleteSupervisionItem,
  getAuthToken,
  getAdminMenus,
  getCurrentUser,
  getImportBatchDetail,
  getMySupervisionItemDetail,
  getSupervisionItemDetail,
  importSupervisionExcel,
  assignSupervisionItem,
  listAssignmentRecommendations,
  loginUser,
  listBasicConfigs,
  listImportBatches,
  listImportTemplates,
  listMySupervisionItems,
  listSupervisionItems,
  listUsers,
  logoutUser,
  registerUser,
  rejectSupervisionAssignment,
  resumeChat,
  runChat,
  saveBasicConfig,
  saveSupervisionItem,
  setAuthToken,
  updateUser
} from "./api/client";
import AuthPage from "./components/AuthPage.vue";
import AssistantPage from "./components/AssistantPage.vue";
import CrudPage from "./components/CrudPage.vue";
import MySupervisionPage from "./components/MySupervisionPage.vue";
import SupervisionPage from "./components/SupervisionPage.vue";
import ToastHost from "./components/ToastHost.vue";
import UserManagementPage from "./components/UserManagementPage.vue";

const menus = ref([]);
const currentMenuId = ref("assistant");
const healthOk = ref(null);
const healthText = ref("检查中");
const chatMessages = ref([]);
const chatSending = ref(false);
const threadId = ref("");
const configItems = ref([]);
const supervisionItems = ref([]);
const mySupervisionItems = ref([]);
const managedUsers = ref([]);
const importTemplates = ref([]);
const toasts = ref([]);
const authLoading = ref(false);
const currentUser = ref(null);

const currentMenu = computed(() => menus.value.find((menu) => menu.id === currentMenuId.value) || menus.value[0]);

onMounted(async () => {
  await restoreSession();
  if (!currentUser.value) {
    return;
  }
  await Promise.all([loadMenuConfig(), checkServiceHealth()]);
  if (currentMenu.value?.type === "supervision") {
    await loadSupervisionItems();
  }
  if (currentMenu.value?.type === "my-supervision") {
    await loadMySupervisionItems();
  }
  if (currentMenu.value?.type === "user-management") {
    await loadUsers();
  }
});

async function restoreSession() {
  if (!getAuthToken()) {
    return;
  }
  authLoading.value = true;
  try {
    currentUser.value = await getCurrentUser();
  } catch (error) {
    setAuthToken("");
    currentUser.value = null;
  } finally {
    authLoading.value = false;
  }
}

async function handleLogin(payload) {
  await authenticate(() => loginUser(payload), "登录失败");
}

async function handleRegister(payload) {
  await authenticate(() => registerUser(payload), "注册失败");
}

async function authenticate(action, errorTitle) {
  authLoading.value = true;
  try {
    const session = await action();
    setAuthToken(session.access_token);
    currentUser.value = session.user;
    await Promise.all([loadMenuConfig(), checkServiceHealth()]);
    if (currentMenu.value?.type === "supervision") {
      await loadSupervisionItems();
    }
    if (currentMenu.value?.type === "my-supervision") {
      await loadMySupervisionItems();
    }
    if (currentMenu.value?.type === "user-management") {
      await loadUsers();
    }
  } catch (error) {
    showToast({ type: "error", title: errorTitle, message: error.message });
  } finally {
    authLoading.value = false;
  }
}

async function handleLogout() {
  await runSafely(async () => {
    await logoutUser();
    setAuthToken("");
    currentUser.value = null;
    menus.value = [];
    chatMessages.value = [];
    threadId.value = "";
    managedUsers.value = [];
  }, { successTitle: "已退出", successMessage: "当前账号已退出", errorTitle: "退出失败" });
}

async function loadMenuConfig(preferredMenuId) {
  const data = await runSafely(() => getAdminMenus(), {
    errorTitle: "菜单加载失败",
    silentSuccess: true
  });
  if (!data) return;

  let lastGroup = "";
  menus.value = (data.menus || []).map((menu) => {
    const groupName = menu.groupName || (menu.type === "assistant" ? "智能助手" : "基础配置");
    const groupLabel = groupName !== lastGroup ? groupName : "";
    lastGroup = groupName;
    return { ...menu, groupLabel };
  });
  const nextId = preferredMenuId || currentMenuId.value;
  currentMenuId.value = menus.value.some((menu) => menu.id === nextId) ? nextId : menus.value[0]?.id;
}

async function checkServiceHealth() {
  try {
    await checkHealth();
    healthOk.value = true;
    healthText.value = "服务正常";
  } catch (error) {
    healthOk.value = false;
    healthText.value = "服务未连接";
  }
}

async function switchMenu(id) {
  currentMenuId.value = id;
  configItems.value = [];
  supervisionItems.value = [];
  mySupervisionItems.value = [];
  if (currentMenu.value?.type === "crud") {
    await loadConfigs();
  }
  if (currentMenu.value?.type === "supervision") {
    await loadSupervisionItems();
  }
  if (currentMenu.value?.type === "my-supervision") {
    await loadMySupervisionItems();
  }
  if (currentMenu.value?.type === "user-management") {
    await loadUsers();
  }
}

async function sendChat(message) {
  const text = message.trim();
  if (!text || chatSending.value) return;
  chatMessages.value.push({ role: "user", content: text });
  chatSending.value = true;
  try {
    const payload = { user_id: currentUser.value?.username || "anonymous", message: text };
    if (threadId.value) payload.thread_id = threadId.value;
    const data = threadId.value ? await resumeChat(payload) : await runChat(payload);
    threadId.value = data.thread_id;
    chatMessages.value.push({ role: "assistant", content: data.answer });
  } catch (error) {
    chatMessages.value.push({ role: "assistant", content: `调用失败：${error.message}` });
    showToast({ type: "error", title: "AI 调用失败", message: error.message });
  } finally {
    chatSending.value = false;
  }
}

async function loadConfigs() {
  if (!currentMenu.value?.resource) return;
  const data = await runSafely(() => listBasicConfigs(currentMenu.value.resource), {
    errorTitle: "配置加载失败",
    silentSuccess: true
  });
  if (data) {
    configItems.value = data.items || [];
  }
}

async function saveConfig({ id, payload }) {
  await runSafely(async () => {
    await saveBasicConfig(currentMenu.value.resource, id, payload);
    await loadConfigs();
    if (currentMenu.value.resource === "admin-menus") {
      await loadMenuConfig("menu");
    }
  }, { successTitle: "保存成功", successMessage: "配置已更新", errorTitle: "保存失败" });
}

async function removeConfig(id) {
  await runSafely(async () => {
    await deleteBasicConfig(currentMenu.value.resource, id);
    await loadConfigs();
    if (currentMenu.value.resource === "admin-menus") {
      await loadMenuConfig("menu");
    }
  }, { successTitle: "删除成功", successMessage: "配置已删除", errorTitle: "删除失败" });
}

async function loadSupervisionItems() {
  const data = await runSafely(() => listSupervisionItems(), {
    errorTitle: "事项加载失败",
    silentSuccess: true
  });
  if (data) {
    supervisionItems.value = data.items || [];
  }
}

async function loadSupervisionDetail(id) {
  const detail = await runSafely(() => getSupervisionItemDetail(id), {
    errorTitle: "事项详情加载失败",
    silentSuccess: true
  });
  await loadUsers();
  return detail;
}

async function loadUsers() {
  const data = await runSafely(() => listUsers(), {
    errorTitle: "用户加载失败",
    silentSuccess: true
  });
  if (data) {
    managedUsers.value = data.users || [];
  }
}

async function saveManagedUser(id, payload) {
  await runSafely(async () => {
    await updateUser(id, payload);
    await loadUsers();
  }, { successTitle: "保存成功", successMessage: "用户角色已更新", errorTitle: "用户保存失败" });
}

async function loadAssignmentRecommendations(itemId, params) {
  return runSafely(() => listAssignmentRecommendations(itemId, params), {
    errorTitle: "分配建议加载失败",
    silentSuccess: true
  });
}

async function assignItem(itemId, payload) {
  return runSafely(async () => {
    await assignSupervisionItem(itemId, payload);
    await loadSupervisionItems();
  }, { successTitle: "分配成功", successMessage: "已生成待确认的督办分派", errorTitle: "分配失败" });
}

async function loadMySupervisionItems() {
  const data = await runSafely(() => listMySupervisionItems(), {
    errorTitle: "我的督办加载失败",
    silentSuccess: true
  });
  if (data) {
    mySupervisionItems.value = data.items || [];
  }
}

async function loadMySupervisionDetail(id) {
  return runSafely(() => getMySupervisionItemDetail(id), {
    errorTitle: "我的任务详情加载失败",
    silentSuccess: true
  });
}

async function confirmMySupervisionReceive(id) {
  return runSafely(() => confirmSupervisionReceive(id), {
    successTitle: "已确认接收",
    successMessage: "任务已进入处理中",
    errorTitle: "确认接收失败"
  });
}

async function rejectMySupervisionAssignment(id, payload) {
  return runSafely(() => rejectSupervisionAssignment(id, payload), {
    successTitle: "已拒绝接收",
    successMessage: "拒绝原因已记录",
    errorTitle: "拒绝接收失败"
  });
}

async function createSupervisionFeedback(payload) {
  return runSafely(() => createProgressFeedback(payload), {
    successTitle: "反馈已记录",
    successMessage: "进度反馈已添加",
    errorTitle: "反馈保存失败"
  });
}

async function saveSupervision({ id, payload }) {
  await runSafely(async () => {
    await saveSupervisionItem(id, {
      ...payload,
      created_by: payload.created_by || currentUser.value?.username || "admin"
    });
    await loadSupervisionItems();
  }, { successTitle: "保存成功", successMessage: "督办事项已更新", errorTitle: "保存失败" });
}

async function removeSupervision(id) {
  await runSafely(async () => {
    await deleteSupervisionItem(id);
    await loadSupervisionItems();
  }, { successTitle: "删除成功", successMessage: "督办事项已删除", errorTitle: "删除失败" });
}

async function loadImportTemplates() {
  const data = await runSafely(() => listImportTemplates(), {
    errorTitle: "模板加载失败",
    silentSuccess: true
  });
  if (data) {
    importTemplates.value = data.templates || [];
  }
}

async function loadImportBatches() {
  return runSafely(() => listImportBatches(), {
    errorTitle: "批次加载失败",
    silentSuccess: true
  });
}

async function loadImportBatchDetail(batchId) {
  return runSafely(() => getImportBatchDetail(batchId), {
    errorTitle: "批次详情加载失败",
    silentSuccess: true
  });
}

async function handleExcelImport({ templateCode, file }) {
  const formData = new FormData();
  formData.append("file", file);
  formData.append("created_by", currentUser.value?.username || "admin");
  formData.append("template_code", templateCode);
  return runSafely(async () => {
    const result = await importSupervisionExcel(formData);
    await loadSupervisionItems();
    return result;
  }, { errorTitle: "导入失败", silentSuccess: true });
}

async function runSafely(action, options = {}) {
  try {
    const result = await action();
    if (!options.silentSuccess) {
      showToast({
        type: "success",
        title: options.successTitle || "操作成功",
        message: options.successMessage || "操作已完成"
      });
    }
    return result;
  } catch (error) {
    showToast({
      type: "error",
      title: options.errorTitle || "操作失败",
      message: error.message
    });
    return null;
  }
}

function showToast({ type = "info", title = "提示", message = "" }) {
  const id = `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  toasts.value.push({ id, type, title, message });
  window.setTimeout(() => {
    toasts.value = toasts.value.filter((toast) => toast.id !== id);
  }, type === "error" ? 5200 : 3200);
}
</script>
