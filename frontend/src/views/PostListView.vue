<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listPosts } from '@/api/discussion'
import type { PostSummary, PostType } from '@/types/discussion'
import { postTypeLabels } from '@/constants/labels'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PaginationBar from '@/components/common/PaginationBar.vue'

const router = useRouter()
const posts = ref<PostSummary[]>([])
const loading = ref(true)
const error = ref('')
const total = ref(0)
const page = ref(1)
const size = 20
const keyword = ref('')
const postType = ref('')

const types: PostType[] = ['SOLUTION', 'QUESTION', 'CONTEST_SUMMARY', 'TRAINING_EXPERIENCE', 'ANNOUNCEMENT', 'OTHER']

async function fetchPosts() {
  loading.value = true; error.value = ''
  try {
    const res = await listPosts({ postType: postType.value, keyword: keyword.value, page: page.value, size })
    posts.value = res.posts; total.value = res.total
  } catch {
    error.value = '加载讨论列表失败'
  } finally { loading.value = false }
}

function onSearch() { page.value = 1; fetchPosts() }
function onPageChange(p: number) { page.value = p; fetchPosts() }

onMounted(fetchPosts)
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="list-header">
        <h1 class="list-title">讨论区</h1>
        <button class="create-btn" @click="router.push({ name: 'create-post' })">发布帖子</button>
      </div>
    </template>

    <div class="filter-row">
      <select v-model="postType" class="filter-select" @change="onSearch">
        <option value="">全部类型</option>
        <option v-for="t in types" :key="t" :value="t">{{ postTypeLabels[t] || t }}</option>
      </select>
      <input v-model="keyword" class="search-input" placeholder="搜索帖子..." @keyup.enter="onSearch" />
      <button class="search-btn" @click="onSearch">搜索</button>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="fetchPosts" />

    <template v-else>
      <div v-if="posts.length === 0" class="empty-state"><p>暂无帖子</p></div>

      <div v-else class="post-list">
        <div
          v-for="p in posts" :key="p.id"
          class="post-card"
          :class="{ pinned: p.pinned, inactive: !p.active }"
          @click="router.push({ name: 'post-detail', params: { id: p.id } })"
        >
          <div class="post-main">
            <div class="post-title-row">
              <span v-if="p.pinned" class="pin-badge">置顶</span>
              <span class="type-badge">{{ postTypeLabels[p.postType] || p.postType }}</span>
              <h2 class="post-title">{{ p.title }}</h2>
            </div>
            <div class="post-meta">
              <RouterLink :to="`/users/${p.authorUserId}`" class="author-link" @click.stop>
                {{ p.authorNickname || p.authorUsername }}
              </RouterLink>
              <span v-if="p.problemTitle" class="problem-ref">· {{ p.problemTitle }}</span>
              <span class="post-time">{{ new Date(p.createTime).toLocaleDateString('zh-CN') }}</span>
            </div>
          </div>
          <div class="post-stats">
            <span>{{ p.likeCount }} 赞</span>
            <span>{{ p.commentCount }} 评论</span>
            <span>{{ p.viewCount }} 浏览</span>
          </div>
        </div>
      </div>

      <PaginationBar :page="page" :total="total" :size="size" @change="onPageChange" />
    </template>
  </PageContainer>
</template>

<style scoped>
.list-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.list-title { font-family: var(--font-headline); font-size: var(--text-display-lg); font-weight: 700; color: var(--color-on-surface); }
.create-btn { height: 36px; padding: 0 20px; border: none; border-radius: var(--radius-md); background: var(--color-primary-container); color: var(--color-on-primary); font-size: var(--text-body-md); font-weight: 600; cursor: pointer; }
.create-btn:hover { opacity: 0.9; }

.filter-row { display: flex; gap: 8px; margin: var(--space-stack-md) 0; }
.filter-select { padding: 8px 12px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); font-size: var(--text-body-md); color: var(--color-on-surface); background: var(--color-surface-card); }
.search-input { flex: 1; max-width: 300px; padding: 8px 12px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); font-size: var(--text-body-md); color: var(--color-on-surface); background: var(--color-surface-container-lowest); }
.search-input:focus { outline: none; border-color: var(--color-primary-container); }
.search-btn { height: 38px; padding: 0 16px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); background: var(--color-surface-card); color: var(--color-on-surface); font-size: var(--text-body-md); cursor: pointer; }

.empty-state { text-align: center; padding: 60px 24px; color: var(--color-on-surface-variant); font-size: var(--text-body-lg); }

.post-list { display: flex; flex-direction: column; gap: 2px; }

.post-card {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; cursor: pointer;
  border-bottom: 1px solid var(--color-border-subtle);
  transition: background 0.15s;
}
.post-card:hover { background: var(--color-surface-container-low); }
.post-card.pinned { background: rgba(0,0,0,0.02); }
.post-card.inactive { opacity: 0.5; }

.post-main { flex: 1; min-width: 0; }

.post-title-row { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.pin-badge { font-size: var(--text-label-sm); font-weight: 600; color: var(--color-status-pending); background: rgba(243,161,60,0.12); padding: 1px 6px; border-radius: 999px; }
.type-badge { font-size: var(--text-label-sm); font-weight: 600; padding: 1px 6px; border-radius: 999px; color: var(--color-on-surface-variant); background: var(--color-surface-container); }
.post-title { font-size: var(--text-body-lg); font-weight: 500; color: var(--color-on-surface); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.post-meta { display: flex; gap: 12px; align-items: center; font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
.author-link { color: var(--color-primary-container); }
.post-time { margin-left: auto; }
.problem-ref { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.post-stats { display: flex; gap: 12px; flex-shrink: 0; margin-left: 16px; font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
</style>
