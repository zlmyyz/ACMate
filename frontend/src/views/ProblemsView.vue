<script setup lang="ts">
import { ref, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProblems } from '@/api/problems'
import type { ProblemSummary, ProblemQueryParams } from '@/types/problem'
import PageContainer from '@/components/layout/PageContainer.vue'
import ProblemFilters from '@/components/problem/ProblemFilters.vue'
import ProblemTable from '@/components/problem/ProblemTable.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorState from '@/components/common/ErrorState.vue'

const route = useRoute()
const router = useRouter()

const problems = ref<ProblemSummary[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)

const filters = computed<ProblemQueryParams>(() => ({
  page: page.value,
  size: size.value,
  keyword: (route.query.keyword as string) || undefined,
  platform: (route.query.platform as string) || undefined,
  difficulty: (route.query.difficulty as string) || undefined,
}))

function syncFromQuery() {
  page.value = Number(route.query.page) || 1
}

async function fetchProblems() {
  loading.value = true
  error.value = ''
  try {
    const result = await getProblems(filters.value)
    problems.value = result.records
    total.value = result.total
  } catch (e: unknown) {
    const err = e as { response?: { status: number } }
    if (err.response?.status === 401) {
      router.push({ name: 'login', query: { redirect: route.fullPath } })
      return
    }
    error.value = '加载题目列表失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 1
  fetchProblems()
}

function onPageChange(p: number) {
  page.value = p
  router.push({ query: { ...route.query, page: String(p) } })
}

function goToCreate() {
  router.push({ name: 'create-problem' })
}

watch(
  () => route.query,
  () => {
    syncFromQuery()
    fetchProblems()
  },
)

onMounted(() => {
  syncFromQuery()
  fetchProblems()
})
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="page-header">
        <h1 class="page-title">题库</h1>
        <button class="create-btn" @click="goToCreate">创建题目</button>
      </div>
    </template>

    <div class="problems-page">
      <ProblemFilters
        v-model="filters"
        @search="onSearch"
      />

      <div class="problems-card">
        <ProblemTable :problems="problems" :loading="loading" />
        <EmptyState
          v-if="!loading && !error && problems.length === 0"
          message="没有找到符合当前条件的题目。"
        />
        <ErrorState
          v-if="error"
          :message="error"
          @retry="fetchProblems"
        />
      </div>

      <PaginationBar
        v-if="!error && total > 0"
        :page="page"
        :total="total"
        :size="size"
        @change="onPageChange"
      />
    </div>
  </PageContainer>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  font-family: var(--font-headline);
  font-size: var(--text-display-lg);
  font-weight: 700;
  line-height: var(--leading-display-lg);
  color: var(--color-on-surface);
}

.create-btn {
  height: 40px;
  padding: 0 20px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-primary-container);
  color: var(--color-on-primary);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.create-btn:hover {
  opacity: 0.9;
}

.problems-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-stack-md);
}

.problems-card {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
</style>
