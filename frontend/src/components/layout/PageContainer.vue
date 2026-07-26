<script setup lang="ts">
defineProps<{
  title?: string
  subtitle?: string
}>()
</script>

<template>
  <div class="page-container">
    <div v-if="title || $slots.header" class="page-header">
      <h1 v-if="title" class="page-title">{{ title }}</h1>
      <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
      <slot name="header" />
    </div>
    <div class="page-body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.page-container {
  max-width: var(--container-max);
  margin: 0 auto;
  padding: var(--space-stack-lg) var(--space-margin-page);
  width: 100%;
}

.page-header {
  margin-bottom: var(--space-stack-lg);
}

.page-title {
  font-family: var(--font-headline);
  font-size: var(--text-display-lg);
  font-weight: 700;
  line-height: var(--leading-display-lg);
  color: var(--color-on-surface);
}

.page-subtitle {
  margin-top: var(--space-stack-sm);
  font-size: var(--text-body-lg);
  color: var(--color-on-surface-variant);
}

.page-body {
  display: flex;
  flex-direction: column;
}
</style>
