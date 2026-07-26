<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPostDetail, updatePost } from '@/api/discussion'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'

const route = useRoute()
const router = useRouter()
const title = ref('')
const contentMd = ref('')
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const postId = computed(() => Number(route.params.id))

async function fetch() {
  loading.value = true
  try {
    const p = await getPostDetail(postId.value)
    title.value = p.title; contentMd.value = p.contentMd
  } catch { error.value = '加载失败' }
  finally { loading.value = false }
}

async function handleSave() {
  if (!title.value.trim()) return
  error.value = ''; saving.value = true
  try {
    await updatePost(postId.value, { title: title.value.trim(), contentMd: contentMd.value })
    router.push({ name: 'post-detail', params: { id: postId.value } })
  } catch { error.value = '保存失败' }
  finally { saving.value = false }
}

onMounted(fetch)
</script>

<template>
  <PageContainer>
    <template #header>
      <button class="back-link" @click="router.back()">&larr; 返回</button>
    </template>

    <LoadingState v-if="loading" />

    <div v-else class="form-card">
      <h1 class="form-title">编辑帖子</h1>

      <div class="field">
        <label class="field-label">标题</label>
        <input v-model="title" class="field-input" maxlength="255" />
      </div>

      <div class="field">
        <label class="field-label">正文（Markdown）</label>
        <textarea v-model="contentMd" class="field-textarea" rows="12" />
      </div>

      <div class="form-actions">
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="cancel-btn" @click="router.back()">取消</button>
        <button class="save-btn" :disabled="saving || !title.trim()" @click="handleSave">
          {{ saving ? '保存中...' : '保存' }}
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
