<template>
  <div class="page assistant-page">
    <div class="chat-panel">
      <div ref="messagesEl" class="messages">
        <div v-if="messages.length === 0" class="welcome">
          <h2>有什么督办事项需要我帮你处理？</h2>
          <p>可以查询事项、创建事项、更新状态或添加进度反馈。</p>
          <div class="quick-prompts">
            <button v-for="prompt in prompts" :key="prompt" @click="submit(prompt)">{{ prompt }}</button>
          </div>
        </div>
        <div v-for="(message, index) in messages" :key="index" class="message" :class="message.role">
          <div class="bubble">{{ message.content }}</div>
        </div>
      </div>
      <form class="chat-input" @submit.prevent="submit(input)">
        <input v-model="input" :disabled="sending" placeholder="输入督办事项或进度反馈" />
        <button type="submit" :disabled="sending || !input.trim()">{{ sending ? "发送中" : "发送" }}</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from "vue";

const props = defineProps({
  messages: { type: Array, required: true },
  sending: { type: Boolean, default: false }
});
const emit = defineEmits(["send"]);
const input = ref("");
const messagesEl = ref(null);
const prompts = ["查询督办事项列表", "新增督办 整理本周经营周报", "查询进度反馈"];

function submit(text) {
  const value = text.trim();
  if (!value) return;
  emit("send", value);
  input.value = "";
}

watch(
  () => props.messages.length,
  async () => {
    await nextTick();
    if (messagesEl.value) {
      messagesEl.value.scrollTop = messagesEl.value.scrollHeight;
    }
  }
);
</script>
