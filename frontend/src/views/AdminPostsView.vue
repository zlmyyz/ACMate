<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminPosts, deactivatePost, restorePost } from '@/api/admin-content'
import type { AdminPostItem } from '@/types/admin-content'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const items = ref<AdminPostItem[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
const page = ref(1)
const size = 20
const keyword = ref('')
const deactivateDialog = ref<{ id: number; title: string } | null>(null)
const deactivateReason = ref('')

async function fetch() {
  loading.value = true; error.value = ''
  try {
    const r = await getAdminPosts({ page: page.value, size, keyword: keyword.value })
    items.value = r.items; total.value = r.total
  } catch { error.value = '加载失败' }
  finally { loading.value = false }
}

async function onDeactivate(id: number) {
  const p = items.value.find(i => i.id === id)
  if (!p) return
  deactivateDialog.value = { id, title: p.title }
  deactivateReason.value = ''
}

async function confirmDeactivate() {
  if (!deactivateDialog.value || !deactivateReason.value.trim()) return
  try {
    await deactivatePost(deactivateDialog.value.id, deactivateReason.value.trim())
    deactivateDialog.value = null
    await fetch()
  } catch { /* ignore */ }
}

async function onRestore(id: number) {
  try { await restorePost(id); await fetch() } catch { /* ignore */ }
}

function onSearch() { page.value = 1; fetch() }

const typeLabels: Record<string, string> = {
  SOLUTION: '题解', QUESTION: '问题求助', CONTEST_SUMMARY: '竞赛总结',
  TRAINING_EXPERIENCE: '训练经验', ANNOUNCEMENT: '公告', OTHER: '其他',
}

onMounted(fetch)
</script>

<template>
  <PageContainer>
    <template #header><h1 class="page-title">内容管理 - 帖子</h1></template>

    <div class="search-bar">
      <input v-model="keyword" class="search-input" placeholder="搜索帖子标题..." @keyup.enter="onSearch" />
      <button class="search-btn" @click="onSearch">搜索</button>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetch" />

    <template v-else>
      <div v-if="items.length === 0" class="empty-state"><p>暂无帖子</p></div>

      <div v-else class="table">
        <div class="table-header">
          <span class="col-title">标题</span><span class="col-type">类型</span><span class="col-status">状态</span>
          <span class="col-reason">停用原因</span><span class="col-action">操作</span>
        </div>
        <div v-for="item in items" :key="item.id" class="table-row">
          <span class="col-title"><RouterLink :to="`/posts/${item.id}`" class="link">{{ item.title }}</RouterLink></span>
          <span class="col-type">{{ typeLabels[item.postType] || item.postType }}</span>
          <span class="col-status"><span :class="item.status === 1 ? 'tag-active' : 'tag-inactive'">{{ item.status === 1 ? '正常' : '已停用' }}</span></span>
          <span class="col-reason">{{ item.deactivationReason || '-' }}</span>
          <span class="col-action">
            <button v-if="item.status === 1" class="btn-danger" @click="onDeactivate(item.id)">停用</button>
            <button v-else-if="item.deactivationSource === 'ADMIN'" class="btn-restore" @click="onRestore(item.id)">恢复</button>
          </span>
        </div>
      </div>
      <PaginationBar :page="page" :size="size" :total="total" @change="p => { page = p; fetch() }" />
    </template>

    <div v-if="deactivateDialog" class="dialog-overlay" @click.self="deactivateDialog = null">
      <div class="dialog">
        <h3>停用帖子</h3>
        <p>帖子：{{ deactivateDialog.title }}</p>
        <textarea v-model="deactivateReason" class="dialog-input" placeholder="请输入停用原因（必填）" rows="3" />
        <div class="dialog-actions">
          <button class="btn-cancel" @click="deactivateDialog = null">取消</button>
          <button class="btn-danger" :disabled="!deactivateReason.trim()" @click="confirmDeactivate">确认停用</button>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.page-title { font-family: var(--font-headline); font-size: var(--text-display-lg); font-weight: 700; color: var(--color-on-surface); }
.search-bar { display: flex; gap: 8px; margin-bottom: var(--space-stack-md); }
.search-input { flex: 1; padding: 8px 12px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); font-size: var(--text-body-md); }
.search-btn { padding: 8px 16px; background: var(--color-primary-container); color: var(--color-on-primary); border-radius: var(--radius-md); font-weight: 500; }
.empty-state { text-align: center; padding: 60px 24px; color: var(--color-on-surface-variant); }
.table { border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); overflow: hidden; }
.table-header { display: flex; padding: 10px 16px; background: var(--color-surface-container-low); font-size: var(--text-body-sm); font-weight: 600; color: var(--color-on-surface-variant); }
.table-row { display: flex; padding: 10px 16px; border-top: 1px solid var(--color-border-subtle); align-items: center; }
.col-title { flex: 2; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-type { width: 90px; flex-shrink: 0; }
.col-status { width: 80px; flex-shrink: 0; text-align: center; }
.col-reason { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
.col-action { width: 80px; flex-shrink: 0; text-align: right; }
.link { color: var(--color-primary-container); }
.tag-active { color: var(--color-status-success); font-size: var(--text-label-sm); }
.tag-inactive { color: var(--color-status-error); font-size: var(--text-label-sm); }
.btn-danger { padding: 4px 10px; background: var(--color-status-error); color: #fff; border-radius: var(--radius-sm); font-size: var(--text-label-sm); }
.btn-restore { padding: 4px 10px; background: var(--color-status-success); color: #fff; border-radius: var(--radius-sm); font-size: var(--text-label-sm); }
.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 100; }
.dialog { background: var(--color-surface-card); padding: 24px; border-radius: var(--radius-lg); width: 400px; max-width: 90vw; }
.dialog h3 { margin-bottom: 12px; }
.dialog p { color: var(--color-on-surface-variant); margin-bottom: 12px; }
.dialog-input { width: 100%; padding: 8px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); resize: vertical; }
.dialog-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 12px; }
.btn-cancel { padding: 6px 16px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); }
</style>
