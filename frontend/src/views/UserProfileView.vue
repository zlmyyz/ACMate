<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserProfile } from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import type { UserProfile } from '@/types/user'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const profile = ref<UserProfile | null>(null)
const loading = ref(true)
const error = ref('')
const notFound = ref(false)

const userId = computed(() => Number(route.params.id))
const isSelf = computed(() => auth.user?.id === userId.value)

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

async function fetchProfile() {
  loading.value = true
  error.value = ''
  notFound.value = false
  try {
    profile.value = await getUserProfile(userId.value)
  } catch (e: unknown) {
    const err = e as { response?: { status: number } }
    if (err.response?.status === 404) {
      notFound.value = true
      return
    }
    error.value = '加载用户资料失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function goToEdit() {
  router.push({ name: 'profile-edit' })
}

onMounted(fetchProfile)
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="profile-header-row">
        <button class="back-link" @click="router.push({ name: 'problems' })">
          &larr; 返回
        </button>
      </div>
    </template>

    <LoadingState v-if="loading" />

    <template v-else-if="profile">
      <div class="profile-card">
        <div class="profile-top">
          <div class="avatar-section">
            <div class="avatar">
              <img v-if="profile.avatarUrl" :src="profile.avatarUrl" :alt="profile.nickname" />
              <span v-else class="avatar-placeholder">
                {{ (profile.nickname || profile.username).charAt(0).toUpperCase() }}
              </span>
            </div>
          </div>

          <div class="profile-info">
            <div class="name-row">
              <h1 class="display-name">{{ profile.nickname || profile.username }}</h1>
              <span v-if="profile.admin" class="admin-badge">管理员</span>
            </div>
            <p class="username">@{{ profile.username }}</p>
            <p v-if="profile.bio" class="bio">{{ profile.bio }}</p>

            <div class="stats-row">
              <div class="stat">
                <span class="stat-value">{{ profile.problemCount }}</span>
                <span class="stat-label">题目</span>
              </div>
              <div class="stat">
                <span class="stat-label">注册于</span>
                <span class="stat-value date">{{ formatDate(profile.createTime) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="isSelf" class="profile-actions">
          <button class="edit-btn" @click="goToEdit">编辑资料</button>
        </div>
      </div>
    </template>

    <ErrorState
      v-else-if="error"
      :message="error"
      @retry="fetchProfile"
    />

    <div v-else-if="notFound" class="status-page">
      <h2>用户不存在</h2>
      <p>该用户不存在或已注销。</p>
      <button class="status-btn" @click="router.push({ name: 'problems' })">返回题库</button>
    </div>
  </PageContainer>
</template>

<style scoped>
.profile-header-row {
  display: flex;
  align-items: center;
}

.back-link {
  background: none;
  border: none;
  color: var(--color-primary-container);
  font-size: var(--text-body-md);
  font-weight: 500;
  cursor: pointer;
  padding: 0;
}

.back-link:hover {
  text-decoration: underline;
}

.profile-card {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 32px;
}

.profile-top {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}

.avatar-section {
  flex-shrink: 0;
}

.avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--color-surface-container-low);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  font-family: var(--font-headline);
  font-size: 40px;
  font-weight: 700;
  color: var(--color-on-surface-variant);
}

.profile-info {
  flex: 1;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.display-name {
  font-family: var(--font-headline);
  font-size: var(--text-display-lg);
  font-weight: 700;
  color: var(--color-on-surface);
  line-height: var(--leading-display-lg);
}

.admin-badge {
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-on-primary);
  background: var(--color-primary-container);
  padding: 2px 10px;
  border-radius: 999px;
}

.username {
  margin-top: 4px;
  font-size: var(--text-body-md);
  color: var(--color-on-surface-variant);
  font-family: var(--font-mono);
}

.bio {
  margin-top: 12px;
  font-size: var(--text-body-md);
  color: var(--color-on-surface);
  line-height: 1.6;
  white-space: pre-wrap;
}

.stats-row {
  display: flex;
  gap: 32px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-subtle);
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: var(--text-headline-sm);
  font-weight: 700;
  color: var(--color-on-surface);
}

.stat-value.date {
  font-size: var(--text-body-md);
  font-weight: 500;
}

.stat-label {
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-on-surface-variant);
  letter-spacing: 0.05em;
}

.profile-actions {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-subtle);
}

.edit-btn {
  height: 36px;
  padding: 0 20px;
  border: 1px solid var(--color-primary-container);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-primary-container);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.edit-btn:hover {
  background: var(--color-primary-container);
  color: var(--color-on-primary);
}

.status-page {
  text-align: center;
  padding: 60px 24px;
}

.status-page h2 {
  font-family: var(--font-headline);
  font-size: var(--text-headline-md);
  font-weight: 700;
  color: var(--color-on-surface);
  margin-bottom: 8px;
}

.status-page p {
  color: var(--color-on-surface-variant);
  margin-bottom: 24px;
}

.status-btn {
  padding: 10px 28px;
  background: var(--color-primary-container);
  color: var(--color-on-primary);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
}

.status-btn:hover {
  opacity: 0.9;
}
</style>
