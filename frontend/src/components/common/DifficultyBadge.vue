<script setup lang="ts">
defineProps<{
  difficulty: string
}>()
</script>

<template>
  <span class="difficulty-badge">{{ difficulty }}</span>
</template>

<style scoped>
.difficulty-badge {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: var(--text-code-sm);
  font-weight: 600;
  color: var(--color-tertiary);
  white-space: nowrap;
}
</style>
