<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { ProblemSummary } from '@/types/problem'
import PlatformBadge from '@/components/common/PlatformBadge.vue'
import DifficultyBadge from '@/components/common/DifficultyBadge.vue'
import TagList from '@/components/common/TagList.vue'

defineProps<{
  problems: ProblemSummary[]
  loading: boolean
}>()

const router = useRouter()

function goToDetail(id: number) {
  router.push({ name: 'problem-detail', params: { id } })
}
</script>

<template>
  <div class="table-wrapper">
    <table class="problem-table" v-if="problems.length > 0 || loading">
      <thead>
        <tr>
          <th class="col-id">#</th>
          <th class="col-title">标题</th>
          <th class="col-platform">平台</th>
          <th class="col-difficulty">难度</th>
          <th class="col-tags">标签</th>
          <th class="col-creator">创建者</th>
          <th class="col-time">创建时间</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="loading">
          <td colspan="7" class="loading-cell">加载中...</td>
        </tr>
        <tr
          v-for="problem in problems"
          :key="problem.id"
          class="problem-row"
          @click="goToDetail(problem.id)"
        >
          <td class="col-id">
            <span class="problem-id">{{ problem.id }}</span>
          </td>
          <td class="col-title">
            <span class="problem-title">{{ problem.title }}</span>
            <span v-if="problem.externalProblemKey" class="problem-key">
              {{ problem.externalProblemKey }}
            </span>
          </td>
          <td class="col-platform">
            <PlatformBadge :platform="problem.platform" />
          </td>
          <td class="col-difficulty">
            <DifficultyBadge
              v-if="problem.difficulty"
              :difficulty="problem.difficulty"
            />
            <span v-else class="no-value">—</span>
          </td>
          <td class="col-tags">
            <TagList :tags="problem.tags" />
          </td>
          <td class="col-creator">
            <span class="creator-text">
              用户 #{{ problem.creatorUserId }}
            </span>
          </td>
          <td class="col-time">
            {{ new Date(problem.createTime).toLocaleDateString('zh-CN') }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-wrapper {
  overflow-x: auto;
}

.problem-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 800px;
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

.col-id {
  width: 60px;
}

.col-platform {
  width: 110px;
}

.col-difficulty {
  width: 90px;
  text-align: center;
}

.col-tags {
  max-width: 200px;
}

.col-creator {
  width: 120px;
}

.col-time {
  width: 110px;
  white-space: nowrap;
}

.problem-id {
  font-family: var(--font-mono);
  font-size: var(--text-code-sm);
  color: var(--color-secondary);
}

.problem-title {
  color: var(--color-primary);
  font-weight: 500;
}

.problem-row:hover .problem-title {
  text-decoration: underline;
}

.problem-key {
  display: block;
  font-family: var(--font-mono);
  font-size: var(--text-label-sm);
  color: var(--color-on-surface-variant);
  margin-top: 2px;
}

.no-value {
  color: var(--color-on-surface-variant);
}

.creator-text {
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
}

.loading-cell {
  text-align: center;
  padding: 48px 16px;
  color: var(--color-on-surface-variant);
}
</style>
