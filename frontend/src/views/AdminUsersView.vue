<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listUsers, toggleUserStatus, toggleUserAdmin } from '@/api/admin'
import type { AdminUser } from '@/types/admin'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const users = ref<AdminUser[]>([])
const loading = ref(true)
const error = ref('')
const total = ref(0)
const page = ref(1)
const size = 20
const keyword = ref('')

async function fetchUsers() {
  loading.value = true; error.value = ''
  try {
    const res = await listUsers({ page: page.value, size, keyword: keyword.value })
    users.value = res.users; total.value = res.total
  } catch {
    error.value = '加载用户列表失败'
  } finally { loading.value = false }
}

function onSearch() { page.value = 1; fetchUsers() }
function onPageChange(p: number) { page.value = p; fetchUsers() }

async function handleToggleStatus(id: number) {
  try {
    await toggleUserStatus(id)
    await fetchUsers()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    alert(err?.response?.data?.message || '操作失败')
  }
}

async function handleToggleAdmin(id: number) {
  try {
    await toggleUserAdmin(id)
    await fetchUsers()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    alert(err?.response?.data?.message || '操作失败')
  }
}

onMounted(fetchUsers)
</script>

<template>
  <PageContainer>
    <template #header>
      <h1 class="page-title">用户管理</h1>
    </template>

    <div class="filter-row">
      <input v-model="keyword" class="search-input" placeholder="搜索用户名/昵称..." @keyup.enter="onSearch" />
      <button class="search-btn" @click="onSearch">搜索</button>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchUsers" />

    <template v-else>
      <div class="user-table">
        <div class="table-header">
          <span class="col-id">ID</span>
          <span class="col-user">用户</span>
          <span class="col-email">邮箱</span>
          <span class="col-role">角色</span>
          <span class="col-status">状态</span>
          <span class="col-time">注册时间</span>
          <span class="col-actions">操作</span>
        </div>
        <div v-for="u in users" :key="u.id" class="table-row">
          <span class="col-id">{{ u.id }}</span>
          <span class="col-user">
            <RouterLink :to="`/users/${u.id}`" class="user-link">
              {{ u.nickname || u.username }}
            </RouterLink>
            <span class="username-sub">@{{ u.username }}</span>
          </span>
          <span class="col-email">{{ u.email || '-' }}</span>
          <span class="col-role">
            <span v-if="u.admin" class="badge admin-badge">管理员</span>
            <span v-else class="badge user-badge">用户</span>
          </span>
          <span class="col-status">
            <span :class="u.status === 1 ? 'status-active' : 'status-inactive'">
              {{ u.status === 1 ? '正常' : '已禁用' }}
            </span>
          </span>
          <span class="col-time">{{ u.createTime ? new Date(u.createTime).toLocaleDateString('zh-CN') : '-' }}</span>
          <span class="col-actions">
            <button class="action-btn" @click="handleToggleStatus(u.id)">
              {{ u.status === 1 ? '禁用' : '恢复' }}
            </button>
            <button class="action-btn" @click="handleToggleAdmin(u.id)">
              {{ u.admin ? '取消管理' : '设为管理' }}
            </button>
          </span>
        </div>
      </div>

      <PaginationBar :page="page" :total="total" :size="size" @change="onPageChange" />
    </template>
  </PageContainer>
</template>

<style scoped>
.page-title { font-family: var(--font-headline); font-size: var(--text-display-lg); font-weight: 700; color: var(--color-on-surface); }

.filter-row { display: flex; gap: 8px; margin-bottom: var(--space-stack-md); }
.search-input {
  flex: 1; max-width: 300px; padding: 8px 12px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); font-size: var(--text-body-md); color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
}
.search-input:focus { outline: none; border-color: var(--color-primary-container); }
.search-btn { height: 38px; padding: 0 16px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); background: var(--color-surface-card); color: var(--color-on-surface); font-size: var(--text-body-md); cursor: pointer; }

.user-table { border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); overflow: hidden; }
.table-header {
  display: flex; padding: 10px 16px; background: var(--color-surface-container-low);
  font-size: var(--text-body-sm); font-weight: 600; color: var(--color-on-surface-variant);
}
.table-row {
  display: flex; align-items: center; padding: 10px 16px;
  border-top: 1px solid var(--color-border-subtle); transition: background 0.15s;
}
.table-row:hover { background: var(--color-surface-container-low); }

.col-id { width: 50px; flex-shrink: 0; font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
.col-user { flex: 2; min-width: 0; display: flex; align-items: center; gap: 6px; }
.col-email { flex: 2; min-width: 0; font-size: var(--text-body-sm); color: var(--color-on-surface-variant); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-role { width: 80px; flex-shrink: 0; }
.col-status { width: 70px; flex-shrink: 0; }
.col-time { width: 110px; flex-shrink: 0; font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
.col-actions { width: 160px; flex-shrink: 0; display: flex; gap: 4px; }

.user-link { color: var(--color-primary-container); font-weight: 500; font-size: var(--text-body-md); }
.user-link:hover { text-decoration: underline; }
.username-sub { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }

.badge {
  font-size: var(--text-label-sm); font-weight: 600; padding: 1px 8px; border-radius: 999px;
}
.admin-badge { color: var(--color-on-primary); background: var(--color-primary-container); }
.user-badge { color: var(--color-on-surface-variant); background: var(--color-surface-container); }

.status-active { color: var(--color-status-success); font-size: var(--text-body-sm); font-weight: 500; }
.status-inactive { color: var(--color-status-error); font-size: var(--text-body-sm); font-weight: 500; }

.action-btn {
  padding: 4px 10px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-sm);
  background: var(--color-surface-card); color: var(--color-on-surface); font-size: var(--text-body-sm); cursor: pointer;
  transition: border-color 0.15s;
}
.action-btn:hover { border-color: var(--color-primary-container); }
</style>
