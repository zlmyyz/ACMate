<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getSyncTasks } from '@/api/sync-tasks'
import type { SyncTaskItem } from '@/types/sync-task'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const items = ref<SyncTaskItem[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
const page = ref(1)
const size = 20
const statusFilter = ref('')

async function fetch() {
  loading.value = true; error.value = ''
  try {
    const r = await getSyncTasks({ page: page.value, size, taskStatus: statusFilter.value || undefined })
    items.value = r.items; total.value = r.total
  } catch { error.value = '加载失败' }
  finally { loading.value = false }
}

function onPageChange(p: number) { page.value = p; fetch() }

const statusLabels: Record<string, string> = {
  PENDING: '等待中', RUNNING: '进行中', SUCCESS: '成功', FAILED: '失败',
}
const triggerLabels: Record<string, string> = { SCHEDULED: '定时', MANUAL: '手动' }

onMounted(fetch)
</script>

<template>
  <PageContainer>
    <template #header><h1 class="page-title">同步任务管理</h1></template>

    <div class="filters">
      <select v-model="statusFilter" class="filter-select" @change="page=1; fetch()">
        <option value="">全部状态</option>
        <option value="PENDING">等待中</option>
        <option value="RUNNING">进行中</option>
        <option value="SUCCESS">成功</option>
        <option value="FAILED">失败</option>
      </select>
      <button class="btn-sync" disabled title="同步功能暂未开放">手动同步 (暂未开放)</button>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetch" />

    <template v-else>
      <div v-if="items.length === 0" class="empty-state"><p>暂无同步任务</p></div>

      <div v-else class="table">
        <div class="table-header">
          <span class="col-id">ID</span><span class="col-acc">账号</span><span class="col-trigger">触发</span>
          <span class="col-status">状态</span><span class="col-count">获取/写入/首次AC</span><span class="col-error">错误</span>
        </div>
        <div v-for="item in items" :key="item.id" class="table-row">
          <span class="col-id">{{ item.id }}</span>
          <span class="col-acc">#{{ item.ojAccountId }} / {{ item.platform }}</span>
          <span class="col-trigger">{{ triggerLabels[item.triggerType] || item.triggerType }}</span>
          <span class="col-status"><span :class="'tag-' + item.taskStatus.toLowerCase()">{{ statusLabels[item.taskStatus] || item.taskStatus }}</span></span>
          <span class="col-count">{{ item.fetchedCount }} / {{ item.insertedCount }} / {{ item.firstAcCount }}</span>
          <span class="col-error" :title="item.errorMessage || ''">{{ item.errorMessage || '-' }}</span>
        </div>
      </div>
      <PaginationBar :page="page" :size="size" :total="total" @change="onPageChange" />
    </template>
  </PageContainer>
</template>

<style scoped>
.page-title { font-family: var(--font-headline); font-size: var(--text-display-lg); font-weight: 700; color: var(--color-on-surface); }
.filters { margin-bottom: var(--space-stack-md); }
.filter-select { padding: 6px 12px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); font-size: var(--text-body-md); }
.btn-sync { margin-left: 12px; padding: 6px 16px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); background: var(--color-surface-container-low); color: var(--color-on-surface-variant); font-size: var(--text-body-sm); cursor: not-allowed; opacity: 0.7; }
.empty-state { text-align: center; padding: 60px 24px; color: var(--color-on-surface-variant); }
.table { border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); overflow: hidden; }
.table-header { display: flex; padding: 10px 16px; background: var(--color-surface-container-low); font-size: var(--text-body-sm); font-weight: 600; color: var(--color-on-surface-variant); }
.table-row { display: flex; padding: 10px 16px; border-top: 1px solid var(--color-border-subtle); align-items: center; }
.col-id { width: 60px; flex-shrink: 0; }
.col-acc { width: 140px; flex-shrink: 0; }
.col-trigger { width: 60px; flex-shrink: 0; }
.col-status { width: 80px; flex-shrink: 0; }
.col-count { width: 130px; flex-shrink: 0; }
.col-error { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: var(--text-body-sm); color: var(--color-status-error); }
.tag-pending { color: var(--color-status-pending); font-size: var(--text-label-sm); }
.tag-running { color: var(--color-primary); font-size: var(--text-label-sm); }
.tag-success { color: var(--color-status-success); font-size: var(--text-label-sm); }
.tag-failed { color: var(--color-status-error); font-size: var(--text-label-sm); }
</style>
