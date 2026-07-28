<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'
import AppHeader from '@/components/layout/AppHeader.vue'
import { watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const auth = useAuthStore()
const notification = useNotificationStore()
const router = useRouter()
const route = useRoute()

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      notification.startPolling()
      notification.startVisibilityListener()
    } else {
      notification.reset()
      if (route.meta.requiresAuth) {
        router.push({ name: 'login', query: { redirect: route.fullPath } })
      }
    }
  },
  { immediate: true },
)

function retryInit() {
  auth.clearInitError()
  auth.init()
}
</script>

<template>
  <template v-if="auth.initialized">
    <template v-if="auth.initError">
      <div class="init-error-page">
        <div class="init-error-card">
          <p class="init-error-text">{{ auth.initError }}</p>
          <button class="retry-btn" @click="retryInit">重试</button>
        </div>
      </div>
    </template>
    <template v-else>
      <AppHeader v-if="auth.isLoggedIn" />
      <main class="app-main">
        <RouterView />
      </main>
    </template>
  </template>
  <div v-else class="init-loading">
    <span>加载中...</span>
  </div>
</template>

<style scoped>
.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.init-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-on-surface-variant);
}

.init-error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface-background);
}

.init-error-card {
  text-align: center;
  padding: 40px;
}

.init-error-text {
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-lg);
  margin-bottom: 24px;
}

.retry-btn {
  padding: 10px 32px;
  background: var(--color-primary-container);
  color: var(--color-on-primary);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
}

.retry-btn:hover {
  opacity: 0.9;
}
</style>
