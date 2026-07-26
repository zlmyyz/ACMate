<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { navLabels, actionLabels } from '@/constants/labels'

const auth = useAuthStore()
const router = useRouter()

async function handleLogout() {
  await auth.logout()
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
          <RouterLink v-if="auth.isAdmin" to="/admin/problems" class="nav-link" active-class="nav-link-active">
            {{ navLabels.adminProblems }}
          </RouterLink>
        </nav>
      </div>
      <div class="header-right">
        <span v-if="auth.user" class="user-greeting">
          {{ auth.user.nickname || auth.user.username }}
        </span>
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
}

.admin-badge {
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-on-primary);
  background: var(--color-primary-container);
  padding: 2px 10px;
  border-radius: 999px;
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
