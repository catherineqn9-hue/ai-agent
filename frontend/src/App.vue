<template>
  <div class="shell">
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
          :load-templates="loadImportTemplates"
          :load-batches="loadImportBatches"
          :load-batch-detail="loadImportBatchDetail"
          :load-detail="loadSupervisionDetail"
          :create-feedback="createSupervisionFeedback"
          :import-excel="handleExcelImport"
          @refresh="loadSupervisionItems"
          @save="saveSupervision"
          @delete="removeSupervision"
          @notify="showToast"
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

        <div v-else class="empty-page">请选择左侧菜单</div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import {
  checkHealth,
  createProgressFeedback,
  deleteBasicConfig,
  deleteSupervisionItem,
  getAdminMenus,
  getImportBatchDetail,
  getSupervisionItemDetail,
  importSupervisionExcel,
  listBasicConfigs,
  listImportBatches,
  listImportTemplates,
  listSupervisionItems,
  resumeChat,
  runChat,
  saveBasicConfig,
  saveSupervisionItem
} from "./api/client";
import AssistantPage from "./components/AssistantPage.vue";
import CrudPage from "./components/CrudPage.vue";
import SupervisionPage from "./components/SupervisionPage.vue";
import ToastHost from "./components/ToastHost.vue";

const menus = ref([]);
const currentMenuId = ref("assistant");
const healthOk = ref(null);
const healthText = ref("检查中");
const chatMessages = ref([]);
const chatSending = ref(false);
const threadId = ref("");
const configItems = ref([]);
const supervisionItems = ref([]);
const importTemplates = ref([]);
const toasts = ref([]);

const currentMenu = computed(() => menus.value.find((menu) => menu.id === currentMenuId.value) || menus.value[0]);

onMounted(async () => {
  await Promise.all([loadMenuConfig(), checkServiceHealth()]);
  if (currentMenu.value?.type === "supervision") {
    await loadSupervisionItems();
  }
});

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
  if (currentMenu.value?.type === "crud") {
    await loadConfigs();
  }
  if (currentMenu.value?.type === "supervision") {
    await loadSupervisionItems();
  }
}

async function sendChat(message) {
  const text = message.trim();
  if (!text || chatSending.value) return;
  chatMessages.value.push({ role: "user", content: text });
  chatSending.value = true;
  try {
    const payload = { user_id: "admin_user", message: text };
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
  return runSafely(() => getSupervisionItemDetail(id), {
    errorTitle: "事项详情加载失败",
    silentSuccess: true
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
    await saveSupervisionItem(id, payload);
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
  formData.append("created_by", "admin");
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
