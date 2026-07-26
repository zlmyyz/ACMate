<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import {
  getPlanDetail,
  joinPlan,
  togglePlanActive,
  deletePlan,
} from '@/api/training'
import type { PlanDetail } from '@/types/training'
import { trainingTypeLabels, trainingTimeStatusLabels, platformLabels } from '@/constants/labels'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const plan = ref<PlanDetail | null>(null)
const loading = ref(true)
const error = ref('')
const notFound = ref(false)
const joining = ref(false)
const joinError = ref('')

const planId = computed(() => Number(route.params.id))
const canEdit = computed(() => {
  if (!plan.value) return false
  return auth.user?.id === plan.value.creatorUserId
})

async function fetchDetail() {
  loading.value = true
  error.value = ''
  notFound.value = false
  try {
    plan.value = await getPlanDetail(planId.value)
  } catch (e: unknown) {
    const err = e as { response?: { status: number } }
    if (err.response?.status === 404) { notFound.value = true; return }
    if (err.response?.status === 403) { error.value = '无权查看该计划'; return }
    error.value = '加载计划详情失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function handleJoin() {
  joinError.value = ''
  joining.value = true
  try {
    await joinPlan(planId.value)
    await fetchDetail()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    joinError.value = err.response?.data?.message ?? '加入失败'
  } finally {
    joining.value = false
  }
}

async function handleToggleActive() {
  try {
    await togglePlanActive(planId.value)
    await fetchDetail()
  } catch { /* ignore */ }
}

async function handleDelete() {
  if (!confirm('确定要删除该计划吗？此操作不可撤销。')) return
  try {
    await deletePlan(planId.value)
    router.push({ name: 'training-plans' })
  } catch { /* ignore */ }
}

function formatDate(d: string | null): string {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('zh-CN')
}

onMounted(fetchDetail)
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="detail-header">
        <button class="back-link" @click="router.push({ name: 'training-plans' })">
          &larr; 返回计划列表
        </button>
      </div>
    </template>

    <LoadingState v-if="loading" />

    <ErrorState v-else-if="error" :message="error" @retry="fetchDetail" />

    <div v-else-if="notFound" class="status-page">
      <h2>计划不存在</h2>
      <p>该训练计划不存在或你无权查看。</p>
      <button class="status-btn" @click="router.push({ name: 'training-plans' })">返回计划列表</button>
    </div>

    <template v-else-if="plan">
      <div v-if="!plan.active" class="inactive-notice">
        该计划已停用。
      </div>

      <div class="plan-card">
        <div class="plan-top">
          <div>
            <h1 class="plan-title">{{ plan.title }}</h1>
            <div class="plan-meta">
              <span class="type-badge">{{ trainingTypeLabels[plan.planType] || plan.planType }}</span>
              <span class="time-status" :class="plan.timeStatus.toLowerCase()">
                {{ trainingTimeStatusLabels[plan.timeStatus] || plan.timeStatus }}
              </span>
            </div>
          </div>
          <div class="plan-actions">
            <button v-if="plan.planType === 'PUBLIC' && !plan.member && plan.active" class="join-btn" :disabled="joining" @click="handleJoin">
              {{ joining ? '加入中...' : '加入计划' }}
            </button>
            <button v-if="canEdit" class="edit-btn" @click="router.push({ name: 'edit-plan', params: { id: planId } })">编辑</button>
            <button v-if="canEdit" class="toggle-btn" @click="handleToggleActive">
              {{ plan.active ? '停用' : '恢复' }}
            </button>
            <button v-if="canEdit" class="delete-btn" @click="handleDelete">删除</button>
          </div>
        </div>

        <p v-if="joinError" class="join-error">{{ joinError }}</p>

        <div class="plan-info">
          <span>创建者：<RouterLink :to="`/users/${plan.creatorUserId}`">{{ plan.creatorNickname || plan.creatorUsername }}</RouterLink></span>
          <span>{{ plan.problemCount }} 题 · {{ plan.memberCount }} 人</span>
          <span v-if="plan.startTime || plan.endTime">
            {{ formatDate(plan.startTime) }} ~ {{ formatDate(plan.endTime) }}
          </span>
        </div>

        <div v-if="plan.description" class="plan-desc">{{ plan.description }}</div>
      </div>

      <div class="problems-card">
        <h2 class="section-title">题目列表（{{ plan.problems.length }}）</h2>
        <div v-if="plan.problems.length === 0" class="empty-hint">暂无题目</div>
        <div v-else class="problem-list">
          <div
            v-for="p in plan.problems"
            :key="p.id"
            class="problem-row"
            :class="{ inactive: !p.problemActive }"
          >
            <span class="problem-order">{{ p.sortOrder || '-' }}</span>
            <span class="problem-required">{{ p.required ? '必做' : '选做' }}</span>
            <RouterLink :to="`/problems/${p.problemId}`" class="problem-title">
              {{ p.problemTitle }}
              <span v-if="!p.problemActive" class="problem-inactive-hint">（已停用）</span>
            </RouterLink>
            <span class="problem-platform">{{ platformLabels[p.platform] || p.platform }}</span>
            <span v-if="p.difficulty" class="problem-diff">{{ p.difficulty }}</span>
          </div>
        </div>
      </div>
    </template>
  </PageContainer>
</template>

<style scoped>
.detail-header { display: flex; align-items: center; }

.back-link {
  background: none; border: none;
  color: var(--color-primary-container);
  font-size: var(--text-body-md); font-weight: 500; cursor: pointer; padding: 0;
}
.back-link:hover { text-decoration: underline; }

.inactive-notice {
  padding: 12px 20px; background: #fff3e0; border: 1px solid #ffcc02;
  border-radius: var(--radius-md); color: #e65100;
  font-size: var(--text-body-md); font-weight: 500;
  margin-bottom: var(--space-stack-md);
}

.plan-card, .problems-card {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 24px;
  margin-bottom: var(--space-stack-md);
}

.plan-top {
  display: flex; justify-content: space-between; align-items: flex-start;
  gap: 16px; margin-bottom: 16px; padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.plan-title {
  font-family: var(--font-headline);
  font-size: var(--text-headline-md); font-weight: 700;
  color: var(--color-on-surface); line-height: var(--leading-headline-md);
}

.plan-meta { display: flex; gap: 8px; margin-top: 8px; }

.type-badge {
  font-size: var(--text-label-sm); font-weight: 600;
  padding: 2px 10px; border-radius: 999px;
  color: var(--color-on-primary); background: var(--color-primary-container);
}

.time-status {
  font-size: var(--text-label-sm); font-weight: 600;
  padding: 2px 10px; border-radius: 999px;
}
.time-status.ongoing { color: var(--color-status-success); background: rgba(52,168,83,0.12); }
.time-status.not_started { color: var(--color-status-pending); background: rgba(243,161,60,0.12); }
.time-status.ended { color: var(--color-on-surface-variant); background: var(--color-surface-container); }

.plan-actions { display: flex; gap: 8px; flex-shrink: 0; }

.join-btn {
  height: 36px; padding: 0 20px; border: none; border-radius: var(--radius-md);
  background: var(--color-status-success); color: #fff;
  font-size: var(--text-body-md); font-weight: 600; cursor: pointer;
}
.join-btn:hover:not(:disabled) { opacity: 0.9; }
.join-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.edit-btn, .toggle-btn, .delete-btn {
  height: 36px; padding: 0 14px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); background: transparent;
  font-size: var(--text-body-sm); font-weight: 500; cursor: pointer;
}
.edit-btn:hover { border-color: var(--color-primary-container); color: var(--color-primary-container); }
.toggle-btn:hover { border-color: var(--color-status-pending); color: var(--color-status-pending); }
.delete-btn:hover { border-color: var(--color-status-critical); color: var(--color-status-critical); }

.join-error { color: var(--color-status-critical); font-size: var(--text-body-sm); margin-top: -8px; margin-bottom: 8px; }

.plan-info {
  display: flex; flex-wrap: wrap; gap: 16px;
  font-size: var(--text-body-sm); color: var(--color-on-surface-variant);
  margin-bottom: 16px;
}
.plan-info a { color: var(--color-primary-container); }

.plan-desc {
  font-size: var(--text-body-md); color: var(--color-on-surface);
  line-height: 1.6; white-space: pre-wrap;
}

.section-title {
  font-family: var(--font-headline); font-size: var(--text-headline-sm);
  font-weight: 600; color: var(--color-on-surface);
  margin-bottom: 16px; padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.empty-hint {
  text-align: center; padding: 24px;
  color: var(--color-on-surface-variant); font-size: var(--text-body-sm);
}

.problem-list { display: flex; flex-direction: column; gap: 4px; }

.problem-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; border-radius: var(--radius-sm);
  transition: background 0.15s;
}
.problem-row:hover { background: var(--color-surface-container-low); }
.problem-row.inactive { opacity: 0.5; }

.problem-order {
  width: 28px; text-align: center; font-family: var(--font-mono);
  font-size: var(--text-body-sm); color: var(--color-on-surface-variant);
}

.problem-required {
  font-size: var(--text-label-sm); font-weight: 600;
  padding: 1px 6px; border-radius: 999px;
  color: var(--color-primary-container); background: rgba(0,0,0,0.05);
}

.problem-title {
  flex: 1; font-size: var(--text-body-md); color: var(--color-on-surface); min-width: 0;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.problem-title:hover { color: var(--color-primary-container); }

.problem-inactive-hint { color: var(--color-status-critical); }

.problem-platform, .problem-diff {
  font-size: var(--text-body-sm); color: var(--color-on-surface-variant);
  font-family: var(--font-mono); white-space: nowrap;
}

.status-page {
  text-align: center; padding: 60px 24px;
}
.status-page h2 {
  font-family: var(--font-headline); font-size: var(--text-headline-md);
  font-weight: 700; color: var(--color-on-surface); margin-bottom: 8px;
}
.status-page p { color: var(--color-on-surface-variant); margin-bottom: 24px; }
.status-btn {
  padding: 10px 28px; background: var(--color-primary-container);
  color: var(--color-on-primary); border: none; border-radius: var(--radius-md);
  font-size: var(--text-body-md); font-weight: 600; cursor: pointer;
}
.status-btn:hover { opacity: 0.9; }
</style>
