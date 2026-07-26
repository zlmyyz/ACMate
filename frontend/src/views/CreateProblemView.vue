<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { createProblem } from '@/api/problems'
import type { CreateProblemRequest } from '@/types/problem'
import PageContainer from '@/components/layout/PageContainer.vue'
import ProblemForm from '@/components/problem/ProblemForm.vue'

const router = useRouter()
const submitting = ref(false)
const errorMsg = ref('')

const DRAFT_KEY = 'acmate:create-problem:draft'

async function handleSubmit(data: CreateProblemRequest) {
  errorMsg.value = ''
  submitting.value = true
  try {
    const result = await createProblem(data)
    localStorage.removeItem(DRAFT_KEY)
    router.push({ name: 'problem-detail', params: { id: result.id } })
  } catch (e: unknown) {
    const err = e as { response?: { status: number; data?: { message?: string } } }
    if (err.response?.status === 401) {
      router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      return
    }
    if (err.response?.status === 409) {
      errorMsg.value = '该平台题目标识已存在'
      return
    }
    if (err.response?.status === 400) {
      errorMsg.value = err.response?.data?.message || '请检查输入字段'
      return
    }
    errorMsg.value = '创建失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.push({ name: 'problems' })
}
</script>

<template>
  <PageContainer>
    <template #header>
      <h1 class="page-title">创建题目</h1>
    </template>

    <div class="form-card">
      <ProblemForm
        :draft-key="DRAFT_KEY"
        :submitting="submitting"
        :error-msg="errorMsg"
        @submit="handleSubmit"
        @cancel="handleCancel"
      />
    </div>
  </PageContainer>
</template>

<style scoped>
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
</style>
