<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getPlanDetail,
  joinPlan,
  deactivatePlan,
  restorePlan,
  removeMember,
  updateProblemStatus,
  updateProblemNote,
} from '@/api/training'
import type { PlanDetail, ProblemStatusType } from '@/types/training'
import { trainingTypeLabels, trainingTimeStatusLabels, platformLabels, progressLabels } from '@/constants/labels'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'

const route = useRoute()
const router = useRouter()

const plan = ref<PlanDetail | null>(null)
const loading = ref(true)
const error = ref('')
const notFound = ref(false)
const joining = ref(false)
const joinError = ref('')
const deactivating = ref(false)
const deactivateReason = ref('')
const showDeactivateDialog = ref(false)
const actionError = ref('')
const removingMemberId = ref<number | null>(null)
const updatingProblemId = ref<number | null>(null)
const editingNoteProblemId = ref<number | null>(null)
const editingNoteText = ref('')
const settingStatusProblemId = ref<number | null>(null)

const planId = computed(() => Number(route.params.id))

async function fetchDetail() {
  loading.value = true
  error.value = ''
  notFound.value = false
  try {
    plan.value = await getPlanDetail(planId.value)
  } catch (e: unknown) {
    const err = e as { response?: { status: number; data?: { message?: string } } }
    if (err.response?.status === 404) { notFound.value = true; return }
    if (err.response?.status === 401) {
      router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      return
    }
    error.value = err.response?.data?.message || '加载计划详情失败，请稍后重试'
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

async function handleDeactivate() {
  actionError.value = ''
  deactivating.value = true
  try {
    await deactivatePlan(planId.value, deactivateReason.value || undefined)
    showDeactivateDialog.value = false
    deactivateReason.value = ''
    await fetchDetail()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    actionError.value = err.response?.data?.message ?? '操作失败'
  } finally {
    deactivating.value = false
  }
}

async function handleRestore() {
  actionError.value = ''
  try {
    await restorePlan(planId.value)
    await fetchDetail()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    actionError.value = err.response?.data?.message ?? '操作失败'
  }
}

async function handleRemoveMember(userId: number) {
  if (!confirm('确定要移除该成员吗？')) return
  removingMemberId.value = userId
  try {
    await removeMember(planId.value, userId)
    await fetchDetail()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    alert(err.response?.data?.message ?? '移除失败')
  } finally {
    removingMemberId.value = null
  }
}

async function handleStatusChange(problemId: number, status: ProblemStatusType) {
  settingStatusProblemId.value = problemId
  try {
    await updateProblemStatus(planId.value, problemId, { status })
    await fetchDetail()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    alert(err.response?.data?.message ?? '更新状态失败')
  } finally {
    settingStatusProblemId.value = null
  }
}

function startEditNote(problemId: number, currentNote: string | null) {
  editingNoteProblemId.value = problemId
  editingNoteText.value = currentNote || ''
}

async function saveNote(problemId: number) {
  updatingProblemId.value = problemId
  try {
    await updateProblemNote(planId.value, problemId, { note: editingNoteText.value || null })
    editingNoteProblemId.value = null
    editingNoteText.value = ''
    await fetchDetail()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    alert(err.response?.data?.message ?? '保存备注失败')
  } finally {
    updatingProblemId.value = null
  }
}

function cancelEditNote() {
  editingNoteProblemId.value = null
  editingNoteText.value = ''
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
        该计划已停用
        <span v-if="plan.deactivationReason">：{{ plan.deactivationReason }}</span>
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
            <button v-if="plan.canJoin" class="join-btn" :disabled="joining" @click="handleJoin">
              {{ joining ? '加入中...' : '加入计划' }}
            </button>
            <span v-else-if="plan.joined && !plan.creator" class="joined-label">已加入</span>

            <button v-if="plan.canEdit" class="edit-btn" @click="router.push({ name: 'edit-plan', params: { id: planId } })">编辑</button>

            <template v-if="plan.canDeactivate && plan.creator">
              <button class="deactivate-btn" @click="showDeactivateDialog = true">停用</button>
            </template>
            <template v-if="plan.canDeactivate && !plan.creator">
              <button class="deactivate-btn" @click="showDeactivateDialog = true">强制停用</button>
            </template>

            <button v-if="plan.canRestore" class="restore-btn" @click="handleRestore">恢复</button>
          </div>
        </div>

        <div v-if="showDeactivateDialog" class="deactivate-dialog">
          <template v-if="plan.creator">
            <p>确定要停用该计划吗？停用后其他用户将无法加入。</p>
          </template>
          <template v-else>
            <p>管理员强制停用，请输入原因：</p>
            <input v-model="deactivateReason" class="deactivate-reason" placeholder="停用原因（必填）" maxlength="500" />
          </template>
          <div class="dialog-actions">
            <p v-if="actionError" class="form-error">{{ actionError }}</p>
            <button class="cancel-btn" @click="showDeactivateDialog = false; deactivateReason = ''; actionError = ''">取消</button>
            <button class="confirm-deactivate-btn" :disabled="deactivating || (!plan.creator && !deactivateReason.trim())" @click="handleDeactivate">
              {{ deactivating ? '处理中...' : '确认停用' }}
            </button>
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

      <div v-if="plan.joined && plan.myProgress" class="progress-card">
        <h2 class="section-title">我的进度</h2>
        <div class="progress-stats">
          <div class="stat-item">
            <span class="stat-label">必做</span>
            <span class="stat-value">{{ plan.myProgress.requiredCompletedCount }}/{{ plan.myProgress.requiredTotal }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">选做</span>
            <span class="stat-value">{{ plan.myProgress.optionalCompletedCount }}/{{ plan.myProgress.optionalTotal }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">总计</span>
            <span class="stat-value">{{ plan.myProgress.requiredCompletedCount + plan.myProgress.optionalCompletedCount }}/{{ plan.myProgress.requiredTotal + plan.myProgress.optionalTotal }}</span>
          </div>
        </div>
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
            <span v-if="plan.joined && p.myStatus" class="status-badge" :class="p.myStatus.toLowerCase()">
              {{ progressLabels[p.myStatus] || p.myStatus }}
            </span>
            <div v-if="plan.joined && p.myStatus !== 'ACCEPTED'" class="status-actions">
              <button
                v-if="(!p.myStatus || p.myStatus === 'NOT_STARTED') && settingStatusProblemId !== p.problemId"
                class="status-btn challenging-btn"
                @click="handleStatusChange(p.problemId, 'CHALLENGING')"
              >挑战中</button>
              <button
                v-if="p.myStatus === 'CHALLENGING' && settingStatusProblemId !== p.problemId"
                class="status-btn accepted-btn"
                @click="handleStatusChange(p.problemId, 'ACCEPTED')"
              >已通过</button>
              <span v-if="settingStatusProblemId === p.problemId" class="status-updating">更新中...</span>
            </div>
            <div v-if="plan.joined" class="note-area">
              <span v-if="editingNoteProblemId !== p.problemId" class="note-text" @click="startEditNote(p.problemId, p.performanceNote || null)">
                {{ p.performanceNote || '添加备注' }}
              </span>
              <div v-else class="note-edit">
                <input v-model="editingNoteText" class="note-input" maxlength="500" placeholder="填写备注" @keyup.enter="saveNote(p.problemId)" />
                <button class="note-save-btn" :disabled="updatingProblemId === p.problemId" @click="saveNote(p.problemId)">{{ updatingProblemId === p.problemId ? '保存中' : '保存' }}</button>
                <button class="note-cancel-btn" @click="cancelEditNote()">取消</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="plan.members && plan.members.length" class="members-card">
        <div class="members-header">
          <h2 class="section-title">成员列表（{{ plan.members.length }}）</h2>
        </div>
        <div class="member-list">
          <div v-for="m in plan.members" :key="m.userId" class="member-row">
            <RouterLink :to="`/users/${m.userId}`" class="member-name">
              {{ m.nickname || m.username }}
            </RouterLink>
            <span v-if="m.creator" class="creator-badge">创建者</span>
            <span class="member-join-time">{{ formatDate(m.joinTime) }}</span>
            <button
              v-if="plan.canRemoveMembers && !m.creator"
              class="remove-member-btn"
              :disabled="removingMemberId === m.userId"
              @click.stop="handleRemoveMember(m.userId)"
            >
              {{ removingMemberId === m.userId ? '移除中...' : '移除' }}
            </button>
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

.plan-card, .problems-card, .members-card {
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

.plan-actions { display: flex; gap: 8px; flex-shrink: 0; flex-wrap: wrap; align-items: center; }

.join-btn {
  height: 36px; padding: 0 20px; border: none; border-radius: var(--radius-md);
  background: var(--color-status-success); color: #fff;
  font-size: var(--text-body-md); font-weight: 600; cursor: pointer;
}
.join-btn:hover:not(:disabled) { opacity: 0.9; }
.join-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.joined-label {
  font-size: var(--text-body-sm); color: var(--color-status-success); font-weight: 600;
}

.edit-btn, .deactivate-btn, .restore-btn {
  height: 36px; padding: 0 14px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); background: transparent;
  font-size: var(--text-body-sm); font-weight: 500; cursor: pointer;
}
.edit-btn:hover { border-color: var(--color-primary-container); color: var(--color-primary-container); }
.deactivate-btn:hover { border-color: var(--color-status-pending); color: var(--color-status-pending); }
.restore-btn:hover { border-color: var(--color-status-success); color: var(--color-status-success); }

.deactivate-dialog {
  margin-top: -8px; margin-bottom: 16px; padding: 16px;
  background: var(--color-surface-container-lowest);
  border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md);
}
.deactivate-dialog p { font-size: var(--text-body-md); margin-bottom: 8px; }
.deactivate-reason {
  width: 100%; padding: 8px 12px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); font-size: var(--text-body-md);
  margin-bottom: 8px;
}
.deactivate-reason:focus { outline: none; border-color: var(--color-primary-container); }
.dialog-actions { display: flex; gap: 8px; align-items: center; justify-content: flex-end; }
.form-error { color: var(--color-status-critical); font-size: var(--text-body-sm); margin-right: auto; }
.cancel-btn {
  height: 32px; padding: 0 14px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); background: transparent;
  font-size: var(--text-body-sm); cursor: pointer;
}
.confirm-deactivate-btn {
  height: 32px; padding: 0 16px; border: none; border-radius: var(--radius-md);
  background: var(--color-status-critical); color: #fff;
  font-size: var(--text-body-sm); font-weight: 600; cursor: pointer;
}
.confirm-deactivate-btn:hover:not(:disabled) { opacity: 0.9; }
.confirm-deactivate-btn:disabled { opacity: 0.4; cursor: not-allowed; }

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
  margin-bottom: 0;
}

.members-header {
  display: flex; align-items: center; justify-content: space-between;
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
.problem-row { flex-wrap: wrap; }

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

.status-badge {
  font-size: var(--text-label-sm); font-weight: 600;
  padding: 1px 8px; border-radius: 999px;
  white-space: nowrap;
}
.status-badge.not_started { color: var(--color-on-surface-variant); background: var(--color-surface-container); }
.status-badge.challenging { color: var(--color-status-pending); background: rgba(243,161,60,0.12); }
.status-badge.accepted { color: var(--color-status-success); background: rgba(52,168,83,0.12); }

.status-actions { display: flex; gap: 4px; }
.status-btn {
  height: 26px; padding: 0 10px; border: none; border-radius: var(--radius-sm);
  font-size: var(--text-label-sm); font-weight: 600; cursor: pointer;
}
.status-btn:hover { opacity: 0.9; }
.challenging-btn { background: rgba(243,161,60,0.15); color: var(--color-status-pending); }
.accepted-btn { background: rgba(52,168,83,0.15); color: var(--color-status-success); }
.status-updating { font-size: var(--text-label-sm); color: var(--color-on-surface-variant); }

.note-area { width: 100%; margin-top: 4px; }
.note-text {
  font-size: var(--text-label-sm); color: var(--color-on-surface-variant);
  cursor: pointer; display: inline-block; padding: 2px 6px;
  border-radius: var(--radius-sm); border: 1px dashed transparent;
}
.note-text:hover { border-color: var(--color-border-subtle); background: var(--color-surface-container-low); }
.note-edit { display: flex; gap: 4px; align-items: center; }
.note-input {
  flex: 1; padding: 4px 8px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm); font-size: var(--text-label-sm);
}
.note-input:focus { outline: none; border-color: var(--color-primary-container); }
.note-save-btn, .note-cancel-btn {
  height: 24px; padding: 0 8px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm); background: transparent;
  font-size: var(--text-label-sm); cursor: pointer;
}
.note-save-btn { border-color: var(--color-primary-container); color: var(--color-primary-container); }
.note-save-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.note-cancel-btn:hover { background: var(--color-surface-container); }

.progress-card {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 24px;
  margin-bottom: var(--space-stack-md);
}
.progress-stats { display: flex; gap: 24px; margin-top: 12px; }
.stat-item { display: flex; flex-direction: column; gap: 4px; }
.stat-label { font-size: var(--text-label-sm); color: var(--color-on-surface-variant); }
.stat-value { font-size: var(--text-headline-sm); font-weight: 700; color: var(--color-on-surface); }

.member-list { display: flex; flex-direction: column; gap: 4px; }

.member-row {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; border-radius: var(--radius-sm);
  transition: background 0.15s;
}
.member-row:hover { background: var(--color-surface-container-low); }

.member-name { font-size: var(--text-body-md); color: var(--color-on-surface); font-weight: 500; }
.member-name:hover { color: var(--color-primary-container); }

.creator-badge {
  font-size: var(--text-label-sm); font-weight: 600;
  padding: 1px 8px; border-radius: 999px;
  color: var(--color-on-primary); background: var(--color-primary-container);
}

.member-join-time {
  margin-left: auto;
  font-size: var(--text-body-sm); color: var(--color-on-surface-variant);
}

.remove-member-btn {
  height: 28px; padding: 0 10px; border: 1px solid var(--color-status-critical);
  border-radius: var(--radius-sm); background: transparent;
  color: var(--color-status-critical);
  font-size: var(--text-label-sm); cursor: pointer;
}
.remove-member-btn:hover:not(:disabled) { background: rgba(217,45,32,0.08); }
.remove-member-btn:disabled { opacity: 0.4; cursor: not-allowed; }

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
