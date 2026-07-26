<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminComments, deactivateComment, restoreComment } from '@/api/admin-content'
import type { AdminCommentItem } from '@/types/admin-content'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const items = ref<AdminCommentItem[]>([])
const total = ref(0)
const loading = ref(true)
const error = ref('')
const page = ref(1)
const size = 20
const deactivateDialog = ref<{ id: number; content: string } | null>(null)
const deactivateReason = ref('')

async function fetch() {
  loading.value = true; error.value = ''
  try {
    const r = await getAdminComments({ page: page.value, size })
    items.value = r.items; total.value = r.total
  } catch { error.value = '加载失败' }
  finally { loading.value = false }
}

async function onDeactivate(id: number) {
  const c = items.value.find(i => i.id === id)
  if (!c) return
  deactivateDialog.value = { id, content: c.content }
  deactivateReason.value = ''
}

async function confirmDeactivate() {
  if (!deactivateDialog.value || !deactivateReason.value.trim()) return
  try {
    await deactivateComment(deactivateDialog.value.id, deactivateReason.value.trim())
    deactivateDialog.value = null
    await fetch()
  } catch { /* ignore */ }
}

async function onRestore(id: number) {
  try { await restoreComment(id); await fetch() } catch { /* ignore */ }
}

function onPageChange(p: number) { page.value = p; fetch() }

onMounted(fetch)
</script>

<template>
  <PageContainer>
    <template #header><h1 class="page-title">内容管理 - 评论</h1></template>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetch" />

    <template v-else>
      <div v-if="items.length === 0" class="empty-state"><p>暂无评论</p></div>

      <div v-else class="table">
        <div class="table-header">
          <span class="col-id">ID</span><span class="col-post">帖子</span><span class="col-content">内容</span>
          <span class="col-status">状态</span><span class="col-action">操作</span>
        </div>
        <div v-for="item in items" :key="item.id" class="table-row">
          <span class="col-id">{{ item.id }}</span>
          <span class="col-post"><RouterLink :to="`/posts/${item.postId}`" class="link">#{{ item.postId }}</RouterLink></span>
          <span class="col-content">{{ item.content.length > 80 ? item.content.slice(0, 80) + '...' : item.content }}</span>
          <span class="col-status"><span :class="item.status === 1 ? 'tag-active' : 'tag-inactive'">{{ item.status === 1 ? '正常' : '已停用' }}</span></span>
          <span class="col-action">
            <button v-if="item.status === 1" class="btn-danger" @click="onDeactivate(item.id)">停用</button>
            <button v-else-if="item.deactivationSource === 'ADMIN'" class="btn-restore" @click="onRestore(item.id)">恢复</button>
          </span>
        </div>
      </div>
      <PaginationBar :page="page" :size="size" :total="total" @change="onPageChange" />
    </template>

    <div v-if="deactivateDialog" class="dialog-overlay" @click.self="deactivateDialog = null">
      <div class="dialog">
        <h3>停用评论</h3>
        <p class="dialog-preview">{{ deactivateDialog.content.slice(0, 200) }}</p>
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
.empty-state { text-align: center; padding: 60px 24px; color: var(--color-on-surface-variant); }
.table { border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); overflow: hidden; }
.table-header { display: flex; padding: 10px 16px; background: var(--color-surface-container-low); font-size: var(--text-body-sm); font-weight: 600; color: var(--color-on-surface-variant); }
.table-row { display: flex; padding: 10px 16px; border-top: 1px solid var(--color-border-subtle); align-items: center; }
.col-id { width: 60px; flex-shrink: 0; }
.col-post { width: 80px; flex-shrink: 0; }
.col-content { flex: 2; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-status { width: 80px; flex-shrink: 0; text-align: center; }
.col-action { width: 80px; flex-shrink: 0; text-align: right; }
.link { color: var(--color-primary-container); }
.tag-active { color: var(--color-status-success); font-size: var(--text-label-sm); }
.tag-inactive { color: var(--color-status-error); font-size: var(--text-label-sm); }
.btn-danger { padding: 4px 10px; background: var(--color-status-error); color: #fff; border-radius: var(--radius-sm); font-size: var(--text-label-sm); }
.btn-restore { padding: 4px 10px; background: var(--color-status-success); color: #fff; border-radius: var(--radius-sm); font-size: var(--text-label-sm); }
.dialog-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 100; }
.dialog { background: var(--color-surface-card); padding: 24px; border-radius: var(--radius-lg); width: 400px; max-width: 90vw; }
.dialog h3 { margin-bottom: 12px; }
.dialog-preview { color: var(--color-on-surface-variant); margin-bottom: 12px; max-height: 100px; overflow: auto; font-size: var(--text-body-sm); }
.dialog-input { width: 100%; padding: 8px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); resize: vertical; }
.dialog-actions { display: flex; gap: 8px; justify-content: flex-end; margin-top: 12px; }
.btn-cancel { padding: 6px 16px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); }
</style>
