<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, computed } from 'vue'
import type { CreateProblemRequest, ProblemDetail, ImportedProblemPreview } from '@/types/problem'
import { importNowcoderProblem } from '@/api/problems'
import MarkdownContent from '@/components/common/MarkdownContent.vue'
import { platformLabels } from '@/constants/labels'

const props = defineProps<{
  allowImport?: boolean
  initialData?: ProblemDetail | null
  draftKey: string
  submitting: boolean
  errorMsg: string
}>()

const emit = defineEmits<{
  submit: [data: CreateProblemRequest]
  cancel: []
}>()

const importUrl = ref('')
const importing = ref(false)
const importError = ref('')
const importPreview = ref<ImportedProblemPreview | null>(null)
async function readNowcoder() {
  if (!importUrl.value.trim() || importing.value) return
  importError.value = ''
  importPreview.value = null
  importing.value = true
  try { importPreview.value = await importNowcoderProblem(importUrl.value.trim()) }
  catch (e: unknown) {
    const error = e as { response?: { data?: { message?: string } } }
    importError.value = error.response?.data?.message || '读取失败，请稍后重试或手动填写'
  } finally { importing.value = false }
}
function applyImport() {
  const data = importPreview.value
  if (!data) return
  platform.value = data.platform
  externalProblemKey.value = data.externalProblemKey || ''
  title.value = data.title
  sourceUrl.value = data.sourceUrl || ''
  difficulty.value = data.difficulty || ''
  tags.value = data.tags || ''
  contentMd.value = data.contentMd || ''
  importPreview.value = null
}
const platform = ref('CUSTOM')
const externalProblemKey = ref('')
const title = ref('')
const sourceUrl = ref('')
const difficulty = ref('')
const tags = ref('')
const contentMd = ref('')

const platforms = ['CUSTOM', 'CODEFORCES', 'NOWCODER', 'OTHER']

const isDirty = ref(false)
let saveTimer: ReturnType<typeof setTimeout> | null = null

const nonCustomNoKey = computed(
  () => platform.value !== 'CUSTOM' && !externalProblemKey.value.trim(),
)

function markDirty() {
  isDirty.value = true
}

function buildRequest(): CreateProblemRequest {
  return {
    platform: platform.value,
    externalProblemKey: externalProblemKey.value.trim() || undefined,
    title: title.value.trim(),
    sourceUrl: sourceUrl.value.trim() || undefined,
    difficulty: difficulty.value.trim() || undefined,
    tags: tags.value.trim() || undefined,
    contentMd: contentMd.value || undefined,
  }
}

function saveDraft() {
  const data = buildRequest()
  localStorage.setItem(props.draftKey, JSON.stringify(data))
}

function debouncedSaveDraft() {
  if (saveTimer) clearTimeout(saveTimer)
  saveTimer = setTimeout(saveDraft, 800)
}

function loadDraft() {
  const raw = localStorage.getItem(props.draftKey)
  if (!raw) return
  try {
    const data = JSON.parse(raw) as CreateProblemRequest
    if (data.platform) platform.value = data.platform
    if (data.externalProblemKey) externalProblemKey.value = data.externalProblemKey
    if (data.title) title.value = data.title
    if (data.sourceUrl) sourceUrl.value = data.sourceUrl
    if (data.difficulty) difficulty.value = data.difficulty
    if (data.tags) tags.value = data.tags
    if (data.contentMd) contentMd.value = data.contentMd
    isDirty.value = true
  } catch {
    // Corrupted draft, ignore
  }
}

function clearDraft() {
  localStorage.removeItem(props.draftKey)
}

function loadInitial() {
  if (props.initialData) {
    platform.value = props.initialData.platform
    externalProblemKey.value = props.initialData.externalProblemKey || ''
    title.value = props.initialData.title
    sourceUrl.value = props.initialData.sourceUrl || ''
    difficulty.value = props.initialData.difficulty || ''
    tags.value = props.initialData.tags || ''
    contentMd.value = props.initialData.contentMd || ''
  } else {
    loadDraft()
  }
}

function handleSubmit() {
  if (nonCustomNoKey.value || importing.value) return
  emit('submit', buildRequest())
}

function beforeUnload(e: BeforeUnloadEvent) {
  if (isDirty.value) {
    e.preventDefault()
  }
}

onMounted(() => {
  loadInitial()
  window.addEventListener('beforeunload', beforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeUnload)
  if (saveTimer) clearTimeout(saveTimer)
})

watch([platform, externalProblemKey, title, sourceUrl, difficulty, tags, contentMd], () => {
  markDirty()
  if (!props.initialData) {
    debouncedSaveDraft()
  }
})

defineExpose({ clearDraft, isDirty })
</script>

<template>
  <form class="problem-form" @submit.prevent="handleSubmit">
    <section v-if="allowImport" class="link-import" aria-label="牛客链接导入">
      <label for="nowcoder-import-url" class="form-label">从牛客链接导入</label>
      <div class="import-input-row">
        <input id="nowcoder-import-url" v-model="importUrl" type="text" inputmode="url" class="form-input" placeholder="粘贴牛客题目链接（https://…）" :disabled="importing || submitting" @keydown.enter.prevent="readNowcoder" />
        <button type="button" class="btn-submit" :disabled="importing || submitting || !importUrl.trim()" @click="readNowcoder">{{ importing ? '读取中…' : '读取题目' }}</button>
      </div>
      <p class="form-hint">支持牛客竞赛题、公开题库及练习题。读取后确认填入，检查内容后再保存。</p>
      <p v-if="importError" role="alert" class="form-error">{{ importError }}</p>
      <div v-if="importPreview" class="import-preview" role="status">
        <strong>{{ importPreview.title }}</strong>
        <p class="form-hint">牛客 · {{ importPreview.externalProblemKey }}<span v-if="importPreview.difficulty"> · 难度 {{ importPreview.difficulty }}</span></p>
        <p v-for="warning in importPreview.warnings" :key="warning" class="form-hint">{{ warning }}</p>
        <details><summary>查看题面</summary><MarkdownContent :content="importPreview.contentMd || null" /></details>
        <p class="form-hint">填入将替换当前表单中的题目信息。</p>
        <div class="import-actions"><button type="button" class="btn-submit" :disabled="submitting" @click="applyImport">确认填入表单</button><button type="button" class="btn-cancel" @click="importPreview = null">取消</button></div>
      </div>
    </section>
    <div class="form-group">
      <label class="form-label">
        平台 <span class="required">*</span>
      </label>
      <select v-model="platform" class="form-input" :disabled="submitting">
        <option v-for="p in platforms" :key="p" :value="p">
          {{ platformLabels[p] || p }}
        </option>
      </select>
    </div>

    <div class="form-row">
      <div class="form-group">
        <label class="form-label">
          外部题目标识
          <span v-if="platform !== 'CUSTOM'" class="required">*</span>
        </label>
        <input
          v-model="externalProblemKey"
          type="text"
          class="form-input"
          placeholder="如：1A、NC319875"
          maxlength="64"
          :disabled="submitting"
        />
        <p v-if="nonCustomNoKey" class="form-hint form-hint-error">
          非自定义平台必须提供外部题目标识
        </p>
      </div>
      <div class="form-group">
        <label class="form-label">来源链接</label>
        <input
          v-model="sourceUrl"
          type="url"
          class="form-input"
          placeholder="https://"
          maxlength="1024"
          :disabled="submitting"
        />
      </div>
    </div>

    <div class="form-group">
      <label class="form-label">
        标题 <span class="required">*</span>
      </label>
      <input
        v-model="title"
        type="text"
        class="form-input"
        placeholder="输入题目标题"
        maxlength="255"
        required
        :disabled="submitting"
      />
    </div>

    <div class="form-row">
      <div class="form-group">
        <label class="form-label">难度</label>
        <input
          v-model="difficulty"
          type="text"
          class="form-input"
          placeholder="如：800、1200、Easy、Hard"
          maxlength="32"
          :disabled="submitting"
        />
      </div>
      <div class="form-group">
        <label class="form-label">标签</label>
        <input
          v-model="tags"
          type="text"
          class="form-input"
          placeholder="多个标签用逗号分隔，如：dp, graph, math"
          maxlength="255"
          :disabled="submitting"
        />
      </div>
    </div>

    <div class="form-group">
      <label class="form-label">题目正文（Markdown）</label>
      <textarea
        v-model="contentMd"
        class="form-textarea"
        placeholder="使用 Markdown 编写题目描述..."
        :disabled="submitting"
      ></textarea>
    </div>

    <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>

    <div class="form-actions">
      <button type="button" class="btn-cancel" :disabled="submitting" @click="$emit('cancel')">
        取消
      </button>
      <button type="submit" class="btn-submit" :disabled="submitting || importing || nonCustomNoKey">
        {{ submitting ? '提交中...' : '保存' }}
      </button>
    </div>
  </form>
</template>

<style scoped>
.link-import{display:grid;gap:10px;padding-bottom:22px;border-bottom:1px solid var(--color-border-subtle)}
.import-input-row{display:flex;gap:12px}.import-input-row input{flex:1;min-width:0}.import-input-row button{white-space:nowrap}
.import-preview{display:grid;gap:10px;padding:16px;background:var(--color-surface-container-low);border-radius:var(--radius-sm)}
.import-preview details summary{cursor:pointer;color:var(--color-primary)}.import-actions{display:flex;gap:10px}
@media(max-width:600px){.import-input-row{flex-direction:column}}

.problem-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.form-label {
  font-size: var(--text-label-sm);
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--color-on-surface-variant);
  text-transform: uppercase;
}

.required {
  color: var(--color-error);
}

.form-input {
  height: 42px;
  padding: 0 12px;
  border: 1px solid var(--color-outline-variant);
  border-radius: var(--radius-sm);
  font-size: var(--text-body-md);
  color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
  outline: none;
  transition: border-color 0.2s;
}

.form-input:focus {
  border-color: var(--color-primary);
}

.form-input:disabled {
  opacity: 0.6;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-textarea {
  min-height: 320px;
  padding: 12px;
  border: 1px solid var(--color-outline-variant);
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: var(--text-code-sm);
  line-height: 1.7;
  color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
  outline: none;
  resize: vertical;
  transition: border-color 0.2s;
}

.form-textarea:focus {
  border-color: var(--color-primary);
}

.form-textarea:disabled {
  opacity: 0.6;
}

.form-hint {
  font-size: var(--text-label-sm);
  margin-top: 2px;
}

.form-hint-error {
  color: var(--color-error);
}

.form-error {
  color: var(--color-error);
  font-size: var(--text-body-md);
  text-align: center;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border-subtle);
}

.btn-cancel {
  height: 40px;
  padding: 0 20px;
  border: 1px solid var(--color-outline-variant);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-cancel:hover:not(:disabled) {
  background: var(--color-surface-container);
}

.btn-submit {
  height: 40px;
  padding: 0 24px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: var(--color-on-primary);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-submit:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-submit:disabled,
.btn-cancel:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (max-width: 640px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>

