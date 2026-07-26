<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { getProblemDetail, updateProblem } from '@/api/problems'
import { useAuthStore } from '@/stores/auth'
import type { ProblemDetail, UpdateProblemRequest } from '@/types/problem'
import PageContainer from '@/components/layout/PageContainer.vue'
import ProblemForm from '@/components/problem/ProblemForm.vue'
import LoadingState from '@/components/common/LoadingState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const problem = ref<ProblemDetail | null>(null)
const loading = ref(true)
const submitting = ref(false)
const errorMsg = ref('')
const pageError = ref('')
const formRef = ref<InstanceType<typeof ProblemForm> | null>(null)

const problemId = computed(() => Number(route.params.id))
const DRAFT_KEY = computed(() => `acmate:edit-problem:${problemId.value}:draft`)

async function loadProblem() {
  loading.value = true
  pageError.value = ''
  try {
    problem.value = await getProblemDetail(problemId.value)
    if (!auth.isAdmin && auth.user && auth.user.id !== problem.value.creatorUserId) {
      pageError.value = 'forbidden'
    }
  } catch (e: unknown) {
    const err = e as { response?: { status: number } }
    if (err.response?.status === 401) {
      router.push({ name: 'login', query: { redirect: route.fullPath } })
      return
    }
    if (err.response?.status === 404) {
      pageError.value = 'notfound'
      return
    }
    pageError.value = 'error'
  } finally {
    loading.value = false
  }
}

async function handleSubmit(data: UpdateProblemRequest) {
  errorMsg.value = ''
  submitting.value = true
  try {
    const result = await updateProblem(problemId.value, data)
    localStorage.removeItem(DRAFT_KEY.value)
    formRef.value?.clearDraft()
    router.push({ name: 'problem-detail', params: { id: result.id } })
  } catch (e: unknown) {
    const err = e as { response?: { status: number; data?: { message?: string } } }
    if (err.response?.status === 401) {
      router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      return
    }
    if (err.response?.status === 409) {
      errorMsg.value = '该平台题目标识已被其他题目使用'
      return
    }
    if (err.response?.status === 400) {
      errorMsg.value = err.response?.data?.message || '请检查输入字段'
      return
    }
    if (err.response?.status === 403) {
      errorMsg.value = '无权修改该题目'
      return
    }
    errorMsg.value = '更新失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.push({ name: 'problem-detail', params: { id: problemId.value } })
}

onBeforeRouteLeave((_to, _from, next) => {
  if (formRef.value?.isDirty) {
    const leave = window.confirm('你有未保存的修改，确定离开吗？')
    if (!leave) {
      next(false)
      return
    }
  }
  next()
})

onMounted(loadProblem)
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="edit-header">
        <button class="back-link" @click="handleCancel">&larr; 返回详情</button>
        <h1 class="page-title">编辑题目</h1>
      </div>
    </template>

    <LoadingState v-if="loading" />

    <template v-else-if="problem && !pageError">
      <div class="form-card">
        <ProblemForm
          ref="formRef"
          :initial-data="problem"
          :draft-key="DRAFT_KEY"
          :submitting="submitting"
          :error-msg="errorMsg"
          @submit="handleSubmit"
          @cancel="handleCancel"
        />
      </div>
    </template>

    <div v-else-if="pageError === 'forbidden'" class="status-page">
      <h2>无权访问</h2>
      <p>你没有权限编辑该题目。</p>
      <button class="status-btn" @click="router.push({ name: 'problems' })">返回题库</button>
    </div>

    <div v-else-if="pageError === 'notfound'" class="status-page">
      <h2>题目不存在</h2>
      <p>该题目不存在或你无权查看。</p>
      <button class="status-btn" @click="router.push({ name: 'problems' })">返回题库</button>
    </div>

    <div v-else-if="pageError === 'error'" class="status-page">
      <h2>加载失败</h2>
      <p>加载题目信息失败。</p>
      <button class="status-btn" @click="loadProblem">重试</button>
    </div>
  </PageContainer>
</template>

<style scoped>
.edit-header {
  display: flex;
  align-items: center;
  gap: 16px;
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

.page-title {
  font-family: var(--font-headline);
  font-size: var(--text-display-lg);
  font-weight: 700;
  line-height: var(--leading-display-lg);
  color: var(--color-on-surface);
}

.form-card {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 24px;
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
