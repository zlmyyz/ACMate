<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMyProblems, deactivateProblem, restoreProblem } from '@/api/problems'
import { actionLabels } from '@/constants/labels'
import type { MyProblemSummary, MineProblemStatusFilter, MyProblemQueryParams } from '@/types/problem'
import PageContainer from '@/components/layout/PageContainer.vue'
import ProblemStatusTabs from '@/components/problem/ProblemStatusTabs.vue'
import ProblemTable from '@/components/problem/ProblemTable.vue'
import ProblemActionButtons from '@/components/problem/ProblemActionButtons.vue'
import StatusBadge from '@/components/problem/StatusBadge.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const route = useRoute()
const router = useRouter()

const problems = ref<MyProblemSummary[]>([])
const loading = ref(true)
const error = ref('')
const currentPage = ref(1)
const totalPages = ref(0)
const total = ref(0)
const statusFilter = ref<MineProblemStatusFilter>((route.query.status as MineProblemStatusFilter) || 'ALL')
const keyword = ref((route.query.keyword as string) || '')

const confirmVisible = ref(false)
const confirmTitle = ref('')
const confirmMessage = ref('')
const confirmAction = ref<(() => Promise<void>) | null>(null)
const confirmLoading = ref(false)

const pageSize = 20

function getRowClass(problem: MyProblemSummary | import('@/types/problem').ProblemSummary) {
  const p = problem as MyProblemSummary
  return { 'row-inactive': p.status === 'INACTIVE' }
}

async function fetchProblems() {
  loading.value = true
  error.value = ''
  try {
    const params: MyProblemQueryParams = {
      page: currentPage.value,
      size: pageSize,
      status: statusFilter.value,
    }
    if (keyword.value) params.keyword = keyword.value
    const result = await getMyProblems(params)
    problems.value = result.records
    totalPages.value = result.pages
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

function onStatusChange(value: MineProblemStatusFilter) {
  statusFilter.value = value
  currentPage.value = 1
  syncQuery()
  fetchProblems()
}

function onPageChange(page: number) {
  currentPage.value = page
  syncQuery()
  fetchProblems()
}

function syncQuery() {
  const query: Record<string, string> = {}
  if (statusFilter.value !== 'ALL') query.status = statusFilter.value
  if (keyword.value) query.keyword = keyword.value
  if (currentPage.value > 1) query.page = String(currentPage.value)
  router.replace({ query })
}

function goToEdit(id: number) {
  router.push({ name: 'edit-problem', params: { id } })
}

function goToCreate() {
  router.push({ name: 'create-problem' })
}

function showDeactivateConfirm(id: number) {
  confirmTitle.value = '停用题目'
  confirmMessage.value = '确认要停用这道题目吗？停用后该题目将不会出现在公共题库中，但你仍然可以查看和恢复它。'
  confirmAction.value = () => doDeactivate(id)
  confirmVisible.value = true
}

function showRestoreConfirm(id: number) {
  confirmTitle.value = '恢复题目'
  confirmMessage.value = '确认要恢复这道题目吗？恢复后该题目将重新出现在公共题库中。'
  confirmAction.value = () => doRestore(id)
  confirmVisible.value = true
}

async function doDeactivate(id: number) {
  confirmLoading.value = true
  try {
    await deactivateProblem(id)
    confirmVisible.value = false
    await fetchProblems()
  } catch (e: unknown) {
    confirmVisible.value = false
    const err = e as { response?: { status: number; data?: { message?: string } } }
    if (err.response?.status === 401) {
      router.push({ name: 'login', query: { redirect: route.fullPath } })
      return
    }
    if (err.response?.status === 403) {
      error.value = '没有权限操作该题目'
      return
    }
    if (err.response?.status === 404) {
      error.value = '题目不存在或无权操作'
      await fetchProblems()
      return
    }
    error.value = err.response?.data?.message || '操作失败，请稍后重试'
  } finally {
    confirmLoading.value = false
  }
}

async function doRestore(id: number) {
  confirmLoading.value = true
  try {
    await restoreProblem(id)
    confirmVisible.value = false
    await fetchProblems()
  } catch (e: unknown) {
    confirmVisible.value = false
    const err = e as { response?: { status: number; data?: { message?: string } } }
    if (err.response?.status === 401) {
      router.push({ name: 'login', query: { redirect: route.fullPath } })
      return
    }
    if (err.response?.status === 403) {
      error.value = '没有权限操作该题目'
      return
    }
    if (err.response?.status === 404) {
      error.value = '题目不存在或无权操作'
      await fetchProblems()
      return
    }
    error.value = err.response?.data?.message || '操作失败，请稍后重试'
  } finally {
    confirmLoading.value = false
  }
}

function onSearch() {
  currentPage.value = 1
  syncQuery()
  fetchProblems()
}

onMounted(() => {
  if (route.query.page) currentPage.value = Number(route.query.page) || 1
  fetchProblems()
})
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">我的题目</h1>
          <p class="page-subtitle">管理和追踪你创建的编程挑战题目。</p>
        </div>
        <button class="create-btn" @click="goToCreate">
          + {{ actionLabels.create }}
        </button>
      </div>
    </template>

    <div class="toolbar">
      <div class="toolbar-left">
        <ProblemStatusTabs v-model="statusFilter" @update:model-value="onStatusChange" />
        <div class="search-input-wrap">
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索标题或题目 ID..."
            class="search-input"
            @keyup.enter="onSearch"
          />
        </div>
        <button class="search-btn" @click="onSearch">搜索</button>
      </div>
    </div>

    <LoadingState v-if="loading && problems.length === 0" />

    <ErrorState
      v-else-if="error"
      :message="error"
      @retry="fetchProblems"
    />

    <template v-else>
      <ProblemTable
        :problems="problems"
        :loading="loading"
        :get-row-class="getRowClass"
      >
        <template #status="{ problem }">
          <StatusBadge :status="(problem as MyProblemSummary).status" />
        </template>
        <template #actions="{ problem }">
          <ProblemActionButtons
            :problem-id="problem.id"
            :status="(problem as MyProblemSummary).status"
            @edit="goToEdit"
            @deactivate="showDeactivateConfirm"
            @restore="showRestoreConfirm"
          />
        </template>
      </ProblemTable>

      <PaginationBar
        v-if="totalPages > 1"
        :page="currentPage"
        :total="total"
        :size="pageSize"
        @change="onPageChange"
      />
    </template>

    <ConfirmDialog
      :visible="confirmVisible"
      :title="confirmTitle"
      :message="confirmMessage"
      :loading="confirmLoading"
      @confirm="confirmAction?.()"
      @cancel="confirmVisible = false"
    />
  </PageContainer>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.header-left {
  flex: 1;
}

.page-title {
  font-family: var(--font-headline);
  font-size: var(--text-display-lg);
  font-weight: 700;
  color: var(--color-on-surface);
  line-height: var(--leading-display-lg);
}

.page-subtitle {
  margin-top: 4px;
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
}

.create-btn {
  height: 40px;
  padding: 0 22px;
  background: var(--color-primary-container);
  color: var(--color-on-primary);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.2s;
}

.create-btn:hover {
  opacity: 0.9;
}

.toolbar {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  margin-bottom: var(--space-stack-md);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input-wrap {
  flex: 1;
  min-width: 180px;
  max-width: 320px;
}

.search-input {
  width: 100%;
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
  outline: none;
  transition: border-color 0.2s;
}

.search-input:focus {
  border-color: var(--color-primary-container);
}

.search-btn {
  height: 38px;
  padding: 0 18px;
  background: var(--color-primary-container);
  color: var(--color-on-primary);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.search-btn:hover {
  opacity: 0.9;
}
</style>
