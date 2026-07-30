<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getUserProfile, getUserProblems, getUserTrainingPlans } from '@/api/users'
import { useAuthStore } from '@/stores/auth'
import type { UserProfile } from '@/types/user'
import type { ProblemSummary } from '@/types/problem'
import type { PublicPlanSummary } from '@/types/user'
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

const problems = ref<ProblemSummary[]>([])
const problemsTotal = ref(0)
const problemsPage = ref(1)
const problemsLoading = ref(false)
const pageSize = 20

const plans = ref<PublicPlanSummary[]>([])
const plansTotal = ref(0)
const plansPage = ref(1)
const plansLoading = ref(false)

const userId = computed(() => Number(route.params.id))
const isSelf = computed(() => auth.user?.id === userId.value)

function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
}

function formatDateTime(dateStr: string): string {
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const timeStatusLabel: Record<string, string> = {
  NOT_STARTED: '未开始',
  ONGOING: '进行中',
  ENDED: '已结束',
}

async function fetchProfile() {
  loading.value = true
  error.value = ''
  notFound.value = false
  try {
    profile.value = await getUserProfile(userId.value)
    fetchProblems()
    fetchPlans()
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

async function fetchProblems() {
  problemsLoading.value = true
  try {
    const data = await getUserProblems(userId.value, problemsPage.value, pageSize)
    problems.value = data.records
    problemsTotal.value = data.total
  } catch {
    problems.value = []
    problemsTotal.value = 0
  } finally {
    problemsLoading.value = false
  }
}

async function fetchPlans() {
  plansLoading.value = true
  try {
    const data = await getUserTrainingPlans(userId.value, plansPage.value, pageSize)
    plans.value = data.plans
    plansTotal.value = data.total
  } catch {
    plans.value = []
    plansTotal.value = 0
  } finally {
    plansLoading.value = false
  }
}

function changeProblemsPage(page: number) {
  problemsPage.value = page
  fetchProblems()
}

function changePlansPage(page: number) {
  plansPage.value = page
  fetchPlans()
}

const problemsTotalPages = computed(() => Math.max(1, Math.ceil(problemsTotal.value / pageSize)))
const plansTotalPages = computed(() => Math.max(1, Math.ceil(plansTotal.value / pageSize)))

function goToEdit() {
  router.push({ name: 'profile-edit' })
}

function goToProblem(id: number) {
  router.push({ name: 'problem-detail', params: { id } })
}

function goToPlan(id: number) {
  router.push({ name: 'plan-detail', params: { id } })
}

const ojStatsItems = computed(() => {
  const s = profile.value?.ojStats
  if (!s) return []
  return [
    { label: '总通过', value: s.solvedCount },
    { label: '30天通过', value: s.solvedCount30d },
    { label: '7天通过', value: s.solvedCount7d },
  ]
})

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
      <!-- Disabled banner -->
      <div v-if="profile.accountStatus === 'DISABLED'" class="disabled-banner">
        账号已停用
      </div>

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
                <span class="stat-value">{{ profile.createdProblemCount }}</span>
                <span class="stat-label">题目</span>
              </div>
              <div class="stat">
                <span class="stat-label">注册于</span>
                <span class="stat-value date">{{ formatDate(profile.createTime) }}</span>
              </div>
            </div>

            <!-- Codeforces handle -->
            <div v-if="profile.codeforcesHandle" class="cf-handle-row">
              <span class="cf-label">Codeforces</span>
              <a
                :href="`https://codeforces.com/profile/${profile.codeforcesHandle}`"
                target="_blank"
                rel="noopener noreferrer"
                class="cf-link"
              >
                {{ profile.codeforcesHandle }}
              </a>
            </div>
          </div>
        </div>

        <div v-if="isSelf" class="profile-actions">
          <button class="edit-btn" @click="goToEdit">编辑资料</button>
        </div>
      </div>

      <!-- OJ Stats -->
      <section v-if="ojStatsItems.length" class="section">
        <h2 class="section-title">刷题统计</h2>
        <div class="oj-stats-grid">
          <div v-for="item in ojStatsItems" :key="item.label" class="oj-stat-card">
            <span class="oj-stat-value">{{ item.value }}</span>
            <span class="oj-stat-label">{{ item.label }}</span>
          </div>
        </div>
      </section>

      <!-- Public Problems -->
      <section class="section">
        <h2 class="section-title">公开题目 ({{ problemsTotal }})</h2>
        <LoadingState v-if="problemsLoading" />
        <template v-else-if="problems.length">
          <table class="data-table">
            <thead>
              <tr>
                <th>标题</th>
                <th>平台</th>
                <th>难度</th>
                <th>创建时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in problems" :key="p.id" class="clickable-row" @click="goToProblem(p.id)">
                <td class="title-cell">{{ p.title }}</td>
                <td><span class="platform-tag">{{ p.platform }}</span></td>
                <td>{{ p.difficulty || '-' }}</td>
                <td>{{ formatDate(p.createTime) }}</td>
              </tr>
            </tbody>
          </table>
          <div v-if="problemsTotalPages > 1" class="pagination">
            <button :disabled="problemsPage <= 1" @click="changeProblemsPage(problemsPage - 1)">上一页</button>
            <span class="page-info">{{ problemsPage }} / {{ problemsTotalPages }}</span>
            <button :disabled="problemsPage >= problemsTotalPages" @click="changeProblemsPage(problemsPage + 1)">下一页</button>
          </div>
        </template>
        <p v-else class="empty-hint">暂无公开题目</p>
      </section>

      <!-- Public Training Plans -->
      <section class="section">
        <h2 class="section-title">公开训练计划 ({{ plansTotal }})</h2>
        <LoadingState v-if="plansLoading" />
        <template v-else-if="plans.length">
          <div class="plan-cards">
            <div
              v-for="plan in plans"
              :key="plan.id"
              class="plan-card clickable-row"
              @click="goToPlan(plan.id)"
            >
              <div class="plan-card-header">
                <h3 class="plan-title">{{ plan.title }}</h3>
                <span class="plan-status-badge" :class="plan.timeStatus.toLowerCase()">
                  {{ timeStatusLabel[plan.timeStatus] || plan.timeStatus }}
                </span>
              </div>
              <div class="plan-meta">
                <span>{{ plan.problemCount }} 题</span>
                <span>{{ plan.memberCount }} 人</span>
                <span v-if="plan.startTime">{{ formatDateTime(plan.startTime) }}</span>
              </div>
            </div>
          </div>
          <div v-if="plansTotalPages > 1" class="pagination">
            <button :disabled="plansPage <= 1" @click="changePlansPage(plansPage - 1)">上一页</button>
            <span class="page-info">{{ plansPage }} / {{ plansTotalPages }}</span>
            <button :disabled="plansPage >= plansTotalPages" @click="changePlansPage(plansPage + 1)">下一页</button>
          </div>
        </template>
        <p v-else class="empty-hint">暂无公开训练计划</p>
      </section>
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

.disabled-banner {
  background: var(--color-error-container);
  color: var(--color-on-error-container);
  text-align: center;
  padding: 10px 16px;
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  font-weight: 600;
  margin-bottom: 16px;
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

.cf-handle-row {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.cf-label {
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-on-surface-variant);
}

.cf-link {
  font-size: var(--text-body-md);
  font-weight: 600;
  color: var(--color-primary-container);
  text-decoration: none;
  font-family: var(--font-mono);
}

.cf-link:hover {
  text-decoration: underline;
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

/* OJ Stats */
.section {
  margin-top: 24px;
}

.section-title {
  font-family: var(--font-headline);
  font-size: var(--text-headline-sm);
  font-weight: 700;
  color: var(--color-on-surface);
  margin-bottom: 12px;
}

.oj-stats-grid {
  display: flex;
  gap: 16px;
}

.oj-stat-card {
  flex: 1;
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  padding: 16px;
  text-align: center;
}

.oj-stat-value {
  display: block;
  font-size: var(--text-display-md);
  font-weight: 700;
  color: var(--color-primary-container);
}

.oj-stat-label {
  display: block;
  margin-top: 4px;
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-on-surface-variant);
}

/* Table */
.data-table {
  width: 100%;
  border-collapse: collapse;
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.data-table th,
.data-table td {
  padding: 10px 14px;
  text-align: left;
  font-size: var(--text-body-md);
}

.data-table th {
  background: var(--color-surface-container-low);
  color: var(--color-on-surface-variant);
  font-weight: 600;
  font-size: var(--text-label-sm);
  letter-spacing: 0.05em;
}

.data-table tbody tr {
  border-top: 1px solid var(--color-border-subtle);
}

.clickable-row {
  cursor: pointer;
  transition: background 0.15s;
}

.clickable-row:hover {
  background: var(--color-surface-container-low);
}

.title-cell {
  font-weight: 600;
  color: var(--color-on-surface);
}

.platform-tag {
  display: inline-block;
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-on-surface-variant);
  background: var(--color-surface-container-low);
  padding: 2px 8px;
  border-radius: 999px;
}

/* Pagination */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 12px;
}

.pagination button {
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-surface-card);
  color: var(--color-on-surface);
  font-size: var(--text-body-sm);
  cursor: pointer;
}

.pagination button:disabled {
  opacity: 0.4;
  cursor: default;
}

.pagination button:not(:disabled):hover {
  background: var(--color-surface-container-low);
}

.page-info {
  font-size: var(--text-body-sm);
  color: var(--color-on-surface-variant);
}

/* Plan cards */
.plan-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.plan-card {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  padding: 14px 18px;
}

.plan-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.plan-title {
  font-size: var(--text-body-md);
  font-weight: 600;
  color: var(--color-on-surface);
  margin: 0;
}

.plan-status-badge {
  font-size: var(--text-label-xs);
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 999px;
}

.plan-status-badge.ongoing {
  color: var(--color-on-primary);
  background: var(--color-primary-container);
}

.plan-status-badge.ended {
  color: var(--color-on-surface-variant);
  background: var(--color-surface-container-low);
}

.plan-status-badge.not_started {
  color: var(--color-tertiary-container);
  background: var(--color-tertiary);
}

.plan-meta {
  margin-top: 6px;
  display: flex;
  gap: 16px;
  font-size: var(--text-body-sm);
  color: var(--color-on-surface-variant);
}

.empty-hint {
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
  text-align: center;
  padding: 24px 0;
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
