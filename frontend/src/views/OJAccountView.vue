<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getMyAccount, bindAccount, unbindAccount, getPendingAccounts, verifyAccount, syncMyAccount } from '@/api/oj'
import type { MyAccount, PendingAccount, SyncResult } from '@/types/oj'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'

const auth = useAuthStore()

const account = ref<MyAccount>({ hasAccount: false })
const loading = ref(true)
const error = ref('')
const handle = ref('')
const submitting = ref(false)
const msg = ref('')

const pendingAccounts = ref<PendingAccount[]>([])
const pendingLoading = ref(false)

const syncResult = ref<SyncResult | null>(null)
const syncing = ref(false)
const syncError = ref('')

const verifyLabels: Record<number, string> = { 0: '待审核', 1: '已通过', 2: '已拒绝' }

async function fetchMyAccount() {
  loading.value = true; error.value = ''; msg.value = ''
  try {
    account.value = await getMyAccount()
  } catch {
    error.value = '加载 OJ 账号信息失败'
  } finally { loading.value = false }
}

async function handleBind() {
  if (!handle.value.trim()) return
  submitting.value = true; msg.value = ''
  try {
    await bindAccount(handle.value.trim())
    msg.value = '绑定成功，等待管理员审核'
    handle.value = ''
    await fetchMyAccount()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    msg.value = err?.response?.data?.message || '绑定失败'
  } finally { submitting.value = false }
}

async function handleUnbind() {
  submitting.value = true; msg.value = ''
  try {
    await unbindAccount()
    msg.value = '已解绑'
    await fetchMyAccount()
  } catch {
    msg.value = '解绑失败'
  } finally { submitting.value = false }
}

async function fetchPending() {
  if (!auth.isAdmin) return
  pendingLoading.value = true
  try {
    pendingAccounts.value = await getPendingAccounts()
  } catch {
    /* ignore */
  } finally { pendingLoading.value = false }
}

async function handleVerify(id: number, status: number) {
  try {
    await verifyAccount(id, status)
    await fetchPending()
  } catch {
    msg.value = '审核操作失败'
  }
}

function canSync(): boolean {
  return account.value.hasAccount === true && account.value.verifyStatus === 1
}

function syncDisabledReason(): string {
  if (!account.value.hasAccount) return '请先绑定 Codeforces 账号'
  if (account.value.verifyStatus === 0) return '账号审核通过后才能同步'
  if (account.value.verifyStatus === 2) return '账号已被拒绝'
  return ''
}

async function handleSync() {
  if (!canSync() || syncing.value) return
  syncing.value = true; syncError.value = ''; syncResult.value = null
  try {
    syncResult.value = await syncMyAccount()
    await fetchMyAccount()
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    syncError.value = err?.response?.data?.message || '同步失败'
  } finally { syncing.value = false }
}

onMounted(() => { fetchMyAccount(); fetchPending() })
</script>

<template>
  <PageContainer>
    <template #header>
      <h1 class="page-title">OJ 账号绑定</h1>
    </template>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchMyAccount" />

    <template v-else>
      <div class="account-section">
        <h2 class="section-title">我的 Codeforces 账号</h2>

        <div v-if="account.hasAccount" class="account-card">
          <div class="account-row">
            <span class="label">平台</span>
            <span class="value">{{ account.platform }}</span>
          </div>
          <div class="account-row">
            <span class="label">Handle</span>
            <span class="value">{{ account.externalUserId }}</span>
          </div>
          <div class="account-row">
            <span class="label">审核状态</span>
            <span class="value" :class="{ pending: account.verifyStatus === 0, verified: account.verifyStatus === 1, rejected: account.verifyStatus === 2 }">
              {{ verifyLabels[account.verifyStatus!] || '未知' }}
            </span>
          </div>
          <div class="account-row" v-if="account.lastSyncTime">
            <span class="label">最后同步</span>
            <span class="value">{{ new Date(account.lastSyncTime).toLocaleString('zh-CN') }}</span>
          </div>
          <div class="sync-row">
            <button class="sync-btn" :disabled="!canSync() || syncing" @click="handleSync">
              {{ syncing ? '同步中...' : '同步' }}
            </button>
            <span v-if="!canSync()" class="sync-disabled-hint">{{ syncDisabledReason() }}</span>
          </div>
          <div v-if="syncResult && syncResult.syncStatus === 'COOLDOWN'" class="cooldown-hint">
            冷却中，请在 {{ syncResult.remainingCooldownSeconds }} 秒后重试
          </div>
          <div v-else-if="syncResult && syncResult.syncStatus === 'SUCCESS'" class="sync-result">
            <p>同步完成：获取 {{ syncResult.fetchedCount }} 条，新增 {{ syncResult.insertedCount }} 条，AC {{ syncResult.acceptedCount }} 条，首次 AC {{ syncResult.newAcceptedProblemCount }} 题</p>
          </div>
          <p v-if="syncError" class="sync-error">{{ syncError }}</p>
          <button class="unbind-btn" :disabled="submitting" @click="handleUnbind">解绑</button>
        </div>

        <div v-else class="bind-card">
          <p class="bind-hint">输入你的 Codeforces Handle 进行绑定</p>
          <div class="bind-row">
            <input v-model="handle" class="bind-input" placeholder="Codeforces Handle" @keyup.enter="handleBind" />
            <button class="bind-btn" :disabled="submitting || !handle.trim()" @click="handleBind">
              {{ submitting ? '绑定中...' : '绑定' }}
            </button>
          </div>
          <p class="bind-note">绑定后需要管理员审核，审核通过后才会同步统计数据。</p>
        </div>

        <p v-if="msg" class="form-msg">{{ msg }}</p>
      </div>

      <div v-if="auth.isAdmin" class="admin-section">
        <h2 class="section-title">审核管理</h2>
        <LoadingState v-if="pendingLoading" />
        <div v-else-if="pendingAccounts.length === 0" class="empty-state"><p>暂无待处理账号</p></div>
        <div v-else class="pending-list">
          <div v-for="acc in pendingAccounts" :key="acc.id" class="pending-row">
            <span class="pending-user">UID: {{ acc.userId }}</span>
            <span class="pending-handle">{{ acc.externalUserId }}</span>
            <span class="pending-status" :class="{ pending: acc.verifyStatus === 0, verified: acc.verifyStatus === 1, rejected: acc.verifyStatus === 2 }">
              {{ verifyLabels[acc.verifyStatus] || '未知' }}
            </span>
            <div class="pending-actions">
              <button class="verify-btn approve" @click="handleVerify(acc.id, 1)">通过</button>
              <button class="verify-btn reject" @click="handleVerify(acc.id, 2)">拒绝</button>
            </div>
          </div>
        </div>
      </div>
    </template>
  </PageContainer>
</template>

<style scoped>
.page-title { font-family: var(--font-headline); font-size: var(--text-display-lg); font-weight: 700; color: var(--color-on-surface); }
.section-title { font-size: var(--text-headline-sm); font-weight: 600; color: var(--color-on-surface); margin-bottom: 12px; }

.account-section { margin-bottom: var(--space-stack-lg); }

.account-card, .bind-card {
  border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg);
  padding: 20px; background: var(--color-surface-card);
}

.account-row {
  display: flex; align-items: center; padding: 8px 0; border-bottom: 1px solid var(--color-border-subtle);
}
.account-row:last-of-type { border-bottom: none; }
.label { width: 100px; flex-shrink: 0; color: var(--color-on-surface-variant); font-size: var(--text-body-md); }
.value { font-size: var(--text-body-md); color: var(--color-on-surface); }
.value.pending { color: var(--color-status-pending); }
.value.verified { color: var(--color-status-success); }
.value.rejected { color: var(--color-status-error); }

.unbind-btn {
  margin-top: 12px; padding: 6px 20px; border: 1px solid var(--color-status-error);
  border-radius: var(--radius-md); background: transparent; color: var(--color-status-error);
  font-size: var(--text-body-md); cursor: pointer;
}
.unbind-btn:hover { background: rgba(220,50,50,0.08); }

.sync-row { margin-top: 16px; display: flex; align-items: center; gap: 12px; }
.sync-btn {
  padding: 6px 20px; border: none; border-radius: var(--radius-md);
  background: var(--color-primary-container); color: var(--color-on-primary);
  font-size: var(--text-body-md); font-weight: 600; cursor: pointer;
}
.sync-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.sync-disabled-hint { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
.sync-result { margin-top: 8px; padding: 8px 12px; border-radius: var(--radius-md); background: rgba(0,180,100,0.08); font-size: var(--text-body-sm); color: var(--color-on-surface); }
.sync-error { margin-top: 8px; font-size: var(--text-body-sm); color: var(--color-status-error); }
.cooldown-hint { margin-top: 8px; padding: 8px 12px; border-radius: var(--radius-md); background: rgba(255,180,0,0.1); font-size: var(--text-body-sm); color: var(--color-status-pending); }

.bind-hint { color: var(--color-on-surface-variant); font-size: var(--text-body-md); margin-bottom: 12px; }
.bind-row { display: flex; gap: 8px; margin-bottom: 12px; }
.bind-input {
  flex: 1; max-width: 300px; padding: 8px 12px; border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md); font-size: var(--text-body-md); color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
}
.bind-input:focus { outline: none; border-color: var(--color-primary-container); }
.bind-btn {
  height: 38px; padding: 0 20px; border: none; border-radius: var(--radius-md);
  background: var(--color-primary-container); color: var(--color-on-primary);
  font-size: var(--text-body-md); font-weight: 600; cursor: pointer;
}
.bind-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.bind-note { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
.form-msg { margin-top: 8px; font-size: var(--text-body-md); color: var(--color-on-surface-variant); }

.admin-section {
  border-top: 1px solid var(--color-border-subtle); padding-top: var(--space-stack-lg);
}
.empty-state { text-align: center; padding: 24px; color: var(--color-on-surface-variant); font-size: var(--text-body-md); }

.pending-list { display: flex; flex-direction: column; gap: 1px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); overflow: hidden; }
.pending-row {
  display: flex; align-items: center; gap: 16px; padding: 12px 16px;
  background: var(--color-surface-card); border-bottom: 1px solid var(--color-border-subtle);
}
.pending-row:last-child { border-bottom: none; }
.pending-user { font-size: var(--text-body-md); color: var(--color-on-surface-variant); width: 80px; }
.pending-handle { font-size: var(--text-body-md); color: var(--color-on-surface); font-weight: 500; flex: 1; }
.pending-status { font-size: var(--text-body-sm); font-weight: 600; width: 70px; }
.pending-status.pending { color: var(--color-status-pending); }
.pending-status.verified { color: var(--color-status-success); }
.pending-status.rejected { color: var(--color-status-error); }
.pending-actions { display: flex; gap: 4px; }
.verify-btn {
  padding: 4px 12px; border: none; border-radius: var(--radius-sm); font-size: var(--text-body-sm); font-weight: 600; cursor: pointer;
}
.verify-btn.approve { background: rgba(0,180,100,0.12); color: var(--color-status-success); }
.verify-btn.approve:hover { background: rgba(0,180,100,0.2); }
.verify-btn.reject { background: rgba(220,50,50,0.08); color: var(--color-status-error); }
.verify-btn.reject:hover { background: rgba(220,50,50,0.15); }
</style>
