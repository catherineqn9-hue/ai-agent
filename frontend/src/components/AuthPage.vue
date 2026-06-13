<template>
  <main class="auth-shell">
    <section class="auth-panel">
      <div class="auth-heading">
        <strong>Sherry 督办后台</strong>
        <span>{{ mode === "login" ? "登录后进入工作台" : "创建本地账号" }}</span>
      </div>

      <form class="auth-form" @submit.prevent="submit">
        <label>
          <span>用户名</span>
          <input v-model.trim="form.username" autocomplete="username" required minlength="3" maxlength="80" />
        </label>
        <label v-if="mode === 'register'">
          <span>显示名称</span>
          <input v-model.trim="form.displayName" autocomplete="name" required maxlength="120" />
        </label>
        <label>
          <span>密码</span>
          <input v-model="form.password" type="password" autocomplete="current-password" required minlength="8" />
        </label>
        <button class="primary" type="submit" :disabled="loading">
          {{ loading ? "处理中..." : mode === "login" ? "登录" : "注册并登录" }}
        </button>
      </form>

      <button class="link-btn" type="button" @click="toggleMode">
        {{ mode === "login" ? "没有账号，去注册" : "已有账号，去登录" }}
      </button>
    </section>
  </main>
</template>

<script setup>
import { ref } from "vue";

const props = defineProps({
  loading: { type: Boolean, default: false }
});
const emit = defineEmits(["login", "register"]);

const mode = ref("login");
const form = ref(emptyForm());

function submit() {
  const payload = {
    username: form.value.username,
    password: form.value.password
  };
  if (mode.value === "register") {
    emit("register", { ...payload, display_name: form.value.displayName });
    return;
  }
  emit("login", payload);
}

function toggleMode() {
  mode.value = mode.value === "login" ? "register" : "login";
  form.value = emptyForm();
}

function emptyForm() {
  return {
    username: "",
    displayName: "",
    password: ""
  };
}
</script>
