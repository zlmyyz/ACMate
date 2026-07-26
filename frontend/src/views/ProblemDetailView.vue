<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProblemDetail } from '@/api/problems'
import { useAuthStore } from '@/stores/auth'
import type { ProblemDetail } from '@/types/problem'
import { platformLabels } from '@/constants/labels'
import PageContainer from '@/components/layout/PageContainer.vue'
import MarkdownContent from '@/components/common/MarkdownContent.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const problem = ref<ProblemDetail | null>(null)
const loading = ref(true)
const error = ref('')
const notFound = ref(false)
const forbidden = ref(false)

const problemId = computed(() => Number(route.params.id))

const canEdit = computed(() => {
  if (!problem.value || !auth.user) return false
  return auth.isAdmin || auth.user.id === problem.value.creatorUserId
})

const isInactive = computed(() => {
  return route.query.inactive === 'true'
})

async function fetchDetail() {
  loading.value = true
  error.value = ''
  notFound.value = false
  forbidden.value = false
  try {
    problem.value = await getProblemDetail(problemId.value)
  } catch (e: unknown) {
    const err = e as { response?: { status: number } }
    if (err.response?.status === 401) {
      router.push({ name: 'login', query: { redirect: route.fullPath } })
      return
    }
    if (err.response?.status === 404) {
      notFound.value = true
      return
    }
    if (err.response?.status === 403) {
      forbidden.value = true
      return
    }
    error.value = '加载题目详情失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function goToEdit() {
  router.push({ name: 'edit-problem', params: { id: problemId.value } })
}

onMounted(fetchDetail)
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="detail-header">
        <button class="back-link" @click="router.push({ name: 'problems' })">
          &larr; 返回题库
        </button>
      </div>
    </template>

    <LoadingState v-if="loading" />

    <template v-else-if="problem">
      <div v-if="isInactive" class="inactive-notice">
        该题目当前已停用，不会出现在公共题库中。
      </div>

      <div class="detail-card">
        <div class="detail-top">
          <h1 class="detail-title">{{ problem.title }}</h1>
          <div class="detail-actions">
            <button v-if="canEdit" class="edit-btn" @click="goToEdit">编辑</button>
          </div>
        </div>

        <div class="meta-grid">
          <div class="meta-item">
            <span class="meta-label">平台</span>
            <span class="meta-value">
              {{ platformLabels[problem.platform] || problem.platform }}
            </span>
          </div>
          <div class="meta-item" v-if="problem.externalProblemKey">
            <span class="meta-label">题目标识</span>
            <span class="meta-value mono">{{ problem.externalProblemKey }}</span>
          </div>
          <div class="meta-item" v-if="problem.difficulty">
            <span class="meta-label">难度</span>
            <span class="meta-value mono">{{ problem.difficulty }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">创建者</span>
            <span class="meta-value">
              用户 #{{ problem.creatorUserId }}
            </span>
          </div>
          <div class="meta-item" v-if="problem.sourceUrl">
            <span class="meta-label">来源链接</span>
            <a class="meta-value link" :href="problem.sourceUrl" target="_blank" rel="noopener noreferrer">
              {{ problem.sourceUrl }}
            </a>
          </div>
          <div class="meta-item" v-if="problem.tags">
            <span class="meta-label">标签</span>
            <span class="meta-value">{{ problem.tags }}</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">创建时间</span>
            <span class="meta-value">
              {{ new Date(problem.createTime).toLocaleString('zh-CN') }}
            </span>
          </div>
          <div class="meta-item">
            <span class="meta-label">更新时间</span>
            <span class="meta-value">
              {{ new Date(problem.updateTime).toLocaleString('zh-CN') }}
            </span>
          </div>
        </div>
      </div>

      <div class="content-card">
        <h2 class="content-heading">题目描述</h2>
        <MarkdownContent :content="problem.contentMd" />
      </div>
    </template>

    <ErrorState
      v-else-if="error"
      :message="error"
      @retry="fetchDetail"
    />

    <div v-else-if="notFound" class="status-page">
      <h2>题目不存在</h2>
      <p>该题目不存在或你无权查看。</p>
      <button class="status-btn" @click="router.push({ name: 'problems' })">返回题库</button>
    </div>

    <div v-else-if="forbidden" class="status-page">
      <h2>无权访问</h2>
      <p>你没有权限查看该题目。</p>
      <button class="status-btn" @click="router.push({ name: 'problems' })">返回题库</button>
    </div>
  </PageContainer>
</template>

<style scoped>
.detail-header {
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

.inactive-notice {
  padding: 12px 20px;
  background: #fff3e0;
  border: 1px solid #ffcc02;
  border-radius: var(--radius-md);
  color: #e65100;
  font-size: var(--text-body-md);
  font-weight: 500;
  margin-bottom: var(--space-stack-md);
}

.detail-card,
.content-card {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 24px;
  margin-bottom: var(--space-stack-md);
}

.detail-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.detail-title {
  font-family: var(--font-headline);
  font-size: var(--text-headline-md);
  font-weight: 700;
  color: var(--color-on-surface);
  line-height: var(--leading-headline-md);
}

.edit-btn {
  height: 36px;
  padding: 0 18px;
  border: 1px solid var(--color-primary-container);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-primary-container);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s, color 0.2s;
}

.edit-btn:hover {
  background: var(--color-primary-container);
  color: var(--color-on-primary);
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px 24px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-label {
  font-size: var(--text-label-sm);
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--color-on-surface-variant);
}

.meta-value {
  font-size: var(--text-body-md);
  color: var(--color-on-surface);
}

.meta-value.mono {
  font-family: var(--font-mono);
  font-size: var(--text-code-sm);
}

.meta-value.link {
  color: var(--color-primary-container);
  word-break: break-all;
}

.content-heading {
  font-family: var(--font-headline);
  font-size: var(--text-headline-sm);
  font-weight: 600;
  color: var(--color-on-surface);
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-subtle);
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
