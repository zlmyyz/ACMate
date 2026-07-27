<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { createPost } from '@/api/discussion'
import type { PostType } from '@/types/discussion'
import { postTypeLabels } from '@/constants/labels'
import PageContainer from '@/components/layout/PageContainer.vue'

const router = useRouter()
const auth = useAuthStore()
const title = ref('')
const contentMd = ref('')
const postType = ref<PostType>('OTHER')
const problemId = ref('')
const saving = ref(false)
const error = ref('')

const types: PostType[] = ['SOLUTION', 'QUESTION', 'CONTEST_SUMMARY', 'TRAINING_EXPERIENCE', 'ANNOUNCEMENT', 'OTHER']

const canSelect = (t: PostType) => t !== 'ANNOUNCEMENT' || auth.isAdmin
const isSolution = () => postType.value === 'SOLUTION'

async function handleCreate() {
  if (!title.value.trim() || !contentMd.value.trim()) return
  if (isSolution() && !problemId.value.trim()) { error.value = '题解必须关联题目'; return }
  error.value = ''; saving.value = true
  try {
    const r = await createPost({
      title: title.value.trim(),
      contentMd: contentMd.value,
      postType: postType.value,
      problemId: problemId.value ? Number(problemId.value) : undefined,
    })
    router.push({ name: 'post-detail', params: { id: r.id } })
  } catch (e: unknown) {
    const err = e as { response?: { status?: number; data?: { code?: number; message?: string; fieldErrors?: Array<{ field: string; message: string }> } } }
    const status = err.response?.status
    if (status === 400) {
      const fields = err.response?.data?.fieldErrors
      if (fields && fields.length > 0) {
        error.value = fields.map(f => f.message).join('；')
      } else {
        error.value = err.response?.data?.message ?? '请检查输入内容'
      }
    } else if (status === 401) {
      error.value = '登录已失效，请重新登录'
    } else if (status === 403) {
      error.value = '无权发布'
    } else if (status === 409) {
      error.value = err.response?.data?.message ?? '内容冲突，请修改后重试'
    } else if (status === 500) {
      error.value = '服务器暂时无法处理请求'
    } else {
      error.value = err.response?.data?.message ?? '发布失败'
    }
  } finally { saving.value = false }
}
</script>

<template>
  <PageContainer>
    <template #header>
      <button class="back-link" @click="router.push({ name: 'posts' })">&larr; 返回</button>
    </template>

    <div class="form-card">
      <h1 class="form-title">发布帖子</h1>

      <div class="field">
        <label class="field-label">类型</label>
        <select v-model="postType" class="field-input">
          <option v-for="t in types" :key="t" :value="t" :disabled="!canSelect(t)">
            {{ postTypeLabels[t] || t }}{{ !canSelect(t) ? '（仅管理员）' : '' }}
          </option>
        </select>
      </div>

      <div class="field" v-if="isSolution()">
        <label class="field-label">关联题目 ID</label>
        <input v-model="problemId" type="number" class="field-input" placeholder="输入题目ID" />
      </div>

      <div class="field">
        <label class="field-label">标题</label>
        <input v-model="title" class="field-input" maxlength="255" placeholder="帖子标题" />
      </div>

      <div class="field">
        <label class="field-label">正文（Markdown）</label>
        <textarea v-model="contentMd" class="field-textarea" rows="12" placeholder="支持 Markdown 格式..." />
      </div>

      <div class="form-actions">
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="cancel-btn" @click="router.back()">取消</button>
        <button class="save-btn" :disabled="saving || !title.trim() || !contentMd.trim()" @click="handleCreate">
          {{ saving ? '发布中...' : '发布' }}
        </button>
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.back-link { background: none; border: none; color: var(--color-primary-container); font-size: var(--text-body-md); font-weight: 500; cursor: pointer; padding: 0; }
.back-link:hover { text-decoration: underline; }

.form-card { background: var(--color-surface-card); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); box-shadow: var(--shadow-card); padding: 32px; max-width: 720px; }
.form-title { font-family: var(--font-headline); font-size: var(--text-headline-md); font-weight: 700; color: var(--color-on-surface); margin-bottom: 28px; padding-bottom: 16px; border-bottom: 1px solid var(--color-border-subtle); }

.field { display: flex; flex-direction: column; gap: 6px; margin-bottom: 20px; }
.field-label { font-size: var(--text-label-sm); font-weight: 600; color: var(--color-on-surface); letter-spacing: 0.05em; }
.field-input, .field-textarea { padding: 10px 14px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); font-size: var(--text-body-md); color: var(--color-on-surface); background: var(--color-surface-container-lowest); font-family: inherit; }
.field-input:focus, .field-textarea:focus { outline: none; border-color: var(--color-primary-container); }
.field-textarea { resize: vertical; min-height: 200px; font-family: var(--font-mono); font-size: var(--text-code-sm); line-height: 1.6; }

.form-actions { display: flex; align-items: center; justify-content: flex-end; gap: 12px; margin-top: 8px; padding-top: 20px; border-top: 1px solid var(--color-border-subtle); }
.form-error { margin-right: auto; font-size: var(--text-body-sm); color: var(--color-status-critical); }
.cancel-btn { height: 36px; padding: 0 18px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); background: transparent; color: var(--color-on-surface-variant); font-size: var(--text-body-md); font-weight: 500; cursor: pointer; }
.save-btn { height: 36px; padding: 0 20px; border: none; border-radius: var(--radius-md); background: var(--color-primary-container); color: var(--color-on-primary); font-size: var(--text-body-md); font-weight: 600; cursor: pointer; }
.save-btn:hover:not(:disabled) { opacity: 0.9; }
.save-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
