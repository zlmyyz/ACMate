<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getNotifications, markRead, markAllRead } from '@/api/notifications'
import type { NotificationItem } from '@/types/notification'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const items = ref<NotificationItem[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
const page = ref(1)
const size = 20

async function fetch() {
  loading.value = true; error.value = ''
  try {
    const r = await getNotifications(page.value, size)
    items.value = r.items
    total.value = r.total
  } catch {
    error.value = '加载通知失败'
  } finally { loading.value = false }
}

async function onMarkRead(id: number) {
  try { await markRead(id); items.value.find(i => i.id === id)!.isRead = true } catch { /* ignore */ }
}

async function onMarkAllRead() {
  try { await markAllRead(); items.value.forEach(i => i.isRead = true) } catch { /* ignore */ }
}

function onPageChange(p: number) { page.value = p; fetch() }

function linkFor(n: NotificationItem): string | null {
  if (!n.resourceType || !n.resourceId) return null
  switch (n.resourceType) {
    case 'post': return `/posts/${n.resourceId}`
    case 'plan': return `/training-plans/${n.resourceId}`
    case 'problem': return `/problems/${n.resourceId}`
    case 'oj_account': return '/settings/oj-account'
    default: return null
  }
}

const typeLabels: Record<string, string> = {
  PLAN_UPDATE: '计划更新',
  PLAN_START: '计划开始',
  PLAN_REMOVE: '移除成员',
  POST_REPLY: '帖子回复',
  CONTENT_DEACTIVATE: '内容停用',
  OJ_VERIFY: '账号审核',
  OJ_SYNC_FAIL: '同步失败',
}

onMounted(fetch)
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="header-row">
        <h1 class="page-title">通知中心</h1>
        <button v-if="items.some(i => !i.isRead)" class="mark-all-btn" @click="onMarkAllRead">全部已读</button>
      </div>
    </template>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetch" />

    <template v-else>
      <div v-if="items.length === 0" class="empty-state"><p>暂无通知</p></div>

      <div v-else class="notification-list">
        <div
          v-for="n in items" :key="n.id"
          class="notification-item"
          :class="{ unread: !n.isRead }"
          @click="!n.isRead && onMarkRead(n.id)"
        >
          <div class="notif-main">
            <span class="notif-type">{{ typeLabels[n.type] || n.type }}</span>
            <span class="notif-title">
              <RouterLink v-if="linkFor(n)" :to="linkFor(n)!" class="notif-link">{{ n.title }}</RouterLink>
              <span v-else>{{ n.title }}</span>
            </span>
            <span v-if="n.content" class="notif-content">{{ n.content }}</span>
          </div>
          <span class="notif-time">{{ new Date(n.createTime).toLocaleDateString() }}</span>
        </div>
      </div>

      <PaginationBar :page="page" :size="size" :total="total" @change="onPageChange" />
    </template>
  </PageContainer>
</template>

<style scoped>
.page-title { font-family: var(--font-headline); font-size: var(--text-display-lg); font-weight: 700; color: var(--color-on-surface); }

.header-row { display: flex; align-items: center; justify-content: space-between; }
.mark-all-btn {
  font-size: var(--text-body-sm); color: var(--color-primary); cursor: pointer;
  padding: 4px 12px; border: 1px solid var(--color-primary); border-radius: var(--radius-md);
}
.mark-all-btn:hover { background: var(--color-primary-container); }

.empty-state { text-align: center; padding: 60px 24px; color: var(--color-on-surface-variant); font-size: var(--text-body-lg); }

.notification-list { display: flex; flex-direction: column; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); overflow: hidden; }

.notification-item {
  display: flex; justify-content: space-between; padding: 14px 20px;
  border-bottom: 1px solid var(--color-border-subtle); cursor: default; transition: background 0.15s;
}
.notification-item:last-child { border-bottom: none; }
.notification-item:hover { background: var(--color-surface-container-low); }
.notification-item.unread { background: rgba(0,0,0,0.02); }

.notif-main { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.notif-type { font-size: var(--text-label-sm); font-weight: 600; color: var(--color-primary-container); }
.notif-title { font-size: var(--text-body-md); color: var(--color-on-surface); }
.notif-link { color: var(--color-primary-container); }
.notif-link:hover { text-decoration: underline; }
.notif-content { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.notif-time { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); white-space: nowrap; margin-left: 16px; flex-shrink: 0; }
</style>
