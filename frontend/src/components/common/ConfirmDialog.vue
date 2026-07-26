<script setup lang="ts">
import { actionLabels } from '@/constants/labels'

defineProps<{
  visible: boolean
  title: string
  message: string
  confirmText?: string
  loading?: boolean
}>()

const emit = defineEmits<{
  confirm: []
  cancel: []
}>()
</script>

<template>
  <div v-if="visible" class="overlay" @click.self="emit('cancel')">
    <div class="dialog">
      <h3 class="dialog-title">{{ title }}</h3>
      <p class="dialog-msg">{{ message }}</p>
      <div class="dialog-actions">
        <button class="dialog-btn cancel-btn" :disabled="loading" @click="emit('cancel')">
          {{ actionLabels.cancel }}
        </button>
        <button class="dialog-btn confirm-btn" :disabled="loading" @click="emit('confirm')">
          {{ loading ? '处理中...' : (confirmText || actionLabels.confirm) }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
}

.dialog {
  background: var(--color-surface-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  min-width: 360px;
  max-width: 440px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
}

.dialog-title {
  font-family: var(--font-headline);
  font-size: var(--text-headline-sm);
  font-weight: 600;
  color: var(--color-on-surface);
  margin-bottom: 12px;
}

.dialog-msg {
  font-size: var(--text-body-md);
  color: var(--color-on-surface-variant);
  line-height: 1.6;
  margin-bottom: 24px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-btn {
  padding: 8px 22px;
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
  border: none;
}

.dialog-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-btn {
  background: var(--color-surface-container);
  color: var(--color-on-surface-variant);
  border: 1px solid var(--color-border-subtle);
}

.confirm-btn {
  background: var(--color-primary-container);
  color: var(--color-on-primary);
}
</style>
