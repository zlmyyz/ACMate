<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getLeaderboard } from '@/api/leaderboard'
import type { LeaderboardEntry } from '@/types/leaderboard'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const route = useRoute()
const router = useRouter()

const entries = ref<LeaderboardEntry[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
const period = ref('total')
const page = ref(1)
const size = 20

const periods = [
  { value: 'total', label: '总榜' },
  { value: '7d', label: '近7天' },
  { value: '30d', label: '近30天' },
]

async function fetchLeaderboard() {
  loading.value = true; error.value = ''
  try {
    const r = await getLeaderboard(period.value, page.value, size)
    entries.value = r.entries
    total.value = r.total
  } catch {
    error.value = '加载排行榜失败'
  } finally { loading.value = false }
}

function onPeriodChange(p: string) {
  period.value = p
  page.value = 1
  fetchLeaderboard()
}

function onPageChange(p: number) {
  page.value = p
  router.replace({ query: { period: period.value, page: String(p) } })
  fetchLeaderboard()
}

onMounted(() => {
  const q = route.query
  if (q.period && ['total', '7d', '30d'].includes(String(q.period))) period.value = String(q.period)
  if (q.page && Number(q.page) > 0) page.value = Number(q.page)
  fetchLeaderboard()
})

watch(period, (v) => router.replace({ query: { period: v, page: String(page.value) } }))
</script>

<template>
  <PageContainer>
    <template #header>
      <h1 class="page-title">排行榜</h1>
    </template>

    <div class="period-tabs">
      <button
        v-for="p in periods" :key="p.value"
        class="period-tab"
        :class="{ active: period === p.value }"
        @click="onPeriodChange(p.value)"
      >{{ p.label }}</button>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchLeaderboard" />

    <template v-else>
      <div v-if="entries.length === 0" class="empty-state">
        <p>暂无可信同步数据</p>
      </div>

      <template v-else>
        <div class="leaderboard-table">
          <div class="table-header">
            <span class="col-rank">排名</span>
            <span class="col-user">用户</span>
            <span class="col-count">AC 题数</span>
          </div>
          <div
            v-for="entry in entries" :key="entry.userId"
            class="table-row"
            :class="{ 'is-me': entry.isMe }"
          >
            <span class="col-rank">
              <span v-if="entry.rank <= 3" class="rank-badge" :class="'rank-' + entry.rank">
                {{ entry.rank }}
              </span>
              <span v-else class="rank-num">{{ entry.rank }}</span>
            </span>
            <span class="col-user">
              <RouterLink :to="`/users/${entry.userId}`" class="user-link">
                {{ entry.nickname || entry.username }}
              </RouterLink>
            </span>
            <span class="col-count">{{ entry.solvedCount }}</span>
          </div>
        </div>
        <PaginationBar
          :page="page" :size="size" :total="total"
          @change="onPageChange"
        />
      </template>
    </template>
  </PageContainer>
</template>

<style scoped>
.page-title { font-family: var(--font-headline); font-size: var(--text-display-lg); font-weight: 700; color: var(--color-on-surface); }

.period-tabs { display: flex; gap: 4px; margin-bottom: var(--space-stack-md); }
.period-tab {
  padding: 6px 20px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md);
  background: var(--color-surface-card); color: var(--color-on-surface-variant);
  font-size: var(--text-body-md); font-weight: 500; cursor: pointer; transition: all 0.2s;
}
.period-tab.active { background: var(--color-primary-container); color: var(--color-on-primary); border-color: var(--color-primary-container); }

.empty-state { text-align: center; padding: 60px 24px; color: var(--color-on-surface-variant); font-size: var(--text-body-lg); }

.leaderboard-table { border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); overflow: hidden; }

.table-header {
  display: flex; padding: 12px 20px; background: var(--color-surface-container-low);
  font-size: var(--text-body-sm); font-weight: 600; color: var(--color-on-surface-variant);
}
.table-row {
  display: flex; padding: 12px 20px; border-top: 1px solid var(--color-border-subtle);
  transition: background 0.15s; align-items: center;
}
.table-row:hover { background: var(--color-surface-container-low); }
.table-row.is-me { background: rgba(0,0,0,0.03); }

.col-rank { width: 60px; flex-shrink: 0; text-align: center; }
.col-user { flex: 1; min-width: 0; }
.col-count { width: 100px; flex-shrink: 0; text-align: center; font-weight: 600; color: var(--color-on-surface); }

.rank-num { font-size: var(--text-body-md); color: var(--color-on-surface-variant); }
.rank-badge {
  display: inline-flex; align-items: center; justify-content: center;
  width: 28px; height: 28px; border-radius: 50%; font-weight: 700; font-size: var(--text-body-sm);
}
.rank-badge.rank-1 { background: #F5A623; color: #fff; }
.rank-badge.rank-2 { background: #8B9EB0; color: #fff; }
.rank-badge.rank-3 { background: #C49B6C; color: #fff; }

.user-link { color: var(--color-primary-container); font-weight: 500; }
.user-link:hover { text-decoration: underline; }
</style>
