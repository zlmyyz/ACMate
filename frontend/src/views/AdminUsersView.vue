<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listUsers, deactivateUser, reactivateUser, grantUserAdmin, revokeUserAdmin } from '@/api/admin'
import type { AdminUser } from '@/types/admin'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const route = useRoute()
const router = useRouter()

const users = ref<AdminUser[]>([])
const loading = ref(true)
const error = ref('')
const total = ref(0)
const page = ref(parseInt(route.query.page as string) || 1)
const size = 20
const keyword = ref((route.query.keyword as string) || '')
const statusFilter = ref((route.query.status as string) || '')
const adminFilter = ref((route.query.admin as string) || '')

let requestId = 0

function syncUrl() {
  const q: Record<string, string> = {}
  if (page.value > 1) q.page = String(page.value)
  if (keyword.value) q.keyword = keyword.value
  if (statusFilter.value) q.status = statusFilter.value
  if (adminFilter.value) q.admin = adminFilter.value
  router.replace({ query: q })
}

async function fetchUsers() {
  const id = ++requestId
  loading.value = true; error.value = ''
  try {
    const res = await listUsers({
      page: page.value, size,
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      admin: adminFilter.value || undefined,
    })
    if (id !== requestId) return
    users.value = res.users; total.value = res.total
  } catch (e: unknown) {
    if (id !== requestId) return
    const err = e as { response?: { data?: { message?: string } } }
    error.value = err?.response?.data?.message || '加载用户列表失败'
  } finally {
    if (id === requestId) loading.value = false
  }
}

function onSearch() { page.value = 1; fetchUsers(); syncUrl() }
function onPageChange(p: number) { page.value = p; fetchUsers(); syncUrl() }

function onStatusChange() { page.value = 1; fetchUsers(); syncUrl() }
function onAdminChange() { page.value = 1; fetchUsers(); syncUrl() }

watch(() => route.query, () => {
  page.value = parseInt(route.query.page as string) || 1
  keyword.value = (route.query.keyword as string) || ''
  statusFilter.value = (route.query.status as string) || ''
  adminFilter.value = (route.query.admin as string) || ''
})

// ── Dialogs ──
const showDeactivateDialog = ref(false)
const deactivatingUser = ref<AdminUser | null>(null)
const deactivateReason = ref('')
const deactivating = ref(false)
const deactivateError = ref('')

function openDeactivate(u: AdminUser) {
  deactivatingUser.value = u
  deactivateReason.value = ''
  deactivateError.value = ''
  showDeactivateDialog.value = true
}

async function confirmDeactivate() {
  if (!deactivatingUser.value || !deactivateReason.value.trim()) return
  deactivating.value = true; deactivateError.value = ''
  try {
    await deactivateUser(deactivatingUser.value.id, deactivateReason.value.trim())
    showDeactivateDialog.value = false
    await fetchUsers()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    deactivateError.value = err?.response?.data?.message || '停用失败'
  } finally { deactivating.value = false }
}

function cancelDeactivate() { showDeactivateDialog.value = false }

const showRestoreDialog = ref(false)
const restoringUser = ref<AdminUser | null>(null)
const restoring = ref(false)
const restoreError = ref('')

function openRestore(u: AdminUser) {
  restoringUser.value = u
  restoreError.value = ''
  showRestoreDialog.value = true
}

async function confirmRestore() {
  if (!restoringUser.value) return
  restoring.value = true; restoreError.value = ''
  try {
    await reactivateUser(restoringUser.value.id)
    showRestoreDialog.value = false
    await fetchUsers()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    restoreError.value = err?.response?.data?.message || '恢复失败'
  } finally { restoring.value = false }
}

function cancelRestore() { showRestoreDialog.value = false }

const showGrantDialog = ref(false)
const grantingUser = ref<AdminUser | null>(null)
const granting = ref(false)
const grantError = ref('')

function openGrant(u: AdminUser) {
  grantingUser.value = u
  grantError.value = ''
  showGrantDialog.value = true
}

async function confirmGrant() {
  if (!grantingUser.value) return
  granting.value = true; grantError.value = ''
  try {
    await grantUserAdmin(grantingUser.value.id)
    showGrantDialog.value = false
    await fetchUsers()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    grantError.value = err?.response?.data?.message || '操作失败'
  } finally { granting.value = false }
}

function cancelGrant() { showGrantDialog.value = false }

const showRevokeDialog = ref(false)
const revokingUser = ref<AdminUser | null>(null)
const revoking = ref(false)
const revokeError = ref('')

function openRevoke(u: AdminUser) {
  revokingUser.value = u
  revokeError.value = ''
  showRevokeDialog.value = true
}

async function confirmRevoke() {
  if (!revokingUser.value) return
  revoking.value = true; revokeError.value = ''
  try {
    await revokeUserAdmin(revokingUser.value.id)
    showRevokeDialog.value = false
    await fetchUsers()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    revokeError.value = err?.response?.data?.message || '操作失败'
  } finally { revoking.value = false }
}

function cancelRevoke() { showRevokeDialog.value = false }

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
      <select v-model="statusFilter" class="filter-select" @change="onStatusChange">
        <option value="">全部状态</option>
        <option value="ACTIVE">正常</option>
        <option value="INACTIVE">已禁用</option>
      </select>
      <select v-model="adminFilter" class="filter-select" @change="onAdminChange">
        <option value="">全部角色</option>
        <option value="ADMIN">管理员</option>
        <option value="USER">普通用户</option>
      </select>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchUsers" />

    <template v-else>
      <div v-if="users.length === 0" class="empty-state">
        <p>暂无符合条件的用户</p>
      </div>

      <div v-else class="user-table">
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
            <button v-if="u.status === 1" class="action-btn" @click="openDeactivate(u)">停用</button>
            <button v-else class="action-btn" @click="openRestore(u)">恢复</button>
            <button v-if="u.admin" class="action-btn" @click="openRevoke(u)">取消管理</button>
            <button v-else class="action-btn" @click="openGrant(u)">设为管理</button>
          </span>
        </div>
      </div>

      <PaginationBar v-if="total > 0" :page="page" :total="total" :size="size" @change="onPageChange" />
    </template>

    <!-- Deactivate dialog -->
    <Teleport to="body">
      <div v-if="showDeactivateDialog" class="modal-overlay" @click.self="cancelDeactivate">
        <div class="modal">
          <h2 class="modal-title">确认停用</h2>
          <p class="modal-desc">
            确定要停用 <strong>{{ deactivatingUser?.nickname }}</strong>（@{{ deactivatingUser?.username }}）吗？
          </p>
          <div class="modal-field">
            <label class="modal-label">停用原因</label>
            <textarea v-model="deactivateReason" class="modal-textarea" placeholder="请输入停用原因..." rows="3"></textarea>
          </div>
          <p v-if="deactivateError" class="modal-error">{{ deactivateError }}</p>
          <div class="modal-actions">
            <button class="modal-btn cancel" :disabled="deactivating" @click="cancelDeactivate">取消</button>
            <button class="modal-btn confirm danger" :disabled="!deactivateReason.trim() || deactivating" @click="confirmDeactivate">
              {{ deactivating ? '停用中...' : '确认停用' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Restore dialog -->
    <Teleport to="body">
      <div v-if="showRestoreDialog" class="modal-overlay" @click.self="cancelRestore">
        <div class="modal">
          <h2 class="modal-title">确认恢复</h2>
          <p class="modal-desc">
            确定要恢复 <strong>{{ restoringUser?.nickname }}</strong>（@{{ restoringUser?.username }}）吗？<br>
            恢复后用户必须重新登录。
          </p>
          <p v-if="restoreError" class="modal-error">{{ restoreError }}</p>
          <div class="modal-actions">
            <button class="modal-btn cancel" :disabled="restoring" @click="cancelRestore">取消</button>
            <button class="modal-btn confirm" :disabled="restoring" @click="confirmRestore">
              {{ restoring ? '恢复中...' : '确认恢复' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Grant admin dialog -->
    <Teleport to="body">
      <div v-if="showGrantDialog" class="modal-overlay" @click.self="cancelGrant">
        <div class="modal">
          <h2 class="modal-title">确认设为管理员</h2>
          <p class="modal-desc">
            确定要将 <strong>{{ grantingUser?.nickname }}</strong>（@{{ grantingUser?.username }}）提升为管理员吗？<br>
            提升后该用户需重新登录以获取管理员权限。
          </p>
          <p v-if="grantError" class="modal-error">{{ grantError }}</p>
          <div class="modal-actions">
            <button class="modal-btn cancel" :disabled="granting" @click="cancelGrant">取消</button>
            <button class="modal-btn confirm" :disabled="granting" @click="confirmGrant">
              {{ granting ? '提交中...' : '确认提升' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Revoke admin dialog -->
    <Teleport to="body">
      <div v-if="showRevokeDialog" class="modal-overlay" @click.self="cancelRevoke">
        <div class="modal">
          <h2 class="modal-title">确认取消管理员</h2>
          <p class="modal-desc">
            确定要取消 <strong>{{ revokingUser?.nickname }}</strong>（@{{ revokingUser?.username }}）的管理员权限吗？<br>
            取消后该用户需重新登录，旧会话将失效。
          </p>
          <p v-if="revokeError" class="modal-error">{{ revokeError }}</p>
          <div class="modal-actions">
            <button class="modal-btn cancel" :disabled="revoking" @click="cancelRevoke">取消</button>
            <button class="modal-btn confirm danger" :disabled="revoking" @click="confirmRevoke">
              {{ revoking ? '提交中...' : '确认取消' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </PageContainer>
</template>

<style scoped>
.page-title { font-family: var(--font-headline); font-size: var(--text-display-lg); font-weight: 700; color: var(--color-on-surface); }
.filter-row { display: flex; gap: 8px; margin-bottom: var(--space-stack-md); flex-wrap: wrap; }
.search-input {
  flex: 1; min-width: 200px; max-width: 300px; padding: 8px 12px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); font-size: var(--text-body-md); color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
}
.search-input:focus { outline: none; border-color: var(--color-primary-container); }
.search-btn { height: 38px; padding: 0 16px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); background: var(--color-surface-card); color: var(--color-on-surface); font-size: var(--text-body-md); cursor: pointer; }
.filter-select {
  height: 38px; padding: 0 12px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md);
  background: var(--color-surface-card); color: var(--color-on-surface); font-size: var(--text-body-md); cursor: pointer;
}
.filter-select:focus { outline: none; border-color: var(--color-primary-container); }

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
.col-actions { width: 160px; flex-shrink: 0; display: flex; gap: 4px; flex-wrap: wrap; }

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
  transition: border-color 0.15s; white-space: nowrap;
}
.action-btn:hover { border-color: var(--color-primary-container); }

.empty-state { text-align: center; padding: 48px 16px; color: var(--color-on-surface-variant); }

.modal-overlay {
  position: fixed; inset: 0; z-index: 1000; background: rgba(0,0,0,0.4);
  display: flex; align-items: center; justify-content: center;
}
.modal {
  background: var(--color-surface-card); border-radius: var(--radius-lg); padding: 24px;
  max-width: 480px; width: 90%; box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}
.modal-title { font-size: var(--text-display-sm); font-weight: 600; margin: 0 0 12px; color: var(--color-on-surface); }
.modal-desc { font-size: var(--text-body-md); color: var(--color-on-surface-variant); margin: 0 0 16px; line-height: 1.5; }
.modal-field { margin-bottom: 12px; }
.modal-label { display: block; font-size: var(--text-body-sm); font-weight: 500; margin-bottom: 4px; color: var(--color-on-surface); }
.modal-textarea {
  width: 100%; padding: 8px 12px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md);
  font-size: var(--text-body-md); color: var(--color-on-surface); background: var(--color-surface-container-lowest);
  resize: vertical; box-sizing: border-box;
}
.modal-textarea:focus { outline: none; border-color: var(--color-primary-container); }
.modal-error { color: var(--color-status-error); font-size: var(--text-body-sm); margin-bottom: 12px; }
.modal-actions { display: flex; gap: 8px; justify-content: flex-end; }
.modal-btn {
  padding: 8px 20px; border-radius: var(--radius-md); font-size: var(--text-body-md); font-weight: 500;
  cursor: pointer; border: 1px solid var(--color-border-subtle); transition: opacity 0.15s;
}
.modal-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.modal-btn.cancel { background: var(--color-surface-container); color: var(--color-on-surface); }
.modal-btn.confirm { background: var(--color-primary-container); color: var(--color-on-primary); border-color: var(--color-primary-container); }
.modal-btn.danger { background: var(--color-status-error); color: #fff; border-color: var(--color-status-error); }
</style>
