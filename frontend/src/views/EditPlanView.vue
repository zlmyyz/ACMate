<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPlanDetail, updatePlan } from '@/api/training'
import type { PlanDetail } from '@/types/training'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'

const route = useRoute()
const router = useRouter()

const plan = ref<PlanDetail | null>(null)
const loading = ref(true)
const error = ref('')

const title = ref('')
const description = ref('')
const startTime = ref('')
const endTime = ref('')
const saving = ref(false)
const saveError = ref('')

const planId = computed(() => Number(route.params.id))

function toLocal(d: string | null): string {
  if (!d) return ''
  return d.substring(0, 16)
}

async function fetchPlan() {
  loading.value = true
  error.value = ''
  try {
    plan.value = await getPlanDetail(planId.value)
    title.value = plan.value.title
    description.value = plan.value.description ?? ''
    startTime.value = toLocal(plan.value.startTime)
    endTime.value = toLocal(plan.value.endTime)
  } catch (e: unknown) {
    const err = e as { response?: { status: number } }
    if (err.response?.status === 404) { error.value = '计划不存在'; return }
    if (err.response?.status === 403) { error.value = '无权编辑该计划'; return }
    error.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!title.value.trim()) return
  saveError.value = ''
  saving.value = true
  try {
    await updatePlan(planId.value, {
      title: title.value.trim(),
      description: description.value.trim() || undefined,
      startTime: startTime.value || undefined,
      endTime: endTime.value || undefined,
    })
    router.push({ name: 'plan-detail', params: { id: planId.value } })
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    saveError.value = err.response?.data?.message ?? '保存失败'
  } finally {
    saving.value = false
  }
}

onMounted(fetchPlan)
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="edit-header">
        <button class="back-link" @click="router.back()">&larr; 返回</button>
      </div>
    </template>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchPlan" />

    <div v-else class="form-card">
      <h1 class="form-title">编辑训练计划</h1>

      <div class="field">
        <label class="field-label">标题</label>
        <input v-model="title" class="field-input" maxlength="128" placeholder="计划标题" />
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
        <p v-if="saveError" class="form-error">{{ saveError }}</p>
        <button class="cancel-btn" @click="router.back()">取消</button>
        <button class="save-btn" :disabled="saving || !title.trim()" @click="handleSave">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.edit-header { display: flex; align-items: center; }
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

.field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 20px; }

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
