<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAuditLogs } from '@/api/audit-logs'
import type { AuditLogResponse } from '@/types/audit-log'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const route = useRoute()
const router = useRouter()

const items = ref<AuditLogResponse[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
const page = ref(parseInt(route.query.page as string) || 1)
const size = 20
const actorKeyword = ref((route.query.actorKeyword as string) || '')
const actionTypeFilter = ref((route.query.actionType as string) || '')
const targetTypeFilter = ref((route.query.targetType as string) || '')
const startTime = ref((route.query.startTime as string) || '')
const endTime = ref((route.query.endTime as string) || '')

let requestId = 0

const actionLabels: Record<string, string> = {
  USER_DEACTIVATED: '停用用户',
  USER_RESTORED: '恢复用户',
  ADMIN_GRANTED: '授予管理员',
  ADMIN_REVOKED: '取消管理员',
  POST_ADMIN_DEACTIVATED: '停用帖子',
  POST_RESTORED: '恢复帖子',
  COMMENT_ADMIN_DEACTIVATED: '停用评论',
  COMMENT_RESTORED: '恢复评论',
  TRAINING_ADMIN_DEACTIVATED: '停用计划',
  TRAINING_RESTORED: '恢复计划',
  PROBLEM_ADMIN_DEACTIVATED: '停用题目',
  PROBLEM_RESTORED: '恢复题目',
  OJ_ACCOUNT_VERIFIED: '验证通过',
  OJ_ACCOUNT_REJECTED: '验证拒绝',
}

const targetLabels: Record<string, string> = {
  USER: '用户',
  POST: '帖子',
  COMMENT: '评论',
  TRAINING_PLAN: '训练计划',
  PROBLEM: '题目',
  OJ_ACCOUNT: 'OJ账号',
}

const actionOptions = [
  { value: '', label: '全部操作' },
  ...Object.entries(actionLabels).map(([value, label]) => ({ value, label })),
]

const targetOptions = [
  { value: '', label: '全部类型' },
  ...Object.entries(targetLabels).map(([value, label]) => ({ value, label })),
]

function syncUrl() {
  const q: Record<string, string> = {}
  if (page.value > 1) q.page = String(page.value)
  if (actorKeyword.value) q.actorKeyword = actorKeyword.value
  if (actionTypeFilter.value) q.actionType = actionTypeFilter.value
  if (targetTypeFilter.value) q.targetType = targetTypeFilter.value
  if (startTime.value) q.startTime = startTime.value
  if (endTime.value) q.endTime = endTime.value
  router.replace({ query: q })
}

async function fetch() {
  const id = ++requestId
  loading.value = true; error.value = ''
  try {
    const r = await getAuditLogs({
      page: page.value, size,
      actorKeyword: actorKeyword.value || undefined,
      actionType: actionTypeFilter.value || undefined,
      targetType: targetTypeFilter.value || undefined,
      startTime: startTime.value || undefined,
      endTime: endTime.value || undefined,
    })
    if (id !== requestId) return
    items.value = r.items; total.value = r.total
  } catch {
    if (id !== requestId) return
    error.value = '加载操作日志失败'
  } finally {
    if (id === requestId) loading.value = false
  }
}

function onSearch() { page.value = 1; fetch(); syncUrl() }
function onFilterChange() { page.value = 1; fetch(); syncUrl() }
function onPageChange(p: number) { page.value = p; fetch(); syncUrl() }

watch(() => route.query, () => {
  page.value = parseInt(route.query.page as string) || 1
  actorKeyword.value = (route.query.actorKeyword as string) || ''
  actionTypeFilter.value = (route.query.actionType as string) || ''
  targetTypeFilter.value = (route.query.targetType as string) || ''
  startTime.value = (route.query.startTime as string) || ''
  endTime.value = (route.query.endTime as string) || ''
})

onMounted(fetch)
</script>

<template>
  <PageContainer>
    <template #header>
      <h1 class="page-title">操作日志</h1>
    </template>

    <div class="filter-row">
      <input v-model="actorKeyword" class="search-input" placeholder="搜索操作者..." @keyup.enter="onSearch" />
      <button class="search-btn" @click="onSearch">搜索</button>
      <select v-model="actionTypeFilter" class="filter-select" @change="onFilterChange">
        <option v-for="opt in actionOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
      <select v-model="targetTypeFilter" class="filter-select" @change="onFilterChange">
        <option v-for="opt in targetOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
      </select>
      <label class="date-label">从</label>
      <input v-model="startTime" type="datetime-local" class="date-input" @change="onFilterChange" />
      <label class="date-label">到</label>
      <input v-model="endTime" type="datetime-local" class="date-input" @change="onFilterChange" />
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetch" />

    <template v-else>
      <div v-if="items.length === 0" class="empty-state">
        <p>暂无操作日志</p>
      </div>

      <div v-else class="log-table">
        <div class="table-header">
          <span class="col-time">时间</span>
          <span class="col-actor">操作者</span>
          <span class="col-action">操作</span>
          <span class="col-target">目标</span>
          <span class="col-state">状态变更</span>
          <span class="col-reason">原因</span>
        </div>
        <div v-for="item in items" :key="item.id" class="table-row">
          <span class="col-time">{{ new Date(item.createTime).toLocaleString('zh-CN') }}</span>
          <span class="col-actor">{{ item.actorNickname || item.actorUsername }}<span class="actor-sub">@{{ item.actorUsername }}</span></span>
          <span class="col-action">
            <span class="action-tag">{{ actionLabels[item.actionType] || item.actionType }}</span>
          </span>
          <span class="col-target">{{ targetLabels[item.targetType] || item.targetType }}<span v-if="item.targetId" class="target-sub">#{{ item.targetId }}</span></span>
          <span class="col-state">
            <template v-if="item.beforeState || item.afterState">
              <span class="state-before">{{ item.beforeState || '-' }}</span>
              <span class="state-arrow">→</span>
              <span class="state-after">{{ item.afterState || '-' }}</span>
            </template>
            <span v-else class="state-none">-</span>
          </span>
          <span class="col-reason">{{ item.reason || '-' }}</span>
        </div>
      </div>

      <PaginationBar v-if="total > 0" :page="page" :total="total" :size="size" @change="onPageChange" />
    </template>
  </PageContainer>
</template>

<style scoped>
.page-title { font-family: var(--font-headline); font-size: var(--text-display-lg); font-weight: 700; color: var(--color-on-surface); }
.filter-row { display: flex; gap: 8px; margin-bottom: var(--space-stack-md); flex-wrap: wrap; align-items: center; }
.search-input {
  flex: 1; min-width: 160px; max-width: 240px; padding: 8px 12px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); font-size: var(--text-body-md); color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
}
.search-input:focus { outline: none; border-color: var(--color-primary-container); }
.search-btn { height: 38px; padding: 0 16px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); background: var(--color-surface-card); color: var(--color-on-surface); font-size: var(--text-body-md); cursor: pointer; }
.filter-select {
  height: 38px; padding: 0 12px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md);
  background: var(--color-surface-card); color: var(--color-on-surface); font-size: var(--text-body-md); cursor: pointer;
}
.filter-select:focus { outline: none; border-color: var(--color-primary-container); }
.date-label { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
.date-input {
  height: 38px; padding: 0 8px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md);
  background: var(--color-surface-card); color: var(--color-on-surface); font-size: var(--text-body-sm);
}
.date-input:focus { outline: none; border-color: var(--color-primary-container); }

.log-table { border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); overflow: hidden; }
.table-header {
  display: flex; padding: 10px 16px; background: var(--color-surface-container-low);
  font-size: var(--text-body-sm); font-weight: 600; color: var(--color-on-surface-variant);
}
.table-row {
  display: flex; align-items: center; padding: 10px 16px;
  border-top: 1px solid var(--color-border-subtle); transition: background 0.15s;
}
.table-row:hover { background: var(--color-surface-container-low); }

.col-time { width: 150px; flex-shrink: 0; font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
.col-actor { width: 140px; flex-shrink: 0; font-size: var(--text-body-sm); color: var(--color-on-surface); }
.actor-sub { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); margin-left: 4px; }
.col-action { width: 100px; flex-shrink: 0; }
.col-target { width: 110px; flex-shrink: 0; font-size: var(--text-body-sm); color: var(--color-on-surface); }
.target-sub { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); margin-left: 4px; }
.col-state { flex: 1; min-width: 120px; font-size: var(--text-body-sm); }
.col-reason { width: 150px; flex-shrink: 0; font-size: var(--text-body-sm); color: var(--color-on-surface-variant); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.action-tag {
  display: inline-block; font-size: var(--text-label-sm); font-weight: 500;
  padding: 1px 8px; border-radius: 999px;
  background: var(--color-surface-container); color: var(--color-on-surface);
}
.state-before { color: var(--color-status-error); }
.state-arrow { margin: 0 4px; color: var(--color-on-surface-variant); }
.state-after { color: var(--color-status-success); }
.state-none { color: var(--color-on-surface-variant); }

.empty-state { text-align: center; padding: 48px 16px; color: var(--color-on-surface-variant); }
</style>
