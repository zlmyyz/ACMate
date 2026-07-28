<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { listPlans } from '@/api/training'
import type { PlanSummary, ListType } from '@/types/training'
import { trainingTypeLabels, trainingTimeStatusLabels } from '@/constants/labels'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const router = useRouter()

const plans = ref<PlanSummary[]>([])
const loading = ref(true)
const error = ref('')
const total = ref(0)
const page = ref(1)
const size = 20

const activeTab = ref<ListType>('PUBLIC')
const timeStatusFilter = ref('')
const keyword = ref('')

const tabs: { key: ListType; label: string }[] = [
  { key: 'PUBLIC', label: trainingTypeLabels.PUBLIC },
  { key: 'MY_CREATED', label: '我创建的' },
  { key: 'MY_JOINED', label: '我加入的' },
]

let fetchSeq = 0

async function fetchPlans() {
  const seq = ++fetchSeq
  loading.value = true
  error.value = ''
  try {
    const res = await listPlans({
      type: activeTab.value,
      timeStatus: timeStatusFilter.value,
      keyword: keyword.value,
      page: page.value,
      size,
    })
    if (seq !== fetchSeq) return
    plans.value = res.plans
    total.value = res.total
  } catch (e: unknown) {
    if (seq !== fetchSeq) return
    const err = e as { response?: { status: number; data?: { message?: string } } }
    if (err.response?.status === 401) {
      router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
      return
    }
    error.value = err.response?.data?.message || '加载训练计划失败，请稍后重试'
  } finally {
    if (seq === fetchSeq) loading.value = false
  }
}

function onSearch() {
  page.value = 1
  fetchPlans()
}

function onPageChange(p: number) {
  page.value = p
  fetchPlans()
}

watch(activeTab, () => { page.value = 1; fetchPlans() })

onMounted(fetchPlans)
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="list-header">
        <h1 class="list-title">训练计划</h1>
        <button class="create-btn" @click="router.push({ name: 'create-plan' })">
          创建计划
        </button>
      </div>
    </template>

    <div class="tabs-row">
      <button
        v-for="t in tabs" :key="t.key"
        class="tab-btn"
        :class="{ active: activeTab === t.key }"
        @click="activeTab = t.key"
      >
        {{ t.label }}
      </button>
    </div>

    <div class="filter-row">
      <select v-model="timeStatusFilter" class="filter-select" @change="onSearch">
        <option value="">全部状态</option>
        <option value="NOT_STARTED">{{ trainingTimeStatusLabels.NOT_STARTED }}</option>
        <option value="ONGOING">{{ trainingTimeStatusLabels.ONGOING }}</option>
        <option value="ENDED">{{ trainingTimeStatusLabels.ENDED }}</option>
      </select>
      <input
        v-model="keyword"
        class="search-input"
        placeholder="搜索计划名称..."
        @keyup.enter="onSearch"
      />
      <button class="search-btn" @click="onSearch">搜索</button>
    </div>

    <LoadingState v-if="loading" />

    <ErrorState v-else-if="error" :message="error" @retry="fetchPlans" />

    <template v-else>
      <div v-if="plans.length === 0" class="empty-state">
        <p>暂无训练计划</p>
      </div>

      <div v-else class="plan-grid">
        <div
          v-for="plan in plans"
          :key="plan.id"
          class="plan-card"
          :class="{ inactive: !plan.active }"
          @click="router.push({ name: 'plan-detail', params: { id: plan.id } })"
        >
          <div class="plan-card-header">
            <h2 class="plan-title">{{ plan.title }}</h2>
            <span v-if="!plan.active" class="inactive-badge">已停用</span>
          </div>
          <div class="plan-meta">
            <span class="plan-creator">{{ plan.creatorNickname || plan.creatorUsername }}</span>
            <span class="plan-time-status" :class="plan.timeStatus.toLowerCase()">
              {{ trainingTimeStatusLabels[plan.timeStatus] || plan.timeStatus }}
            </span>
          </div>
          <div class="plan-stats">
            <span>{{ plan.problemCount }} 题</span>
            <span>{{ plan.memberCount }} 人</span>
          </div>
          <div v-if="plan.startTime || plan.endTime" class="plan-dates">
            <span v-if="plan.startTime">{{ new Date(plan.startTime).toLocaleDateString('zh-CN') }}</span>
            <span v-if="plan.startTime && plan.endTime"> ~ </span>
            <span v-if="plan.endTime">{{ new Date(plan.endTime).toLocaleDateString('zh-CN') }}</span>
          </div>
        </div>
      </div>

      <PaginationBar
        :page="page"
        :total="total"
        :size="size"
        @change="onPageChange"
      />
    </template>
  </PageContainer>
</template>

<style scoped>
.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.list-title {
  font-family: var(--font-headline);
  font-size: var(--text-display-lg);
  font-weight: 700;
  color: var(--color-on-surface);
}

.create-btn {
  height: 36px;
  padding: 0 20px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-primary-container);
  color: var(--color-on-primary);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}
.create-btn:hover { opacity: 0.9; }

.tabs-row {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--color-border-subtle);
  margin-top: var(--space-stack-md);
  margin-bottom: var(--space-stack-md);
}

.tab-btn {
  padding: 10px 24px;
  border: none;
  background: none;
  font-size: var(--text-body-lg);
  font-weight: 500;
  color: var(--color-on-surface-variant);
  cursor: pointer;
  border-bottom: 3px solid transparent;
  margin-bottom: -2px;
  transition: color 0.2s, border-color 0.2s;
}
.tab-btn:hover { color: var(--color-on-surface); }
.tab-btn.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
  font-weight: 700;
}

.filter-row {
  display: flex;
  gap: 8px;
  margin-bottom: var(--space-stack-md);
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  color: var(--color-on-surface);
  background: var(--color-surface-card);
}

.search-input {
  flex: 1;
  max-width: 300px;
  padding: 8px 12px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
}
.search-input:focus { outline: none; border-color: var(--color-primary-container); }

.search-btn {
  height: 38px;
  padding: 0 16px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  background: var(--color-surface-card);
  color: var(--color-on-surface);
  font-size: var(--text-body-md);
  cursor: pointer;
  transition: border-color 0.2s;
}
.search-btn:hover { border-color: var(--color-primary-container); }

.empty-state {
  text-align: center;
  padding: 60px 24px;
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-lg);
}

.plan-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-stack-sm);
}

.plan-card {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  padding: 20px;
  cursor: pointer;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.plan-card:hover { box-shadow: var(--shadow-sm); border-color: var(--color-primary-container); }
.plan-card.inactive { opacity: 0.6; }

.plan-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.plan-title {
  font-family: var(--font-headline);
  font-size: var(--text-headline-sm);
  font-weight: 600;
  color: var(--color-on-surface);
}

.inactive-badge {
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-status-critical);
  background: rgba(217, 45, 32, 0.1);
  padding: 2px 8px;
  border-radius: 999px;
}

.plan-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}

.plan-creator { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }

.plan-time-status {
  font-size: var(--text-label-sm);
  font-weight: 600;
  padding: 1px 8px;
  border-radius: 999px;
}
.plan-time-status.ongoing { color: var(--color-status-success); background: rgba(52,168,83,0.12); }
.plan-time-status.not_started { color: var(--color-status-pending); background: rgba(243,161,60,0.12); }
.plan-time-status.ended { color: var(--color-on-surface-variant); background: var(--color-surface-container); }

.plan-stats {
  display: flex;
  gap: 16px;
  font-size: var(--text-body-sm);
  color: var(--color-on-surface-variant);
  margin-bottom: 8px;
}

.plan-dates { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
</style>
