<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { getProblems } from '@/api/problems'
import type { ProblemSummary } from '@/types/problem'
import PlatformBadge from '@/components/common/PlatformBadge.vue'
import DifficultyBadge from '@/components/common/DifficultyBadge.vue'

export interface SelectedProblem {
  problemId: number
  title: string
  platform: string
  difficulty: string | null
  sortOrder: number
}

const props = defineProps<{
  modelValue: SelectedProblem[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: SelectedProblem[]]
}>()

const keyword = ref('')
const results = ref<ProblemSummary[]>([])
const searching = ref(false)
const searchError = ref('')
const page = ref(1)
const totalPages = ref(1)

const selectedIds = computed(() => new Set(props.modelValue.map(p => p.problemId)))

let debounceTimer: ReturnType<typeof setTimeout> | null = null

async function doSearch(p: number) {
  searching.value = true
  searchError.value = ''
  try {
    const q = keyword.value.trim()
    const resp = await getProblems({ keyword: q || undefined, page: p, size: 10 })
    if (p === 1) {
      results.value = resp.records
    } else {
      results.value = [...results.value, ...resp.records]
    }
    page.value = p
    totalPages.value = resp.pages
  } catch {
    searchError.value = '搜索失败'
  } finally {
    searching.value = false
  }
}

watch(keyword, () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => doSearch(1), 300)
})

function addProblem(item: ProblemSummary) {
  if (selectedIds.value.has(item.id)) return
  const entry: SelectedProblem = {
    problemId: item.id,
    title: item.title,
    platform: item.platform,
    difficulty: item.difficulty,
    sortOrder: props.modelValue.length,
  }
  emit('update:modelValue', [...props.modelValue, entry])
}

function removeProblem(index: number) {
  const updated = [...props.modelValue]
  updated.splice(index, 1)
  updated.forEach((p, i) => (p.sortOrder = i))
  emit('update:modelValue', updated)
}

function moveUp(index: number) {
  if (index === 0) return
  const updated = [...props.modelValue]
  const tmp = updated[index]!
  updated[index] = updated[index - 1]!
  updated[index - 1] = tmp
  updated.forEach((p, i) => (p.sortOrder = i))
  emit('update:modelValue', updated)
}

function moveDown(index: number) {
  if (index === props.modelValue.length - 1) return
  const updated = [...props.modelValue]
  const tmp = updated[index]!
  updated[index] = updated[index + 1]!
  updated[index + 1] = tmp
  updated.forEach((p, i) => (p.sortOrder = i))
  emit('update:modelValue', updated)
}

function loadMore() {
  if (page.value < totalPages.value) doSearch(page.value + 1)
}
</script>

<template>
  <div class="selector">
    <div class="search-area">
      <label class="field-label">搜索题目（可选）</label>
      <input
        v-model="keyword"
        class="field-input"
        placeholder="输入标题或平台题号搜索..."
      />
    </div>

    <div v-if="keyword.trim()" class="search-results">
      <div v-if="searching && results.length === 0" class="search-status">搜索中...</div>
      <div v-else-if="searchError" class="search-status error">{{ searchError }}</div>
      <div v-else-if="results.length === 0" class="search-status">无匹配题目</div>
      <div v-else class="result-list">
        <div v-for="item in results" :key="item.id" class="result-row">
          <div class="result-info">
            <PlatformBadge :platform="item.platform" />
            <span v-if="item.externalProblemKey" class="ext-key">{{ item.externalProblemKey }}</span>
            <span class="result-title">{{ item.title }}</span>
            <DifficultyBadge v-if="item.difficulty" :difficulty="item.difficulty" />
          </div>
          <button
            v-if="!selectedIds.has(item.id)"
            class="add-btn"
            @click="addProblem(item)"
          >添加</button>
          <span v-else class="added-label">已添加</span>
        </div>
        <button
          v-if="page < totalPages"
          class="load-more-btn"
          :disabled="searching"
          @click="loadMore"
        >加载更多</button>
      </div>
    </div>

    <div v-if="modelValue.length > 0" class="selected-area">
      <h3 class="selected-title">已选题目 ({{ modelValue.length }})</h3>
      <div v-for="(item, index) in modelValue" :key="item.problemId" class="selected-row">
        <span class="order-label">{{ index + 1 }}.</span>
        <PlatformBadge :platform="item.platform" />
        <span class="result-title">{{ item.title }}</span>
        <DifficultyBadge v-if="item.difficulty" :difficulty="item.difficulty" />
        <div class="order-actions">
          <button class="order-btn" :disabled="index === 0" @click="moveUp(index)" title="上移">&uarr;</button>
          <button class="order-btn" :disabled="index === modelValue.length - 1" @click="moveDown(index)" title="下移">&darr;</button>
          <button class="remove-btn" @click="removeProblem(index)" title="移除">&times;</button>
        </div>
      </div>
    </div>

    <div v-else class="empty-hint">暂未选择题目，可先创建后再添加。</div>
  </div>
</template>

<style scoped>
.selector {
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  padding: 16px;
  background: var(--color-surface-container-lowest);
}

.search-area { margin-bottom: 12px; }

.field-label {
  display: block;
  font-size: var(--text-label-sm); font-weight: 600;
  color: var(--color-on-surface); margin-bottom: 6px; letter-spacing: 0.05em;
}

.field-input {
  width: 100%; padding: 8px 12px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm);
  font-size: var(--text-body-md); color: var(--color-on-surface);
  background: var(--color-surface-card); font-family: inherit;
  box-sizing: border-box;
}
.field-input:focus { outline: none; border-color: var(--color-primary-container); }

.search-status { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); padding: 8px 0; }
.search-status.error { color: var(--color-status-critical); }

.result-list { display: flex; flex-direction: column; gap: 2px; }
.result-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 10px; background: var(--color-surface-card);
  border-radius: var(--radius-sm); border: 1px solid var(--color-border-subtle);
}
.result-info { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.ext-key { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); font-family: monospace; }
.result-title { font-size: var(--text-body-md); font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 280px; }

.add-btn {
  height: 28px; padding: 0 12px; border: 1px solid var(--color-primary-container);
  border-radius: var(--radius-sm); background: transparent;
  color: var(--color-primary-container); font-size: var(--text-body-sm); font-weight: 500; cursor: pointer;
  white-space: nowrap; flex-shrink: 0;
}
.add-btn:hover { background: var(--color-primary-container); color: var(--color-on-primary); }

.added-label {
  font-size: var(--text-body-sm); color: var(--color-on-surface-variant); white-space: nowrap; flex-shrink: 0;
}

.load-more-btn {
  margin-top: 8px; width: 100%; height: 32px;
  border: 1px solid var(--color-border-subtle); border-radius: var(--radius-sm);
  background: var(--color-surface-card); color: var(--color-on-surface-variant);
  font-size: var(--text-body-sm); cursor: pointer;
}
.load-more-btn:hover:not(:disabled) { background: var(--color-surface-container-lowest); }
.load-more-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.selected-area {
  margin-top: 16px; padding-top: 16px;
  border-top: 1px solid var(--color-border-subtle);
}
.selected-title {
  font-size: var(--text-body-md); font-weight: 600;
  color: var(--color-on-surface); margin-bottom: 10px;
}

.selected-row {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid var(--color-border-subtle);
}
.selected-row:last-child { border-bottom: none; }
.order-label {
  font-size: var(--text-body-sm); color: var(--color-on-surface-variant);
  min-width: 22px; text-align: right; font-weight: 600;
}
.order-actions { display: flex; gap: 4px; margin-left: auto; }
.order-btn {
  width: 28px; height: 28px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm); background: var(--color-surface-card);
  color: var(--color-on-surface-variant); font-size: var(--text-body-sm); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}
.order-btn:hover:not(:disabled) { background: var(--color-surface-container-lowest); color: var(--color-on-surface); }
.order-btn:disabled { opacity: 0.3; cursor: not-allowed; }
.remove-btn {
  width: 28px; height: 28px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-sm); background: var(--color-surface-card);
  color: var(--color-status-critical); font-size: 16px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}
.remove-btn:hover { background: var(--color-status-critical); color: #fff; border-color: var(--color-status-critical); }

.empty-hint {
  font-size: var(--text-body-sm); color: var(--color-on-surface-variant);
  padding: 12px 0; text-align: center;
}
</style>
