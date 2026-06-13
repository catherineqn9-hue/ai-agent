<template>
  <div class="dialog-mask" @click.self="$emit('close')">
    <section class="dialog">
      <header class="dialog-header">
        <h2>{{ title }}</h2>
        <button class="icon-btn" type="button" @click="$emit('close')">×</button>
      </header>
      <slot />
    </section>
  </div>
</template>

<script setup>
defineProps({ title: { type: String, required: true } });
defineEmits(["close"]);
</script>
