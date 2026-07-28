<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { navLabels, actionLabels } from '@/constants/labels'
import { getUnreadCount } from '@/api/notifications'

const auth = useAuthStore()
const router = useRouter()
const unreadCount = ref(0)

let pollTimer: ReturnType<typeof setInterval> | null = null

async function handleLogout() {
  stopPolling()
  await auth.logout()
  router.push({ name: 'login' })
}

async function refreshUnread() {
  try { unreadCount.value = await getUnreadCount() } catch { /* ignore */ }
}

function startPolling() {
  if (pollTimer !== null) return
  refreshUnread()
  pollTimer = setInterval(refreshUnread, 30_000)
}

function stopPolling() {
  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function handleVisibilityChange() {
  if (document.hidden) {
    stopPolling()
  } else {
    startPolling()
  }
}

onMounted(() => {
  startPolling()
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  stopPolling()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <div class="header-left">
        <RouterLink to="/" class="brand">
          <span class="brand-icon">&lt;/&gt;</span>
          <span class="brand-text">ACMate</span>
        </RouterLink>
        <nav class="main-nav">
          <RouterLink to="/" class="nav-link" active-class="nav-link-active">
            {{ navLabels.home }}
          </RouterLink>
          <RouterLink to="/problems" class="nav-link" active-class="nav-link-active">
            {{ navLabels.problems }}
          </RouterLink>
          <RouterLink to="/my/problems" class="nav-link" active-class="nav-link-active">
            {{ navLabels.myProblems }}
          </RouterLink>
          <RouterLink to="/training-plans" class="nav-link" active-class="nav-link-active">
            {{ navLabels.trainingPlans }}
          </RouterLink>
          <RouterLink to="/posts" class="nav-link" active-class="nav-link-active">
            {{ navLabels.discussions }}
          </RouterLink>
          <RouterLink to="/leaderboard" class="nav-link" active-class="nav-link-active">
            {{ navLabels.leaderboard }}
          </RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/problems" class="nav-link" active-class="nav-link-active">
            {{ navLabels.adminProblems }}
          </RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/users" class="nav-link" active-class="nav-link-active">
            {{ navLabels.adminUsers }}
          </RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/posts" class="nav-link" active-class="nav-link-active">
            {{ navLabels.adminPosts }}
          </RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/sync-tasks" class="nav-link" active-class="nav-link-active">
            {{ navLabels.adminSyncTasks }}
          </RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/audit-logs" class="nav-link" active-class="nav-link-active">
            {{ navLabels.adminAuditLogs }}
          </RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/exports" class="nav-link" active-class="nav-link-active">
            {{ navLabels.adminExports }}
          </RouterLink>
        </nav>
      </div>
      <div class="header-right">
        <RouterLink v-if="auth.user" to="/notifications" class="notif-bell">
          &#128276;
          <span v-if="unreadCount > 0" class="badge">{{ unreadCount }}</span>
        </RouterLink>
        <RouterLink v-if="auth.user" to="/settings/profile" class="user-greeting">
          {{ auth.user.nickname || auth.user.username }}
        </RouterLink>
        <span v-if="auth.isAdmin" class="admin-badge">管理员</span>
        <button class="logout-btn" @click="handleLogout">
          {{ actionLabels.logout }}
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 50;
  height: var(--header-height);
  background: var(--color-surface-container-lowest);
  border-bottom: 1px solid var(--color-border-subtle);
  box-shadow: var(--shadow-header);
}

.header-inner {
  max-width: var(--container-max);
  margin: 0 auto;
  padding: 0 var(--space-margin-page);
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-headline);
  font-size: var(--text-headline-md);
  font-weight: 700;
  color: var(--color-primary);
}

.brand-icon {
  font-family: var(--font-mono);
  font-size: 24px;
}

.main-nav {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 100%;
}

.nav-link {
  color: var(--color-on-surface-variant);
  font-weight: 500;
  font-size: var(--text-body-md);
  transition: color 0.2s;
  height: 100%;
  display: flex;
  align-items: center;
}

.nav-link:hover {
  color: var(--color-primary);
}

.nav-link-active {
  color: var(--color-primary);
  border-bottom: 2px solid var(--color-primary);
  font-weight: 700;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-greeting {
  color: var(--color-on-surface);
  font-weight: 500;
  transition: color 0.2s;
}

.user-greeting:hover {
  color: var(--color-primary);
}

.admin-badge {
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-on-primary);
  background: var(--color-primary-container);
  padding: 2px 10px;
  border-radius: 999px;
}

.notif-bell { position: relative; font-size: 20px; color: var(--color-on-surface-variant); padding: 4px; }
.notif-bell .badge {
  position: absolute; top: -2px; right: -4px;
  background: var(--color-status-error); color: #fff; font-size: 10px; font-weight: 700;
  min-width: 16px; height: 16px; border-radius: 8px; display: flex; align-items: center; justify-content: center;
}

.logout-btn {
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
  font-weight: 500;
  padding: 6px 12px;
  border-radius: var(--radius-md);
  transition: background 0.2s, color 0.2s;
}

.logout-btn:hover {
  color: var(--color-primary);
  background: var(--color-surface-container-low);
}
</style>
