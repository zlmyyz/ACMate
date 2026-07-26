<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAdminProblems, adminDeactivateProblem, adminRestoreProblem } from '@/api/problems'
import { actionLabels } from '@/constants/labels'
import type { AdminProblemSummary, MineProblemStatusFilter, AdminProblemQueryParams } from '@/types/problem'
import PageContainer from '@/components/layout/PageContainer.vue'
import ProblemStatusTabs from '@/components/problem/ProblemStatusTabs.vue'
import StatusBadge from '@/components/problem/StatusBadge.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const route = useRoute()
const router = useRouter()

const problems = ref<AdminProblemSummary[]>([])
const loading = ref(true)
const error = ref('')
const currentPage = ref(1)
const total = ref(0)
const totalPages = ref(0)
const statusFilter = ref<MineProblemStatusFilter>('ALL')
const keyword = ref('')
const platform = ref('')
const difficulty = ref('')
const creatorUserId = ref<number | null>(null)

const confirmVisible = ref(false)
const confirmTitle = ref('')
const confirmMessage = ref('')
const deactivateReason = ref('')
const isDeactivateDialog = ref(false)
const confirmAction = ref<(() => Promise<void>) | null>(null)
const confirmLoading = ref(false)
const targetProblemId = ref(0)

const pageSize = 20

function getRowClass(problem: AdminProblemSummary) {
  return { 'row-inactive': problem.status === 'INACTIVE' }
}

function creatorLabel(p: AdminProblemSummary) {
  if (p.creatorNickname) {
    return `${p.creatorNickname} (${p.creatorUsername || '#' + p.creatorUserId})`
  }
  return p.creatorUsername || `用户 #${p.creatorUserId}`
}

function deactivationInfo(p: AdminProblemSummary) {
  if (p.status !== 'INACTIVE' || !p.deactivationSource) return ''
  if (p.deactivationSource === 'ADMIN') {
    const reason = p.deactivationReason || '无'
    return `管理员停用，原因：${reason}`
  }
  return '创建者自行停用'
}

async function fetchProblems() {
  loading.value = true
  error.value = ''
  try {
    const params: AdminProblemQueryParams = {
      page: currentPage.value,
      size: pageSize,
      status: statusFilter.value,
    }
    if (keyword.value) params.keyword = keyword.value
    if (platform.value) params.platform = platform.value
    if (difficulty.value) params.difficulty = difficulty.value
    if (creatorUserId.value) params.creatorUserId = creatorUserId.value
    const result = await getAdminProblems(params)
    problems.value = result.records
    totalPages.value = result.pages
    total.value = result.total
  } catch (e: unknown) {
    const err = e as { response?: { status: number } }
    if (err.response?.status === 401) {
      router.push({ name: 'login', query: { redirect: route.fullPath } })
      return
    }
    if (err.response?.status === 403) {
      router.push({ name: 'forbidden' })
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

function onSearch() {
  currentPage.value = 1
  syncQuery()
  fetchProblems()
}

function syncQuery() {
  const query: Record<string, string> = {}
  if (statusFilter.value !== 'ALL') query.status = statusFilter.value
  if (keyword.value) query.keyword = keyword.value
  if (platform.value) query.platform = platform.value
  if (difficulty.value) query.difficulty = difficulty.value
  if (creatorUserId.value) query.creatorUserId = String(creatorUserId.value)
  if (currentPage.value > 1) query.page = String(currentPage.value)
  router.replace({ query })
}

function goToEdit(id: number) {
  router.push({ name: 'edit-problem', params: { id } })
}

function goToDetail(id: number) {
  router.push({ name: 'problem-detail', params: { id } })
}

function showDeactivateDialog(id: number) {
  targetProblemId.value = id
  deactivateReason.value = ''
  confirmTitle.value = '强制停用题目'
  confirmMessage.value = ''
  isDeactivateDialog.value = true
  confirmAction.value = null
  confirmVisible.value = true
}

function showRestoreConfirm(id: number) {
  targetProblemId.value = id
  confirmTitle.value = '恢复题目'
  confirmMessage.value = '确认要恢复这道题目吗？恢复后该题目将重新出现在公共题库中。'
  isDeactivateDialog.value = false
  confirmAction.value = () => doRestore(id)
  confirmVisible.value = true
}

async function doAdminDeactivate() {
  if (!deactivateReason.value.trim()) {
    error.value = '请填写停用原因'
    return
  }
  confirmLoading.value = true
  error.value = ''
  try {
    await adminDeactivateProblem(targetProblemId.value, deactivateReason.value.trim())
    confirmVisible.value = false
    await fetchProblems()
  } catch (e: unknown) {
    confirmVisible.value = false
    const err = e as { response?: { status: number; data?: { message?: string } } }
    handleActionError(err)
  } finally {
    confirmLoading.value = false
  }
}

async function doRestore(id: number) {
  confirmLoading.value = true
  error.value = ''
  try {
    await adminRestoreProblem(id)
    confirmVisible.value = false
    await fetchProblems()
  } catch (e: unknown) {
    confirmVisible.value = false
    const err = e as { response?: { status: number; data?: { message?: string } } }
    handleActionError(err)
  } finally {
    confirmLoading.value = false
  }
}

function handleActionError(err: { response?: { status: number; data?: { message?: string } } }) {
  if (err.response?.status === 401) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (err.response?.status === 403) {
    error.value = '没有权限执行该操作'
    return
  }
  if (err.response?.status === 404) {
    error.value = '题目不存在'
    fetchProblems()
    return
  }
  error.value = err.response?.data?.message || '操作失败，请稍后重试'
}

function handleConfirm() {
  if (isDeactivateDialog.value) {
    doAdminDeactivate()
  } else if (confirmAction.value) {
    confirmAction.value()
  }
}

onMounted(() => {
  if (route.query.page) currentPage.value = Number(route.query.page) || 1
  if (route.query.status) statusFilter.value = route.query.status as MineProblemStatusFilter
  if (route.query.keyword) keyword.value = route.query.keyword as string
  if (route.query.platform) platform.value = route.query.platform as string
  if (route.query.difficulty) difficulty.value = route.query.difficulty as string
  if (route.query.creatorUserId) creatorUserId.value = Number(route.query.creatorUserId)
  fetchProblems()
})
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">全部题库</h1>
          <p class="page-subtitle">管理平台所有题目，包括正常和已停用的题目。</p>
        </div>
      </div>
    </template>

    <div class="toolbar">
      <div class="toolbar-row">
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
        <select v-model="platform" class="filter-select" @change="onSearch">
          <option value="">全部平台</option>
          <option value="CODEFORCES">Codeforces</option>
          <option value="NOWCODER">牛客</option>
          <option value="CUSTOM">自定义</option>
          <option value="OTHER">其他</option>
        </select>
        <input
          v-model="difficulty"
          type="text"
          class="filter-input difficulty-input"
          placeholder="难度"
          @keyup.enter="onSearch"
        />
        <input
          v-model="creatorUserId"
          type="number"
          class="filter-input creator-input"
          placeholder="创建者 ID"
          @keyup.enter="onSearch"
        />
        <button class="search-btn" @click="onSearch">搜索</button>
      </div>
    </div>

    <LoadingState v-if="loading && problems.length === 0" />

    <ErrorState
      v-else-if="error && problems.length === 0"
      :message="error"
      @retry="fetchProblems"
    />

    <template v-else>
      <div v-if="error" class="inline-error">{{ error }}</div>

      <div class="table-wrapper">
        <table class="problem-table" v-if="problems.length > 0 || loading">
          <thead>
            <tr>
              <th class="col-id">#</th>
              <th class="col-title">标题</th>
              <th class="col-platform">平台</th>
              <th class="col-difficulty">难度</th>
              <th class="col-creator">创建者</th>
              <th class="col-status">状态</th>
              <th class="col-time">创建时间</th>
              <th class="col-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="8" class="loading-cell">加载中...</td>
            </tr>
            <tr
              v-for="problem in problems"
              :key="problem.id"
              class="problem-row"
              :class="getRowClass(problem)"
              @click="goToDetail(problem.id)"
            >
              <td class="col-id">
                <span class="problem-id">{{ problem.id }}</span>
              </td>
              <td class="col-title">
                <div class="title-cell">
                  <span class="problem-title">{{ problem.title }}</span>
                  <span v-if="problem.externalProblemKey" class="problem-key">
                    {{ problem.externalProblemKey }}
                  </span>
                  <span v-if="problem.deactivationSource" class="deactivation-info">
                    {{ deactivationInfo(problem) }}
                  </span>
                </div>
              </td>
              <td class="col-platform">
                <span class="platform-text">{{ problem.platform }}</span>
              </td>
              <td class="col-difficulty">
                <span v-if="problem.difficulty" class="difficulty-text">{{ problem.difficulty }}</span>
                <span v-else class="no-value">—</span>
              </td>
              <td class="col-creator" @click.stop>
                <span class="creator-text">{{ creatorLabel(problem) }}</span>
              </td>
              <td class="col-status" @click.stop>
                <StatusBadge :status="problem.status" />
              </td>
              <td class="col-time">
                {{ new Date(problem.createTime).toLocaleDateString('zh-CN') }}
              </td>
              <td class="col-actions" @click.stop>
                <div class="action-btns">
                  <button class="action-btn edit-action" @click="goToEdit(problem.id)">
                    {{ actionLabels.edit }}
                  </button>
                  <button
                    v-if="problem.status === 'ACTIVE'"
                    class="action-btn deactivate-action"
                    @click="showDeactivateDialog(problem.id)"
                  >
                    {{ actionLabels.deactivate }}
                  </button>
                  <button
                    v-else
                    class="action-btn restore-action"
                    @click="showRestoreConfirm(problem.id)"
                  >
                    {{ actionLabels.restore }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <PaginationBar
        v-if="totalPages > 1"
        :page="currentPage"
        :total="total"
        :size="pageSize"
        @change="onPageChange"
      />
    </template>

    <ConfirmDialog
      :visible="confirmVisible && !isDeactivateDialog"
      :title="confirmTitle"
      :message="confirmMessage"
      :loading="confirmLoading"
      @confirm="handleConfirm"
      @cancel="confirmVisible = false"
    />

    <Teleport to="body">
      <div v-if="confirmVisible && isDeactivateDialog" class="overlay" @click.self="confirmVisible = false">
        <div class="dialog">
          <h3 class="dialog-title">强制停用题目</h3>
          <div class="dialog-body">
            <label class="reason-label">停用原因（必填）：</label>
            <textarea
              v-model="deactivateReason"
              class="reason-input"
              maxlength="500"
              placeholder="请输入停用该题目的原因..."
              rows="3"
            ></textarea>
            <span class="reason-count">{{ deactivateReason.length }}/500</span>
          </div>
          <div class="dialog-actions">
            <button class="dialog-btn cancel-btn" :disabled="confirmLoading" @click="confirmVisible = false">
              {{ actionLabels.cancel }}
            </button>
            <button class="dialog-btn confirm-btn" :disabled="confirmLoading" @click="doAdminDeactivate">
              {{ confirmLoading ? '处理中...' : '确认停用' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
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

.toolbar {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  margin-bottom: var(--space-stack-md);
}

.toolbar-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-input-wrap {
  flex: 1;
  min-width: 160px;
  max-width: 280px;
}

.search-input,
.filter-input {
  height: 38px;
  padding: 0 12px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
  outline: none;
  width: 100%;
}

.search-input:focus,
.filter-input:focus {
  border-color: var(--color-primary-container);
}

.filter-select {
  height: 38px;
  padding: 0 10px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
  outline: none;
  cursor: pointer;
}

.difficulty-input {
  width: 90px;
}

.creator-input {
  width: 120px;
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
}

.inline-error {
  padding: 10px 16px;
  background: #fce4ec;
  color: #c62828;
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  margin-bottom: var(--space-stack-md);
}

.table-wrapper {
  overflow-x: auto;
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
}

.problem-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 900px;
}

.problem-table th {
  padding: 12px 16px;
  font-size: var(--text-label-sm);
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--color-on-surface-variant);
  text-align: left;
  border-bottom: 1px solid var(--color-border-subtle);
  background: var(--color-surface-container-lowest);
  white-space: nowrap;
}

.problem-table td {
  padding: 12px 16px;
  font-size: var(--text-body-md);
  border-bottom: 1px solid var(--color-border-subtle);
  vertical-align: middle;
}

.problem-row {
  cursor: pointer;
  transition: background 0.15s;
}

.problem-row:hover {
  background: var(--color-surface-container-low);
}

.col-id { width: 60px; }
.col-platform { width: 100px; }
.col-difficulty { width: 90px; text-align: center; }
.col-creator { width: 140px; }
.col-status { width: 100px; }
.col-time { width: 110px; white-space: nowrap; }
.col-actions { width: 160px; }

.problem-id {
  font-family: var(--font-mono);
  font-size: var(--text-code-sm);
  color: var(--color-secondary);
}

.title-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.problem-title {
  color: var(--color-primary);
  font-weight: 500;
}

.problem-row:hover .problem-title {
  text-decoration: underline;
}

.problem-key {
  font-family: var(--font-mono);
  font-size: var(--text-label-sm);
  color: var(--color-on-surface-variant);
}

.deactivation-info {
  font-size: var(--text-label-sm);
  color: #e65100;
  margin-top: 2px;
}

.platform-text {
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-on-surface-variant);
  background: var(--color-surface-container);
  padding: 2px 10px;
  border-radius: 999px;
}

.difficulty-text {
  font-family: var(--font-mono);
  font-size: var(--text-code-sm);
  color: var(--color-tertiary);
}

.no-value {
  color: var(--color-on-surface-variant);
}

.creator-text {
  color: var(--color-on-surface);
  font-size: var(--text-body-md);
}

.action-btns {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 4px 14px;
  border-radius: var(--radius-md);
  font-size: var(--text-label-sm);
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
  border: 1px solid var(--color-border-subtle);
  background: transparent;
}

.action-btn:hover {
  opacity: 0.85;
}

.edit-action {
  color: var(--color-primary-container);
  border-color: var(--color-primary-container);
}

.deactivate-action {
  color: #e65100;
  border-color: #e65100;
}

.restore-action {
  color: var(--color-primary-container);
  border-color: var(--color-primary-container);
}

.loading-cell {
  text-align: center;
  padding: 48px 16px;
  color: var(--color-on-surface-variant);
}

.row-inactive .problem-title {
  text-decoration: line-through;
  opacity: 0.7;
  color: var(--color-on-surface-variant);
}

.overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
}

.dialog {
  background: var(--color-surface-card);
  border-radius: var(--radius-lg);
  padding: 24px;
  min-width: 420px;
  max-width: 520px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
}

.dialog-title {
  font-family: var(--font-headline);
  font-size: var(--text-headline-sm);
  font-weight: 600;
  color: var(--color-on-surface);
  margin-bottom: 16px;
}

.dialog-body {
  margin-bottom: 20px;
}

.reason-label {
  display: block;
  font-size: var(--text-body-md);
  font-weight: 500;
  color: var(--color-on-surface);
  margin-bottom: 8px;
}

.reason-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
  outline: none;
  resize: vertical;
  font-family: inherit;
}

.reason-input:focus {
  border-color: var(--color-primary-container);
}

.reason-count {
  display: block;
  text-align: right;
  font-size: var(--text-label-sm);
  color: var(--color-on-surface-variant);
  margin-top: 4px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-btn {
  padding: 8px 22px;
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.15s;
  border: none;
}

.dialog-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.cancel-btn {
  background: var(--color-surface-container);
  color: var(--color-on-surface-variant);
  border: 1px solid var(--color-border-subtle);
}

.confirm-btn {
  background: var(--color-primary-container);
  color: var(--color-on-primary);
}
</style>
