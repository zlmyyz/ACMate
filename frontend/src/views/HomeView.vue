<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import {
  trainingTimeStatusLabels,
  trainingTypeLabels,
  postTypeLabels,
} from '@/constants/labels'
import { listPlans } from '@/api/training'
import { listPosts } from '@/api/discussion'
import type { PlanSummary } from '@/types/training'
import type { PostSummary, PostType } from '@/types/discussion'

const router = useRouter()
const planTab = ref<'MY_CREATED' | 'MY_JOINED'>('MY_CREATED')
const postCategory = ref<PostType | ''>('')
const postCategories: { value: PostType | ''; label: string }[] = [
  { value: '', label: '全部' },
  ...Object.entries(postTypeLabels).map(([value, label]) => ({ value: value as PostType, label })),
]
const plans = ref<PlanSummary[]>([])
const posts = ref<PostSummary[]>([])
const plansLoading = ref(true)
const postsLoading = ref(true)
const plansError = ref('')
const postsError = ref('')
const planTotal = ref(0)
let plansRequest = 0
let postsRequest = 0

function errorMessage(error: unknown, fallback: string) {
  const err = error as { response?: { status?: number; data?: { message?: string } } }
  if (err.response?.status === 401) {
    void router.replace({ name: 'login', query: { redirect: '/' } })
    return '登录已过期，请重新登录'
  }
  return err.response?.data?.message || fallback
}

async function fetchPlans() {
  const request = ++plansRequest
  plansLoading.value = true
  plansError.value = ''
  try {
    const result = await listPlans({ type: planTab.value, page: 1, size: 5 })
    if (request !== plansRequest) return
    plans.value = result.plans
    planTotal.value = result.total
  } catch (error) {
    if (request === plansRequest)
      plansError.value = errorMessage(error, '训练计划加载失败，请稍后重试')
  } finally {
    if (request === plansRequest) plansLoading.value = false
  }
}

async function fetchPosts() {
  const request = ++postsRequest
  postsLoading.value = true
  postsError.value = ''
  try {
    const result = await listPosts({
      page: 1,
      size: 5,
      ...(postCategory.value ? { postType: postCategory.value } : {}),
    })
    if (request === postsRequest) posts.value = result.posts
  } catch (error) {
    if (request === postsRequest) postsError.value = errorMessage(error, '讨论加载失败，请稍后重试')
  } finally {
    if (request === postsRequest) postsLoading.value = false
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString('zh-CN')
}

watch(planTab, fetchPlans)
watch(postCategory, fetchPosts)
onMounted(() => {
  void fetchPlans()
  void fetchPosts()
})
onUnmounted(() => {
  plansRequest++
  postsRequest++
})
</script>

<template>
  <PageContainer>
    <div class="home-sections">
      <section class="home-section" aria-labelledby="my-plans-title" :aria-busy="plansLoading">
        <div class="section-heading">
          <h2 id="my-plans-title" class="section-title">我的训练计划</h2>
          <div class="section-actions">
            <RouterLink
              :to="{ name: 'training-plans', query: { type: planTab } }"
              class="section-link"
              >查看全部</RouterLink
            >
            <RouterLink to="/training-plans/create" class="section-action">创建计划</RouterLink>
          </div>
        </div>
        <div class="plan-filters" aria-label="我的训练计划筛选">
          <button
            type="button"
            :class="{ selected: planTab === 'MY_CREATED' }"
            :aria-pressed="planTab === 'MY_CREATED'"
            @click="planTab = 'MY_CREATED'"
          >
            我创建的
          </button>
          <button
            type="button"
            :class="{ selected: planTab === 'MY_JOINED' }"
            :aria-pressed="planTab === 'MY_JOINED'"
            @click="planTab = 'MY_JOINED'"
          >
            我加入的
          </button>
          <span v-if="!plansLoading && !plansError" class="plan-total"
            >共 {{ planTotal }} 个计划</span
          >
        </div>
        <p v-if="plansLoading" class="section-status" role="status">正在加载训练计划…</p>
        <div v-else-if="plansError" class="section-status section-error" role="alert">
          <p>{{ plansError }}</p>
          <button type="button" class="retry-button" @click="fetchPlans">重试</button>
        </div>
        <div v-else-if="!plans.length" class="section-empty">
          <p>{{ planTab === 'MY_CREATED' ? '你还没有创建训练计划' : '你还没有加入训练计划' }}</p>
          <span>{{
            planTab === 'MY_CREATED'
              ? '制定自己的刷题目标，开始下一次进步。'
              : '加入公开计划，和社团伙伴一起训练。'
          }}</span>
          <RouterLink
            :to="planTab === 'MY_CREATED' ? '/training-plans/create' : '/training-plans'"
            class="section-link"
          >
            {{ planTab === 'MY_CREATED' ? '创建第一个计划' : '浏览公开计划' }}
          </RouterLink>
        </div>
        <ul v-else class="preview-list plan-preview">
          <li v-for="(plan, index) in plans.slice(0, 3)" :key="plan.id">
            <RouterLink
              :to="{ name: 'plan-detail', params: { id: plan.id } }"
              class="preview-row"
              :class="{ 'row-inactive': !plan.active }"
            >
              <div class="plan-art" :class="`plan-art-${index % 3}`" aria-hidden="true"><svg viewBox="0 0 64 64" fill="none"><rect x="13" y="10" width="38" height="45" rx="6" stroke="currentColor" stroke-width="3" /><path d="m22 26 5 5 11-12M22 43h20M34 32h8" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" /></svg></div>
              <div class="preview-main">
                <div class="preview-title-row">
                  <span class="preview-title">{{ plan.title }}</span>
                  <span class="item-badge">{{ trainingTypeLabels[plan.planType] }}</span>
                </div>
                <div class="preview-meta">
                  <span>{{ plan.problemCount }} 道题</span>
                  <span>{{ plan.memberCount }} 位成员</span>
                  <span>{{
                    plan.endTime ? `截止 ${formatDate(plan.endTime)}` : '不限截止时间'
                  }}</span>
                </div>
                <span class="plan-status" :class="plan.active ? plan.timeStatus.toLowerCase() : 'inactive'">{{ plan.active ? trainingTimeStatusLabels[plan.timeStatus] : '已停用' }}</span>
              </div>
            </RouterLink>
          </li>
        </ul>
      </section>

      <section class="home-section discussion-section" aria-labelledby="discussion-title" :aria-busy="postsLoading">
        <div class="section-heading">
          <div class="discussion-heading-main">
            <h2 id="discussion-title" class="section-title">讨论区</h2>
            <div class="discussion-filters" aria-label="讨论分类">
              <button
                v-for="category in postCategories"
                :key="category.value"
                type="button"
                :class="{ selected: postCategory === category.value }"
                :aria-pressed="postCategory === category.value"
                @click="postCategory = category.value"
              >
                {{ category.label }}
              </button>
            </div>
          </div>
          <div class="section-actions">
            <RouterLink
              :to="{ path: '/posts', query: postCategory ? { postType: postCategory } : {} }"
              class="section-link"
              >查看全部</RouterLink
            >
            <RouterLink to="/posts/create" class="section-action">发布讨论</RouterLink>
          </div>
        </div>
        <p v-if="postsLoading" class="section-status" role="status">正在加载最新讨论…</p>
        <div v-else-if="postsError" class="section-status section-error" role="alert">
          <p>{{ postsError }}</p>
          <button type="button" class="retry-button" @click="fetchPosts">重试</button>
        </div>
        <div v-else-if="!posts.length" class="section-empty">
          <p>{{ postCategory ? '当前分类还没有讨论' : '讨论区还没有帖子' }}</p>
          <span>分享一道题的解法，或提出你的疑问。</span>
          <RouterLink to="/posts/create" class="section-link">发布第一篇讨论</RouterLink>
        </div>
        <ul v-else class="preview-list post-preview">
          <li v-for="post in posts" :key="post.id" class="feed-post">
            <RouterLink :to="`/users/${post.authorUserId}`" class="feed-avatar" :aria-label="`${post.authorNickname || post.authorUsername}的个人主页`"><UserAvatar :src="post.authorAvatarUrl" :name="post.authorNickname || post.authorUsername" /></RouterLink>
              <div class="preview-main feed-main">
                <RouterLink :to="`/users/${post.authorUserId}`" class="feed-author">{{ post.authorNickname || post.authorUsername }}</RouterLink>
                <div class="preview-title-row">
                  <span v-if="post.pinned" class="item-badge pinned-badge">置顶</span>
                  <span class="item-badge">{{
                    postTypeLabels[post.postType] || post.postType
                  }}</span>
                  <span v-if="!post.active" class="item-badge">已停用</span>
                  <RouterLink :to="{ name: 'post-detail', params: { id: post.id } }" class="preview-title post-title-link">{{ post.title }}</RouterLink>
                </div>
                <div class="preview-meta">
                  <span>{{ formatDate(post.createTime) }}</span>
                  <span v-if="post.problemTitle" class="related-problem">{{
                    post.problemTitle
                  }}</span>
                  <span>{{ post.viewCount }} 次阅读</span>
                  <div class="post-counts">
                <span>{{ post.commentCount }} 评论</span>
                <span>{{ post.likeCount }} 赞</span>
                  </div>
                </div>
              </div>
          </li>
        </ul>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped>
.home-sections { display: flex; flex-direction: column; gap: 44px; }
.home-section { min-width: 0; padding: 0; }
.discussion-section { border-top: 1px solid color-mix(in srgb, var(--color-on-surface-variant) 40%, transparent); padding-top: 28px; }
.section-heading { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 16px; margin-bottom: 16px; }
.section-title { flex-shrink: 0; margin: 0; font: 600 22px/1.4 var(--font-headline); color: var(--color-on-surface); }
.section-actions { display: flex; align-items: center; gap: 18px; font-size: 14px; }
.section-link { color: var(--color-on-surface-variant); font-weight: 500; text-decoration: none; }.section-link:hover { color: var(--color-primary-container); }
.section-action { padding: 7px 0; color: var(--color-primary-container); font-weight: 500; text-decoration: none; }
.plan-filters { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 18px; }
.plan-filters button { padding: 5px 11px; border: 0; border-radius: 6px; background: transparent; color: var(--color-on-surface-variant); cursor: pointer; font: inherit; font-size: 13px; }
.plan-filters button.selected { background: var(--color-surface-container-low); color: var(--color-primary-container); font-weight: 600; }
.plan-total { margin-left: auto; color: var(--color-on-surface-variant); font-size: 12px; }
.preview-list { list-style: none; margin: 0; padding: 0; }
.plan-preview { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 18px; }
.plan-preview li { min-width: 0; }
.plan-preview .preview-row { display: flex; align-items: center; gap: 16px; height: 100%; padding: 14px; background: var(--color-surface-container-low); border-radius: 10px; text-decoration: none; }
.plan-preview .preview-row:hover { background: var(--color-surface-container); }
.plan-art { display: grid; place-items: center; width: 76px; height: 88px; flex-shrink: 0; border-radius: 7px; }
.plan-art svg { width: 52px; height: 52px; }.plan-art-0 { background: #daf0e8; color: #25836e; }.plan-art-1 { background: #e9e0f5; color: #8460b1; }.plan-art-2 { background: #dcebf6; color: #447fa7; }
.preview-main { flex: 1; min-width: 0; }
.preview-title-row { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; min-width: 0; margin-bottom: 10px; }
.preview-title { color: var(--color-on-surface); font-weight: 600; font-size: 17px; line-height: 1.5; text-decoration: none; }
.plan-preview .preview-title { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%; }
.plan-preview .item-badge { display: none; }
.preview-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 12px; color: var(--color-on-surface-variant); font-size: 12px; }
.plan-preview .preview-meta > span:last-child { display: none; }
.plan-status { display: inline-block; margin-top: 8px; font-size: 12px; color: var(--color-on-surface-variant); }.plan-status.ongoing { color: var(--color-primary-container); }.plan-status.not_started { color: #9a6900; }.row-inactive { opacity: .65; }
.discussion-heading-main { display: flex; flex: 1; align-items: center; flex-wrap: wrap; gap: 16px 24px; min-width: 0; }
.discussion-filters { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.discussion-filters button { padding: 10px 14px; border: 0; border-radius: 7px; background: transparent; color: var(--color-on-surface-variant); font: inherit; font-size: 15px; cursor: pointer; white-space: nowrap; }
.discussion-filters button:hover { color: var(--color-on-surface); }.discussion-filters button.selected { color: var(--color-on-surface); background: var(--color-surface-container-low); font-weight: 600; }
.discussion-section .section-heading { margin-bottom: 8px; }
.feed-post { position: relative; display: flex; align-items: flex-start; gap: 18px; padding: 28px 0; border-bottom: 1px solid var(--color-border-subtle); }
.post-title-link::after { content: ''; position: absolute; inset: 0; z-index: 1; }
.post-title-link:focus-visible::after { outline: 2px solid var(--color-primary-container); outline-offset: 3px; border-radius: 6px; }
.feed-avatar,.feed-author { position: relative; z-index: 2; }
.feed-avatar { width: 40px; height: 40px; flex-shrink: 0; border-radius: 50%; overflow: hidden; }
.feed-author { display: inline-block; margin-bottom: 10px; color: var(--color-on-surface-variant); font-size: 14px; text-decoration: none; }.feed-author:hover { color: var(--color-primary-container); }
.feed-main .preview-title-row { margin-bottom: 14px; }.feed-main .preview-title { font-size: 20px; overflow-wrap: anywhere; }.post-title-link:hover { color: var(--color-primary-container); }
.item-badge { flex-shrink: 0; padding: 2px 7px; border-radius: 4px; background: var(--color-surface-container-low); color: var(--color-on-surface-variant); font-size: 11px; }.pinned-badge { color: #986500; background: #fff5da; }
.post-counts { display: flex; align-items: center; gap: 16px; margin-left: auto; }.related-problem { max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.section-status,.section-empty { padding: 28px 0; color: var(--color-on-surface-variant); }.section-empty { display: grid; gap: 10px; }.section-empty p { margin: 0; color: var(--color-on-surface); }.section-empty > span { font-size: 14px; }.section-empty .section-link { justify-self: start; color: var(--color-primary-container); }.section-error { color: var(--color-status-critical); }.retry-button { padding: 0; margin-top: 10px; border: 0; background: transparent; color: var(--color-primary-container); cursor: pointer; text-decoration: underline; }
button:focus-visible,a:focus-visible { outline: 2px solid var(--color-primary-container); outline-offset: 3px; }
@media(max-width:1000px) { .plan-preview { grid-template-columns: repeat(2,minmax(0,1fr)); } }
@media(max-width:600px) { .home-sections { gap: 34px; }.plan-preview { grid-template-columns: minmax(0,1fr); gap: 10px; }.plan-preview > li:last-child:nth-child(3) { display: list-item; }.plan-art { width: 64px; height: 68px; }.plan-art svg { width: 44px; height: 44px; }.section-title { font-size: 20px; }.section-actions { gap: 14px; font-size: 13px; }.discussion-filters { gap: 3px; }.discussion-filters button { padding: 8px 10px; font-size: 14px; }.feed-post { padding: 22px 0; gap: 12px; }.feed-avatar { width: 34px; height: 34px; }.feed-main .preview-title { font-size: 17px; }.post-counts { margin-left: 0; }.plan-total { width: 100%; margin-left: 0; } }
</style>
