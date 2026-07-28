<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getPostDetail, deletePost, addComment, deleteComment, likePost, unlikePost } from '@/api/discussion'
import type { PostDetail } from '@/types/discussion'
import { postTypeLabels } from '@/constants/labels'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import MarkdownContent from '@/components/common/MarkdownContent.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const post = ref<PostDetail | null>(null)
const loading = ref(true)
const error = ref('')
const notFound = ref(false)
const commentText = ref('')
const replyTo = ref<{ id: number; username: string } | null>(null)
const commentSaving = ref(false)

const postId = computed(() => Number(route.params.id))
const canEdit = computed(() => post.value && auth.user?.id === post.value.authorUserId)

async function fetch() {
  loading.value = true; error.value = ''; notFound.value = false
  try { post.value = await getPostDetail(postId.value) }
  catch (e: unknown) {
    const err = e as { response?: { status: number } }
    if (err.response?.status === 404) { notFound.value = true; return }
    error.value = '加载失败'
  } finally { loading.value = false }
}

async function handleDelete() { if (!confirm('确认停用该帖子？')) return; await deletePost(postId.value); router.push({ name: 'posts' }) }
async function handleLike() {
  if (!post.value) return
  const wasLiked = post.value.likedByMe
  post.value.likedByMe = !wasLiked
  post.value.likeCount += wasLiked ? -1 : 1
  try {
    if (wasLiked) {
      await unlikePost(postId.value)
    } else {
      await likePost(postId.value)
    }
  } catch {
    post.value.likedByMe = wasLiked
    post.value.likeCount += wasLiked ? 1 : -1
  }
}

async function submitComment() {
  if (!commentText.value.trim()) return
  commentSaving.value = true
  try {
    await addComment(postId.value, {
      content: commentText.value.trim(),
      parentId: replyTo.value?.id,
      replyToUserId: replyTo.value?.id ? replyTo.value.id : undefined,
    })
    commentText.value = ''; replyTo.value = null; await fetch()
  } catch { /* ignore */ }
  finally { commentSaving.value = false }
}

async function handleDeleteComment(cid: number) {
  if (!confirm('确认删除该评论？')) return
  await deleteComment(postId.value, cid); await fetch()
}

function startReply(c: { id: number; username: string }) {
  replyTo.value = { id: c.id, username: c.username }
  commentText.value = `@${c.username} `
}

onMounted(fetch)
</script>

<template>
  <PageContainer>
    <template #header>
      <button class="back-link" @click="router.push({ name: 'posts' })">&larr; 返回讨论区</button>
    </template>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetch" />
    <div v-else-if="notFound" class="status-page"><h2>帖子不存在</h2></div>

    <template v-else-if="post">
      <div v-if="!post.active" class="inactive-notice">该帖子已停用</div>

      <div class="detail-card">
        <div class="header-row">
          <h1 class="post-title">
            <span class="type-badge">{{ postTypeLabels[post.postType] || post.postType }}</span>
            <span v-if="post.pinned" class="pin-badge">置顶</span>
            {{ post.title }}
          </h1>
          <div class="actions">
            <button v-if="canEdit" class="action-btn" @click="router.push({ name: 'edit-post', params: { id: postId } })">编辑</button>
            <button v-if="canEdit" class="action-btn danger" @click="handleDelete">停用</button>
          </div>
        </div>

        <div class="author-row">
          <RouterLink :to="`/users/${post.authorUserId}`" class="author-link">
            {{ post.authorNickname || post.authorUsername }}
          </RouterLink>
          <span class="meta">
            {{ new Date(post.createTime).toLocaleString('zh-CN') }}
            · {{ post.viewCount }} 次浏览
          </span>
          <RouterLink v-if="post.problemId" :to="`/problems/${post.problemId}`" class="problem-link">
            关联题目：{{ post.problemTitle }}
          </RouterLink>
        </div>

        <div class="content">
          <MarkdownContent :content="post.contentMd" />
        </div>

        <div class="like-row">
          <button class="like-btn" :class="{ liked: post.likedByMe }" @click="handleLike">
            {{ post.likedByMe ? '♥' : '♡' }} {{ post.likeCount }}
          </button>
        </div>
      </div>

      <div class="comments-section">
        <h2 class="section-title">评论（{{ post.comments.length }}）</h2>

        <div class="comment-input">
          <p v-if="replyTo" class="reply-hint">回复 @{{ replyTo.username }} <button class="cancel-reply" @click="replyTo = null; commentText = ''">取消</button></p>
          <textarea v-model="commentText" class="comment-textarea" rows="3" placeholder="写下你的评论..."></textarea>
          <button class="submit-btn" :disabled="commentSaving || !commentText.trim()" @click="submitComment">
            {{ commentSaving ? '提交中...' : '发布评论' }}
          </button>
        </div>

        <div v-if="post.comments.length === 0" class="no-comments">暂无评论</div>

        <div v-for="c in post.comments" :key="c.id" class="comment-item">
          <div class="comment-avatar">{{ (c.nickname || c.username).charAt(0).toUpperCase() }}</div>
          <div class="comment-body">
            <div class="comment-header">
              <RouterLink :to="`/users/${c.userId}`" class="comment-author">{{ c.nickname || c.username }}</RouterLink>
              <span class="comment-time">{{ new Date(c.createTime).toLocaleString('zh-CN') }}</span>
            </div>
            <div class="comment-content">{{ c.content }}</div>
            <div class="comment-actions">
              <button @click="startReply(c)">回复</button>
              <button v-if="auth.user?.id === c.userId || auth.isAdmin" class="danger" @click="handleDeleteComment(c.id)">删除</button>
            </div>

            <div v-if="c.replies && c.replies.length > 0" class="replies">
              <div v-for="r in c.replies" :key="r.id" class="reply-item">
                <div class="comment-avatar small">{{ (r.nickname || r.username).charAt(0).toUpperCase() }}</div>
                <div class="comment-body">
                  <div class="comment-header">
                    <RouterLink :to="`/users/${r.userId}`" class="comment-author">{{ r.nickname || r.username }}</RouterLink>
                    <span v-if="r.replyToUsername" class="reply-to">回复 @{{ r.replyToUsername }}</span>
                    <span class="comment-time">{{ new Date(r.createTime).toLocaleString('zh-CN') }}</span>
                  </div>
                  <div class="comment-content">{{ r.content }}</div>
                  <div class="comment-actions">
                    <button v-if="auth.user?.id === r.userId || auth.isAdmin" class="danger" @click="handleDeleteComment(r.id)">删除</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </PageContainer>
</template>

<style scoped>
.back-link { background: none; border: none; color: var(--color-primary-container); font-size: var(--text-body-md); font-weight: 500; cursor: pointer; padding: 0; }
.back-link:hover { text-decoration: underline; }

.inactive-notice { padding: 12px 20px; background: #fff3e0; border: 1px solid #ffcc02; border-radius: var(--radius-md); color: #e65100; font-size: var(--text-body-md); margin-bottom: var(--space-stack-md); }
.status-page { text-align: center; padding: 60px 24px; }
.status-page h2 { font-family: var(--font-headline); font-size: var(--text-headline-md); color: var(--color-on-surface); }

.detail-card { background: var(--color-surface-card); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); box-shadow: var(--shadow-card); padding: 24px; margin-bottom: var(--space-stack-md); }

.header-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 12px; }
.post-title { font-family: var(--font-headline); font-size: var(--text-headline-md); font-weight: 700; color: var(--color-on-surface); display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.type-badge { font-size: var(--text-label-sm); padding: 2px 8px; border-radius: 999px; background: var(--color-primary-container); color: var(--color-on-primary); }
.pin-badge { font-size: var(--text-label-sm); padding: 2px 8px; border-radius: 999px; color: var(--color-status-pending); background: rgba(243,161,60,0.12); }

.actions { display: flex; gap: 8px; flex-shrink: 0; }
.action-btn { height: 32px; padding: 0 12px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); background: transparent; font-size: var(--text-body-sm); cursor: pointer; }
.action-btn:hover { border-color: var(--color-primary-container); color: var(--color-primary-container); }
.action-btn.danger:hover { border-color: var(--color-status-critical); color: var(--color-status-critical); }

.author-row { display: flex; gap: 16px; align-items: center; font-size: var(--text-body-sm); color: var(--color-on-surface-variant); margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid var(--color-border-subtle); }
.author-link { color: var(--color-primary-container); font-weight: 500; }
.problem-link { color: var(--color-primary-container); }

.content { margin-bottom: 16px; color: var(--color-on-surface); line-height: 1.7; }

.like-row { display: flex; justify-content: center; padding-top: 12px; border-top: 1px solid var(--color-border-subtle); }
.like-btn { height: 40px; padding: 0 24px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); background: transparent; font-size: var(--text-body-lg); cursor: pointer; transition: all 0.2s; }
.like-btn.liked { color: var(--color-status-critical); border-color: var(--color-status-critical); }
.like-btn:hover { transform: scale(1.05); }

.comments-section { background: var(--color-surface-card); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); padding: 24px; }
.section-title { font-family: var(--font-headline); font-size: var(--text-headline-sm); font-weight: 600; color: var(--color-on-surface); margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--color-border-subtle); }

.comment-input { margin-bottom: 20px; }
.reply-hint { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); margin-bottom: 6px; }
.cancel-reply { background: none; border: none; color: var(--color-status-critical); cursor: pointer; font-size: var(--text-body-sm); }
.comment-textarea { width: 100%; padding: 10px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); font-size: var(--text-body-md); background: var(--color-surface-container-lowest); resize: vertical; font-family: inherit; box-sizing: border-box; }
.comment-textarea:focus { outline: none; border-color: var(--color-primary-container); }
.submit-btn { height: 32px; padding: 0 16px; margin-top: 8px; border: none; border-radius: var(--radius-md); background: var(--color-primary-container); color: var(--color-on-primary); font-size: var(--text-body-sm); font-weight: 600; cursor: pointer; }
.submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.no-comments { text-align: center; padding: 24px; color: var(--color-on-surface-variant); }

.comment-item { display: flex; gap: 12px; padding: 16px 0; border-bottom: 1px solid var(--color-border-subtle); }
.comment-avatar { width: 36px; height: 36px; border-radius: 50%; background: var(--color-surface-container-low); display: flex; align-items: center; justify-content: center; font-size: 16px; font-weight: 700; color: var(--color-on-surface-variant); flex-shrink: 0; }
.comment-avatar.small { width: 28px; height: 28px; font-size: 13px; }
.comment-body { flex: 1; min-width: 0; }
.comment-header { display: flex; gap: 8px; align-items: center; margin-bottom: 4px; }
.comment-author { font-size: var(--text-body-sm); font-weight: 600; color: var(--color-on-surface); }
.reply-to { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
.comment-time { font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
.comment-content { font-size: var(--text-body-md); color: var(--color-on-surface); white-space: pre-wrap; word-break: break-word; }
.comment-actions { margin-top: 6px; display: flex; gap: 8px; }
.comment-actions button { background: none; border: none; font-size: var(--text-body-sm); color: var(--color-on-surface-variant); cursor: pointer; padding: 0; }
.comment-actions button:hover { color: var(--color-primary-container); }
.comment-actions button.danger:hover { color: var(--color-status-critical); }

.replies { margin-top: 8px; padding: 8px 0 0 0; }
.reply-item { display: flex; gap: 10px; padding: 10px 0 0; }
</style>
