<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { platformLabels } from '@/constants/labels'

const props = defineProps<{
  modelValue: {
    page?: number
    keyword?: string
    platform?: string
    difficulty?: string
  }
  showCreatorFilter?: boolean
  creatorId?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: typeof props.modelValue]
  'update:creatorId': [value: number | null]
  search: []
}>()

const router = useRouter()

const localKeyword = ref(props.modelValue.keyword || '')
const localPlatform = ref(props.modelValue.platform || '')
const localDifficulty = ref(props.modelValue.difficulty || '')
const localCreatorId = ref(props.creatorId || null)

const platforms = ['', 'CUSTOM', 'CODEFORCES', 'NOWCODER', 'OTHER']

watch(
  () => props.modelValue,
  (v) => {
    localKeyword.value = v.keyword || ''
    localPlatform.value = v.platform || ''
    localDifficulty.value = v.difficulty || ''
  },
)

watch(
  () => props.creatorId,
  (v) => {
    localCreatorId.value = v || null
  },
)

function apply() {
  const query: Record<string, string> = {}
  if (localKeyword.value) query.keyword = localKeyword.value
  if (localPlatform.value) query.platform = localPlatform.value
  if (localDifficulty.value) query.difficulty = localDifficulty.value
  router.push({ query: { ...query, page: '1' } })
  emit('update:creatorId', localCreatorId.value || null)
  emit('search')
}

function reset() {
  localKeyword.value = ''
  localPlatform.value = ''
  localDifficulty.value = ''
  localCreatorId.value = null
  router.push({ query: {} })
  emit('update:creatorId', null)
  emit('search')
}
</script>

<template>
  <div class="filters-bar">
    <div class="filters-row">
      <div class="filter-input keyword-input">
        <input
          v-model="localKeyword"
          type="text"
          placeholder="搜索标题或题目标识..."
          @keyup.enter="apply"
        />
      </div>
      <select v-model="localPlatform" class="filter-select">
        <option value="">全部平台</option>
        <option v-for="p in platforms.filter(Boolean)" :key="p" :value="p">
          {{ platformLabels[p] }}
        </option>
      </select>
      <input
        v-model="localDifficulty"
        type="text"
        class="filter-input difficulty-input"
        placeholder="难度"
      />
      <div v-if="showCreatorFilter" class="filter-input creator-input">
        <input
          v-model="localCreatorId"
          type="number"
          placeholder="创建者 ID"
        />
      </div>
      <button class="filter-btn search-btn" @click="apply">搜索</button>
      <button class="filter-btn reset-btn" @click="reset">重置</button>
    </div>
  </div>
</template>

<style scoped>
.filters-bar {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
}

.filters-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.filter-input {
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

.filter-input:focus {
  border-color: var(--color-primary-container);
}

.keyword-input {
  flex: 1;
  min-width: 200px;
}

.keyword-input input {
  width: 100%;
  height: 100%;
  border: none;
  background: transparent;
  outline: none;
  font-size: inherit;
  color: inherit;
}

.difficulty-input {
  width: 100px;
}

.creator-input {
  width: 120px;
}

.creator-input input {
  width: 100%;
  height: 100%;
  border: none;
  background: transparent;
  outline: none;
  font-size: inherit;
  color: inherit;
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

.filter-select:focus {
  border-color: var(--color-primary-container);
}

.filter-btn {
  height: 38px;
  padding: 0 20px;
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.filter-btn:hover {
  opacity: 0.9;
}

.search-btn {
  background: var(--color-primary-container);
  color: var(--color-on-primary);
}

.reset-btn {
  background: var(--color-surface-container);
  color: var(--color-on-surface-variant);
  border: 1px solid var(--color-border-subtle);
}
</style>
