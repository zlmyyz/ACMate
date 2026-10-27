<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProblemDetail, deactivateProblem, restoreProblem, adminDeactivateProblem, adminRestoreProblem } from '@/api/problems'
import { useAuthStore } from '@/stores/auth'
import type { ProblemDetail } from '@/types/problem'
import { platformLabels } from '@/constants/labels'
import { extractDataConstraintDetails } from '@/utils/problem-constraints'
import PageContainer from '@/components/layout/PageContainer.vue'
import MarkdownContent from '@/components/common/MarkdownContent.vue'
import ConstraintFormula from '@/components/common/ConstraintFormula.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import NowcoderPracticePanel from '@/components/problem/NowcoderPracticePanel.vue'
import ProblemReviews from '@/components/problem/ProblemReviews.vue'
import ErrorState from '@/components/common/ErrorState.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const problem = ref<ProblemDetail | null>(null)
const loading = ref(true)
const error = ref('')
const notFound = ref(false)
const forbidden = ref(false)
const showDeactivateDialog = ref(false)
const deactivateReason = ref('')
const actionLoading = ref(false)
const actionError = ref('')

const problemId = computed(() => Number(route.params.id))
const isTrainingWorkspace = computed(() => problem.value?.platform === 'NOWCODER' && problem.value.active)
const dataConstraints = computed(() => extractDataConstraintDetails(problem.value?.contentMd ?? null))

const isCreator = computed(() => {
  if (!problem.value || !auth.user) return false
  return auth.user.id === problem.value.creatorUserId
})

const canEdit = computed(() => {
  if (!problem.value || !auth.user) return false
  return auth.isAdmin || (isCreator.value && problem.value.platform === 'CUSTOM')
})

const canDeactivate = computed(() => {
  if (!problem.value || !auth.user) return false
  return (auth.isAdmin || (isCreator.value && problem.value.platform === 'CUSTOM')) && problem.value.active
})

const canRestore = computed(() => {
  if (!problem.value || !auth.user) return false
  if (problem.value.active) return false
  if (isCreator.value && problem.value.deactivationSource === 'ADMIN') return false
  return auth.isAdmin || (isCreator.value && problem.value.platform === 'CUSTOM')
})

const isInactive = computed(() => {
  return problem.value ? !problem.value.active : false
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

async function handleDeactivate() {
  actionError.value = ''
  actionLoading.value = true
  try {
    if (isCreator.value) {
      await deactivateProblem(problemId.value)
    } else {
      if (!deactivateReason.value.trim()) {
        actionError.value = '请填写停用原因'
        return
      }
      await adminDeactivateProblem(problemId.value, deactivateReason.value.trim())
    }
    showDeactivateDialog.value = false
    deactivateReason.value = ''
    await fetchDetail()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    actionError.value = err.response?.data?.message ?? '操作失败'
  } finally {
    actionLoading.value = false
  }
}

async function handleRestore() {
  actionError.value = ''
  actionLoading.value = true
  try {
    if (isCreator.value && !auth.isAdmin) {
      await restoreProblem(problemId.value)
    } else {
      await adminRestoreProblem(problemId.value)
    }
    await fetchDetail()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    actionError.value = err.response?.data?.message ?? '操作失败'
  } finally {
    actionLoading.value = false
  }
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

      <div class="problem-workspace" :class="{ 'with-editor': isTrainingWorkspace }">
      <div class="statement-pane">
      <div class="detail-card">
        <div class="detail-top">
          <h1 class="detail-title">{{ problem.title }}</h1>
          <div class="detail-actions">
            <template v-if="canDeactivate && isCreator">
              <button class="deactivate-btn" @click="showDeactivateDialog = true">停用</button>
            </template>
            <template v-else-if="canDeactivate">
              <button class="deactivate-btn admin" @click="showDeactivateDialog = true">强制停用</button>
            </template>
            <button v-if="canRestore" class="restore-btn" @click="handleRestore" :disabled="actionLoading">恢复</button>
            <button v-if="canEdit" class="edit-btn" @click="goToEdit">编辑</button>
            <button v-else-if="problem.active && problem.platform !== 'CUSTOM'" class="edit-btn" @click="goToEdit">提出修改建议</button>
          </div>
        </div>
        <p v-if="route.query.review === 'submitted'" role="status">修改建议已提交，等待管理员审核。</p>

        <div v-if="showDeactivateDialog" class="deactivate-dialog">
          <template v-if="isCreator">
            <p>确定要停用该题目吗？停用后该题目将不会出现在公共题库中。</p>
          </template>
          <template v-else>
            <p>管理员强制停用，请输入原因：</p>
            <input v-model="deactivateReason" class="deactivate-reason" placeholder="停用原因（必填）" maxlength="500" />
          </template>
          <div class="dialog-actions">
            <p v-if="actionError" class="form-error">{{ actionError }}</p>
            <button class="cancel-btn" @click="showDeactivateDialog = false; deactivateReason = ''; actionError = ''">取消</button>
            <button
              class="confirm-btn"
              :disabled="actionLoading || (!isCreator && !deactivateReason.trim())"
              @click="handleDeactivate"
            >
              {{ actionLoading ? '处理中...' : '确认停用' }}
            </button>
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
              <RouterLink :to="`/users/${problem.creatorUserId}`" class="meta-value link">
                {{ problem.creatorNickname || problem.creatorUsername || `用户 #${problem.creatorUserId}` }}
              </RouterLink>
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
        <h2 class="content-heading">题目内容</h2>
        <aside v-if="dataConstraints.length" class="data-constraints" aria-label="数据范围提示">
          <div class="constraints-heading"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 10v7M12 7v.5"/></svg><strong>数据范围提示</strong></div>
          <ul class="constraints-list"><li v-for="range in dataConstraints" :key="range.text"><ConstraintFormula :range="range" /></li></ul>
        </aside>
        <MarkdownContent class="problem-statement" :content="problem.contentMd" />
      </div>
      </div>
      <NowcoderPracticePanel v-if="isTrainingWorkspace" class="editor-pane" :key="problem.id" :problem-id="problem.id" />
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
    <ProblemReviews v-if="problem && problem.platform !== 'CUSTOM'" :problem-id="problemId" />
  </PageContainer>
</template>

<style scoped>
.problem-workspace{display:grid;grid-template-columns:minmax(0,1fr);gap:20px;align-items:start}
.problem-workspace.with-editor{grid-template-columns:minmax(0,.85fr) minmax(0,1.15fr)}
.statement-pane,.editor-pane{min-width:0}.editor-pane{margin-top:0;position:sticky;top:calc(var(--header-height) + 20px);max-height:calc(100dvh - var(--header-height) - 40px);overflow-y:auto}
.with-editor .detail-top{flex-wrap:wrap;gap:12px}.with-editor .detail-title{font-size:24px}.with-editor .meta-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.with-editor .meta-value.link{overflow-wrap:anywhere}
.data-constraints{margin:0 0 24px;padding:16px 18px;border-left:3px solid var(--color-primary);border-radius:8px;background:var(--color-surface-container-low)}
.constraints-heading{display:flex;align-items:center;gap:8px;color:var(--color-primary);font-size:14px;margin-bottom:10px}.constraints-heading svg{width:19px;height:19px;fill:none;stroke:currentColor;stroke-width:1.8;stroke-linecap:round}.constraints-list{display:flex;flex-wrap:wrap;gap:8px;margin:0;padding:0;list-style:none}.constraints-list li{min-width:0;max-width:100%;padding:5px 9px;border:1px solid var(--color-border-subtle);border-radius:5px;background:var(--color-surface-card);color:var(--color-on-surface);overflow-x:auto}.constraints-list code{font:13px/1.55 var(--font-mono),monospace;overflow-wrap:anywhere}.constraints-list :deep(.katex){font-size:1em}
.problem-statement.markdown-content{font-size:15px;line-height:1.9;letter-spacing:.01em}.problem-statement.markdown-content :deep(h1),.problem-statement.markdown-content :deep(h2),.problem-statement.markdown-content :deep(h3),.problem-statement.markdown-content :deep(h4){font-family:var(--font-body);font-weight:650;line-height:1.45;letter-spacing:0;margin:1.6em 0 .6em}.problem-statement.markdown-content :deep(h1){font-size:1.32em}.problem-statement.markdown-content :deep(h2){font-size:1.2em}.problem-statement.markdown-content :deep(h3),.problem-statement.markdown-content :deep(h4){font-size:1.08em}.problem-statement.markdown-content :deep(p){margin:.8em 0}.problem-statement.markdown-content :deep(ul),.problem-statement.markdown-content :deep(ol){margin:.7em 0;padding-left:1.6em}.problem-statement.markdown-content :deep(li){margin:.3em 0}.problem-statement.markdown-content :deep(pre){margin:1em 0;padding:14px 16px;line-height:1.65}.problem-statement.markdown-content :deep(p img),.problem-statement.markdown-content :deep(li img){vertical-align:middle;border-radius:0}.problem-statement.markdown-content :deep(h1:first-child),.problem-statement.markdown-content :deep(h2:first-child),.problem-statement.markdown-content :deep(h3:first-child){margin-top:0}
@media(min-width:1400px){.problem-workspace.with-editor{grid-template-columns:minmax(0,.85fr) minmax(0,1.15fr)}}
@media(max-width:1000px){.problem-workspace.with-editor{grid-template-columns:minmax(0,1fr)}.editor-pane{position:static;max-height:none;overflow:visible}}

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

.detail-actions { display: flex; gap: 8px; flex-shrink: 0; flex-wrap: wrap; align-items: center; }

.deactivate-btn, .restore-btn {
  height: 36px; padding: 0 14px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); background: transparent;
  font-size: var(--text-body-sm); font-weight: 500; cursor: pointer;
}
.deactivate-btn:hover { border-color: var(--color-status-pending); color: var(--color-status-pending); }
.deactivate-btn.admin:hover { border-color: var(--color-status-critical); color: var(--color-status-critical); }
.restore-btn:hover { border-color: var(--color-status-success); color: var(--color-status-success); }

.deactivate-dialog {
  margin-top: -8px; margin-bottom: 16px; padding: 16px;
  background: var(--color-surface-container-lowest);
  border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md);
}
.deactivate-dialog p { font-size: var(--text-body-md); margin-bottom: 8px; }
.deactivate-reason {
  width: 100%; padding: 8px 12px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); font-size: var(--text-body-md); margin-bottom: 8px;
}
.deactivate-reason:focus { outline: none; border-color: var(--color-primary-container); }
.dialog-actions { display: flex; gap: 8px; align-items: center; justify-content: flex-end; }
.form-error { color: var(--color-status-critical); font-size: var(--text-body-sm); margin-right: auto; }
.cancel-btn {
  height: 32px; padding: 0 14px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); background: transparent; font-size: var(--text-body-sm); cursor: pointer;
}
.confirm-btn {
  height: 32px; padding: 0 16px; border: none; border-radius: var(--radius-md);
  background: var(--color-status-critical); color: #fff; font-size: var(--text-body-sm); font-weight: 600; cursor: pointer;
}
.confirm-btn:hover:not(:disabled) { opacity: 0.9; }
.confirm-btn:disabled { opacity: 0.4; cursor: not-allowed; }

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
