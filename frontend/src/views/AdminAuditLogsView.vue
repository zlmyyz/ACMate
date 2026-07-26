<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAuditLogs } from '@/api/audit-logs'
import type { AuditLogItem } from '@/types/audit-log'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const items = ref<AuditLogItem[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
const page = ref(1)
const size = 20
const resTypeFilter = ref('')

async function fetch() {
  loading.value = true; error.value = ''
  try {
    const r = await getAuditLogs({ page: page.value, size, resourceType: resTypeFilter.value || undefined })
    items.value = r.items; total.value = r.total
  } catch { error.value = '加载失败' }
  finally { loading.value = false }
}

function onPageChange(p: number) { page.value = p; fetch() }

const actionLabels: Record<string, string> = {
  TOGGLE_STATUS: '状态切换', TOGGLE_ADMIN: '权限变更',
  DEACTIVATE_POST: '停用帖子', RESTORE_POST: '恢复帖子',
  DEACTIVATE_COMMENT: '停用评论', RESTORE_COMMENT: '恢复评论',
}

onMounted(fetch)
</script>

<template>
  <PageContainer>
    <template #header><h1 class="page-title">操作日志</h1></template>

    <div class="filters">
      <select v-model="resTypeFilter" class="filter-select" @change="page=1; fetch()">
        <option value="">全部类型</option>
        <option value="user">用户</option>
        <option value="post">帖子</option>
        <option value="comment">评论</option>
        <option value="oj_account">OJ账号</option>
      </select>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetch" />

    <template v-else>
      <div v-if="items.length === 0" class="empty-state"><p>暂无操作日志</p></div>

      <div v-else class="table">
        <div class="table-header">
          <span class="col-time">时间</span><span class="col-operator">操作者</span>
          <span class="col-action">操作</span><span class="col-resource">资源</span>
          <span class="col-reason">原因</span>
        </div>
        <div v-for="item in items" :key="item.id" class="table-row">
          <span class="col-time">{{ new Date(item.createTime).toLocaleString() }}</span>
          <span class="col-operator">#{{ item.operatorId }}</span>
          <span class="col-action">{{ actionLabels[item.action] || item.action }}</span>
          <span class="col-resource">{{ item.resourceType }}/{{ item.resourceId || '-' }}</span>
          <span class="col-reason">{{ item.reason || '-' }}</span>
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
.empty-state { text-align: center; padding: 60px 24px; color: var(--color-on-surface-variant); }
.table { border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); overflow: hidden; }
.table-header { display: flex; padding: 10px 16px; background: var(--color-surface-container-low); font-size: var(--text-body-sm); font-weight: 600; color: var(--color-on-surface-variant); }
.table-row { display: flex; padding: 10px 16px; border-top: 1px solid var(--color-border-subtle); align-items: center; font-size: var(--text-body-sm); }
.col-time { width: 160px; flex-shrink: 0; }
.col-operator { width: 80px; flex-shrink: 0; }
.col-action { width: 100px; flex-shrink: 0; }
.col-resource { width: 120px; flex-shrink: 0; }
.col-reason { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
