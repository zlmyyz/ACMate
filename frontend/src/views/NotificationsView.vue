<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getNotifications } from '@/api/notifications'
import { useNotificationStore } from '@/stores/notifications'
import type { NotificationItem } from '@/types/notification'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const notification = useNotificationStore()

const items = ref<NotificationItem[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
const page = ref(1)
const size = 20
const unreadOnly = ref(false)

async function fetch() {
  loading.value = true; error.value = ''
  try {
    const r = await getNotifications(page.value, size, unreadOnly.value)
    items.value = r.items
    total.value = r.total
  } catch {
    error.value = '加载通知失败'
  } finally { loading.value = false }
}

async function onMarkRead(id: number) {
  await notification.markOneRead(id)
  const item = items.value.find(i => i.id === id)
  if (item) item.isRead = true
}

async function onMarkAllRead() {
  await notification.markAllAsRead()
  items.value.forEach(i => i.isRead = true)
}

function onPageChange(p: number) { page.value = p; fetch() }

function toggleFilter() {
  unreadOnly.value = !unreadOnly.value
  page.value = 1
  fetch()
}

function linkFor(n: NotificationItem): string | null {
  if (!n.resourceType || !n.resourceId) return null
  switch (n.resourceType) {
    case 'POST': return `/posts/${n.resourceId}`
    case 'COMMENT': {
      const pid = n.payload && n.payload.postId
      return pid ? `/posts/${pid}` : null
    }
    case 'TRAINING_PLAN': return `/training-plans/${n.resourceId}`
    default: return null
  }
}

function notificationText(n: NotificationItem): string {
  const p = n.payload || {}
  const actor = p.actorNickname || ''
  const planTitle = p.planTitle || ''
  const postTitle = p.postTitle || ''
  const reason = p.reason || ''

  switch (n.notificationType) {
    case 'POST_COMMENTED':
      return `${actor} 评论了你的帖子「${postTitle}」`
    case 'COMMENT_REPLIED':
      return `${actor} 回复了你的评论`
    case 'POST_ADMIN_DEACTIVATED':
      return `管理员停用了你的帖子「${postTitle}」${reason ? '，原因：' + reason : ''}`
    case 'COMMENT_ADMIN_DEACTIVATED':
      return `管理员停用了你的评论${reason ? '，原因：' + reason : ''}`
    case 'POST_RESTORED':
      return `管理员恢复了你的帖子「${postTitle}」`
    case 'COMMENT_RESTORED':
      return `管理员恢复了你的评论`
    case 'TRAINING_MEMBER_REMOVED':
      return `你被移出了计划「${planTitle}」`
    case 'TRAINING_ADMIN_DEACTIVATED':
      return `管理员停用了计划「${planTitle}」${reason ? '，原因：' + reason : ''}`
    case 'TRAINING_RESTORED':
      return `管理员恢复了计划「${planTitle}」`
    case 'TRAINING_SCHEDULE_CHANGED':
      return `计划「${planTitle}」的时间安排已更新`
    case 'TRAINING_PROBLEMS_CHANGED':
      return `计划「${planTitle}」的题目列表已更新`
    case 'ANNOUNCEMENT_BROADCAST':
      return `${actor} 发布了全站公告「${postTitle}」`
    default:
      return n.notificationType || '未知通知'
  }
}

onMounted(fetch)
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="header-row">
        <h1 class="page-title">通知中心</h1>
        <div class="header-actions">
          <label class="filter-toggle">
            <input type="checkbox" :checked="unreadOnly" @change="toggleFilter" />
            <span>只看未读</span>
          </label>
          <button v-if="items.some(i => !i.isRead)" class="mark-all-btn" @click="onMarkAllRead">全部已读</button>
        </div>
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
        >
          <div class="notif-main">
            <span class="notif-text">{{ notificationText(n) }}</span>
          </div>
          <div class="notif-right">
            <RouterLink
              v-if="linkFor(n)"
              :to="linkFor(n)!"
              class="notif-link"
              @click="!n.isRead && onMarkRead(n.id)"
            >查看</RouterLink>
            <button
              v-if="!n.isRead"
              class="mark-read-btn"
              @click="onMarkRead(n.id)"
            >已读</button>
          </div>
          <span class="notif-time">{{ new Date(n.createTime).toLocaleString() }}</span>
        </div>
      </div>

      <PaginationBar :page="page" :size="size" :total="total" @change="onPageChange" />
    </template>
  </PageContainer>
</template>

<style scoped>
.page-title { font-family: var(--font-headline); font-size: var(--text-display-lg); font-weight: 700; color: var(--color-on-surface); }

.header-row { display: flex; align-items: center; justify-content: space-between; }
.header-actions { display: flex; align-items: center; gap: 16px; }
.filter-toggle { display: flex; align-items: center; gap: 6px; font-size: var(--text-body-sm); color: var(--color-on-surface-variant); cursor: pointer; }
.mark-all-btn {
  font-size: var(--text-body-sm); color: var(--color-primary); cursor: pointer;
  padding: 4px 12px; border: 1px solid var(--color-primary); border-radius: var(--radius-md);
}
.mark-all-btn:hover { background: var(--color-primary-container); }

.empty-state { text-align: center; padding: 60px 24px; color: var(--color-on-surface-variant); font-size: var(--text-body-lg); }

.notification-list { display: flex; flex-direction: column; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); overflow: hidden; }

.notification-item {
  display: flex; justify-content: space-between; align-items: center; padding: 14px 20px;
  border-bottom: 1px solid var(--color-border-subtle); transition: background 0.15s; gap: 12px; flex-wrap: wrap;
}
.notification-item:last-child { border-bottom: none; }
.notification-item:hover { background: var(--color-surface-container-low); }
.notification-item.unread { background: rgba(0,0,0,0.02); font-weight: 500; }

.notif-main { flex: 1; min-width: 0; }
.notif-text { font-size: var(--text-body-md); color: var(--color-on-surface); }

.notif-right { display: flex; gap: 8px; align-items: center; flex-shrink: 0; }
.notif-link { font-size: var(--text-body-sm); color: var(--color-primary); }
.notif-link:hover { text-decoration: underline; }
.mark-read-btn { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); cursor: pointer; padding: 2px 8px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-sm); }
.mark-read-btn:hover { background: var(--color-surface-container-low); }

.notif-time { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); white-space: nowrap; flex-shrink: 0; width: 100%; text-align: right; margin-top: 4px; }
</style>
