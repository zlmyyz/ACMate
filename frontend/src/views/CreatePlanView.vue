<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { createPlan } from '@/api/training'
import type { PlanType } from '@/types/training'
import PageContainer from '@/components/layout/PageContainer.vue'

const router = useRouter()
const auth = useAuthStore()

const title = ref('')
const description = ref('')
const planType = ref<PlanType>('PERSONAL')
const startTime = ref('')
const endTime = ref('')
const saving = ref(false)
const error = ref('')

const titleValid = () => title.value.trim().length > 0 && title.value.trim().length <= 128

async function handleCreate() {
  if (!titleValid()) return
  error.value = ''
  saving.value = true
  try {
    const result = await createPlan({
      title: title.value.trim(),
      description: description.value.trim() || undefined,
      planType: planType.value,
      startTime: startTime.value || undefined,
      endTime: endTime.value || undefined,
    })
    router.push({ name: 'plan-detail', params: { id: result.id } })
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    error.value = err.response?.data?.message ?? '创建失败，请稍后重试'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="create-header">
        <button class="back-link" @click="router.push({ name: 'training-plans' })">&larr; 返回</button>
      </div>
    </template>

    <div class="form-card">
      <h1 class="form-title">创建训练计划</h1>

      <div class="field">
        <label class="field-label">标题</label>
        <input v-model="title" class="field-input" maxlength="128" placeholder="计划标题" />
      </div>

      <div class="field">
        <label class="field-label">类型</label>
        <div class="type-options">
          <label class="type-option">
            <input type="radio" v-model="planType" value="PERSONAL" />
            <span>个人计划</span>
          </label>
          <label class="type-option">
            <input type="radio" v-model="planType" value="PUBLIC" :disabled="!auth.isAdmin" />
            <span>{{ auth.isAdmin ? '公开计划' : '公开计划（仅管理员）' }}</span>
          </label>
        </div>
      </div>

      <div class="field">
        <label class="field-label">说明</label>
        <textarea v-model="description" class="field-textarea" maxlength="5000" rows="4" placeholder="计划说明..." />
      </div>

      <div class="field-row">
        <div class="field">
          <label class="field-label">开始时间（可选）</label>
          <input v-model="startTime" type="datetime-local" class="field-input" />
        </div>
        <div class="field">
          <label class="field-label">结束时间（可选）</label>
          <input v-model="endTime" type="datetime-local" class="field-input" />
        </div>
      </div>

      <div class="form-actions">
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="cancel-btn" @click="router.push({ name: 'training-plans' })">取消</button>
        <button class="save-btn" :disabled="saving || !titleValid()" @click="handleCreate">
          {{ saving ? '创建中...' : '创建' }}
        </button>
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.create-header { display: flex; align-items: center; }
.back-link {
  background: none; border: none; color: var(--color-primary-container);
  font-size: var(--text-body-md); font-weight: 500; cursor: pointer; padding: 0;
}
.back-link:hover { text-decoration: underline; }

.form-card {
  background: var(--color-surface-card); border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg); box-shadow: var(--shadow-card);
  padding: 32px; max-width: 640px;
}

.form-title {
  font-family: var(--font-headline); font-size: var(--text-headline-md);
  font-weight: 700; color: var(--color-on-surface);
  margin-bottom: 28px; padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.field {
  display: flex; flex-direction: column; gap: 6px; margin-bottom: 20px;
}

.field-label {
  font-size: var(--text-label-sm); font-weight: 600;
  color: var(--color-on-surface); letter-spacing: 0.05em;
}

.field-input, .field-textarea {
  padding: 10px 14px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); font-size: var(--text-body-md);
  color: var(--color-on-surface); background: var(--color-surface-container-lowest);
  font-family: inherit;
}
.field-input:focus, .field-textarea:focus {
  outline: none; border-color: var(--color-primary-container);
}
.field-textarea { resize: vertical; min-height: 80px; }

.type-options { display: flex; gap: 24px; }
.type-option { display: flex; align-items: center; gap: 6px; font-size: var(--text-body-md); cursor: pointer; }
.type-option input[type="radio"]:disabled + span { color: var(--color-on-surface-variant); opacity: 0.5; }

.field-row { display: flex; gap: 16px; }
.field-row .field { flex: 1; }

.form-actions {
  display: flex; align-items: center; justify-content: flex-end; gap: 12px;
  margin-top: 8px; padding-top: 20px;
  border-top: 1px solid var(--color-border-subtle);
}
.form-error { margin-right: auto; font-size: var(--text-body-sm); color: var(--color-status-critical); }
.cancel-btn {
  height: 36px; padding: 0 18px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); background: transparent;
  color: var(--color-on-surface-variant); font-size: var(--text-body-md); font-weight: 500; cursor: pointer;
}
.save-btn {
  height: 36px; padding: 0 20px; border: none; border-radius: var(--radius-md);
  background: var(--color-primary-container); color: var(--color-on-primary);
  font-size: var(--text-body-md); font-weight: 600; cursor: pointer;
}
.save-btn:hover:not(:disabled) { opacity: 0.9; }
.save-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
