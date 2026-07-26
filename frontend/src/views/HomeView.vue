<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import PageContainer from '@/components/layout/PageContainer.vue'
import { navLabels } from '@/constants/labels'

const auth = useAuthStore()
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="welcome-section">
        <h1 class="welcome-title">
          欢迎回来，{{ auth.user?.nickname || auth.user?.username }}
        </h1>
        <p v-if="auth.isAdmin" class="role-badge">管理员</p>
      </div>
    </template>

    <div class="home-grid">
      <section class="home-card">
        <h2 class="card-title">快速入口</h2>
        <div class="quick-links">
          <RouterLink to="/problems" class="quick-link">
            <span class="link-icon">&#128218;</span>
            <span class="link-label">{{ navLabels.problems }}</span>
          </RouterLink>
          <RouterLink to="/problems/create" class="quick-link">
            <span class="link-icon">&#10133;</span>
            <span class="link-label">创建题目</span>
          </RouterLink>
          <RouterLink to="/my/problems" class="quick-link">
            <span class="link-icon">&#128451;</span>
            <span class="link-label">{{ navLabels.myProblems }}</span>
          </RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/problems" class="quick-link">
            <span class="link-icon">&#128202;</span>
            <span class="link-label">{{ navLabels.adminProblems }}</span>
          </RouterLink>
          <RouterLink to="/training-plans" class="quick-link">
            <span class="link-icon">&#128220;</span>
            <span class="link-label">{{ navLabels.trainingPlans }}</span>
          </RouterLink>
          <RouterLink to="/posts" class="quick-link">
            <span class="link-icon">&#128172;</span>
            <span class="link-label">{{ navLabels.discussions }}</span>
          </RouterLink>
          <RouterLink to="/leaderboard" class="quick-link">
            <span class="link-icon">&#127942;</span>
            <span class="link-label">{{ navLabels.leaderboard }}</span>
          </RouterLink>
        </div>
      </section>

      <section class="home-card">
        <h2 class="card-title">平台说明</h2>
        <p class="card-text">
          ACMate 是程序设计竞赛社团的内部训练与题目管理平台。
          你可以在这里创建和管理题目、浏览题库、参与训练计划。
        </p>
      </section>

      <section class="home-card">
        <h2 class="card-title">后续功能</h2>
        <p class="card-text">更多功能即将推出，敬请期待。</p>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped>
.welcome-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.welcome-title {
  font-family: var(--font-headline);
  font-size: var(--text-display-lg);
  font-weight: 700;
  line-height: var(--leading-display-lg);
  color: var(--color-on-surface);
}

.role-badge {
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-on-primary);
  background: var(--color-primary-container);
  padding: 2px 12px;
  border-radius: 999px;
}

.home-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-stack-lg);
}

.home-card {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 24px;
}

.card-title {
  font-family: var(--font-headline);
  font-size: var(--text-headline-sm);
  font-weight: 600;
  color: var(--color-on-surface);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.card-text {
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
  line-height: 1.7;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.quick-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  font-weight: 500;
  color: var(--color-on-surface);
  transition: box-shadow 0.2s, border-color 0.2s;
}

.quick-link:hover {
  box-shadow: var(--shadow-sm);
}

.disabled-link {
  opacity: 0.5;
  pointer-events: none;
}

.link-icon {
  font-size: 20px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--color-surface-container-low);
}

.upcoming-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upcoming-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
}

.upcoming-badge {
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-status-pending);
  background: rgba(243, 161, 60, 0.12);
  padding: 2px 8px;
  border-radius: 999px;
  white-space: nowrap;
}
</style>
