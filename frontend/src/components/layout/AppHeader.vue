<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'
import { useRouter } from 'vue-router'
import { navLabels, actionLabels } from '@/constants/labels'

const auth = useAuthStore()
const notification = useNotificationStore()
const router = useRouter()

const badgeText = computed(() => {
  const c = notification.unreadCount
  if (c <= 0) return ''
  return c > 99 ? '99+' : String(c)
})

const menuOpen = ref(false)
const userMenuRef = ref<HTMLElement | null>(null)

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}

function closeMenu() {
  menuOpen.value = false
}

function onDocumentClick(e: MouseEvent) {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target as Node)) {
    closeMenu()
  }
}

onMounted(() => document.addEventListener('click', onDocumentClick))
onUnmounted(() => document.removeEventListener('click', onDocumentClick))

async function handleLogout() {
  await auth.logout()
  notification.reset()
  router.push({ name: 'login' })
}
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
          <span v-if="badgeText" class="badge">{{ badgeText }}</span>
        </RouterLink>
        <div v-if="auth.user" ref="userMenuRef" class="user-menu">
          <button class="user-trigger" @click="toggleMenu">
            {{ auth.user.nickname || auth.user.username }}
            <span class="arrow" :class="{ open: menuOpen }">&#9662;</span>
          </button>
          <div v-if="menuOpen" class="menu-dropdown" @click="closeMenu">
            <RouterLink to="/settings/profile" class="menu-item">编辑资料</RouterLink>
            <RouterLink to="/settings/oj-account" class="menu-item">OJ 账号</RouterLink>
            <button class="menu-item logout" @click="handleLogout">
              {{ actionLabels.logout }}
            </button>
          </div>
        </div>
        <span v-if="auth.isAdmin" class="admin-badge">管理员</span>
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

.user-menu { position: relative; }

.user-trigger {
  display: flex; align-items: center; gap: 4px;
  color: var(--color-on-surface); font-weight: 600; font-size: var(--text-body-md);
  padding: 8px 14px; border-radius: var(--radius-md); cursor: pointer;
  border: 1px solid var(--color-border-subtle);
  transition: background 0.2s, border-color 0.2s;
}
.user-trigger:hover { background: var(--color-surface-container-low); border-color: var(--color-primary-container); }
.arrow { font-size: 12px; transition: transform 0.2s; margin-left: 2px; }
.arrow.open { transform: rotate(180deg); }

.menu-dropdown {
  position: absolute; top: 100%; right: 0; margin-top: 4px;
  min-width: 140px; background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md);
  box-shadow: var(--shadow-card); overflow: hidden; z-index: 100;
}
.menu-item {
  display: block; width: 100%; padding: 10px 16px; text-align: left;
  font-size: var(--text-body-md); font-weight: 500; color: var(--color-on-surface);
  transition: background 0.15s;
}
.menu-item:hover { background: var(--color-surface-container-low); }
.menu-item.logout { color: var(--color-status-error); border-top: 1px solid var(--color-border-subtle); }
</style>
