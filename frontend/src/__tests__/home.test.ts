import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'
import type { PlanListResponse, PlanSummary } from '@/types/training'

const mocks = vi.hoisted(() => ({ plans: vi.fn(), posts: vi.fn() }))
vi.mock('@/api/training', () => ({ listPlans: mocks.plans }))
vi.mock('@/api/discussion', () => ({ listPosts: mocks.posts }))

const plan: PlanSummary = {
  id: 11,
  title: '我的动态规划训练',
  planType: 'PERSONAL',
  active: true,
  creatorUsername: 'member',
  creatorNickname: '社团成员',
  startTime: null,
  endTime: null,
  timeStatus: 'ONGOING',
  problemCount: 8,
  memberCount: 1,
  createTime: '2026-10-25T10:00:00',
}
const planResponse = (plans: PlanSummary[]): PlanListResponse => ({
  plans,
  total: plans.length,
  page: 1,
  size: 5,
})
const post = {
  id: 21,
  title: '二分答案的边界怎么处理？',
  postType: 'QUESTION',
  authorUserId: 8,
  authorUsername: 'friend',
  authorNickname: '算法伙伴',
  authorAvatarUrl: null,
  problemId: null,
  problemTitle: null,
  likeCount: 2,
  commentCount: 3,
  viewCount: 5,
  pinned: false,
  active: true,
  createTime: '2026-10-25T12:00:00',
}

async function renderHome() {
  const pinia = createPinia()
  const auth = useAuthStore(pinia)
  auth.user = {
    id: 7,
    username: 'member',
    nickname: '社团成员',
    email: null,
    avatarUrl: null,
    bio: null,
    admin: false,
  }
  const dummy = { template: '<div />' }
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: dummy },
      { path: '/training-plans', name: 'training-plans', component: dummy },
      { path: '/training-plans/create', component: dummy },
      { path: '/training-plans/:id', name: 'plan-detail', component: dummy },
      { path: '/posts', component: dummy },
      { path: '/posts/create', component: dummy },
      { path: '/posts/:id', name: 'post-detail', component: dummy },
      { path: '/problems', component: dummy },
      { path: '/problems/create', component: dummy },
      { path: '/my/problems', component: dummy },
      { path: '/leaderboard', component: dummy },
      { path: '/login', name: 'login', component: dummy },
    ],
  })
  await router.push('/')
  const wrapper = mount(HomeView, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return wrapper
}

describe('Home personal training and discussion previews', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mocks.plans.mockResolvedValue(planResponse([plan]))
    mocks.posts.mockResolvedValue({ posts: [post], total: 1, page: 1, size: 5 })
  })

  it('requests the current user’s created plans instead of public plans and shows discussion links', async () => {
    const wrapper = await renderHome()
    expect(mocks.plans).toHaveBeenCalledWith({ type: 'MY_CREATED', page: 1, size: 5 })
    expect(mocks.posts).toHaveBeenCalledWith({ page: 1, size: 5 })
    expect(wrapper.find('.plan-preview a').attributes('href')).toBe('/training-plans/11')
    expect(wrapper.find('.post-preview .post-title-link').attributes('href')).toBe('/posts/21')
    expect(wrapper.find('.post-preview .feed-avatar').attributes('href')).toBe('/users/8')
    expect(wrapper.find('.home-card').exists()).toBe(false)
    expect(wrapper.text()).toContain('我的动态规划训练')
    expect(wrapper.text()).toContain('二分答案的边界怎么处理？')
    expect(wrapper.text()).not.toContain('平台说明')
    expect(wrapper.text()).not.toContain('后续功能')
    wrapper.unmount()
  })

  it('switches to joined plans and carries that filter into the full list link', async () => {
    const wrapper = await renderHome()
    await wrapper.findAll('.plan-filters button')[1]!.trigger('click')
    await flushPromises()
    expect(mocks.plans).toHaveBeenLastCalledWith({ type: 'MY_JOINED', page: 1, size: 5 })
    expect(
      wrapper.find('[aria-labelledby="my-plans-title"] .section-link').attributes('href'),
    ).toBe('/training-plans?type=MY_JOINED')
    wrapper.unmount()
  })

  it('shows all existing discussion categories beside the title and preserves a selected category', async () => {
    const wrapper = await renderHome()
    const filters = wrapper.findAll('.discussion-filters button')
    expect(filters.map((button) => button.text())).toEqual([
      '全部',
      '题解',
      '问题求助',
      '竞赛总结',
      '训练经验',
      '公告',
      '其他',
    ])
    await filters.find((button) => button.text() === '问题求助')!.trigger('click')
    await flushPromises()
    expect(mocks.posts).toHaveBeenLastCalledWith({ page: 1, size: 5, postType: 'QUESTION' })
    expect(
      wrapper.find('[aria-labelledby="discussion-title"] .section-link').attributes('href'),
    ).toBe('/posts?postType=QUESTION')
    await filters[0]!.trigger('click')
    await flushPromises()
    expect(mocks.posts).toHaveBeenLastCalledWith({ page: 1, size: 5 })
    expect(
      wrapper.find('[aria-labelledby="discussion-title"] .section-link').attributes('href'),
    ).toBe('/posts')
    expect(mocks.plans).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })

  it('keeps discussions usable when plans fail and retries only the failed section', async () => {
    mocks.plans.mockRejectedValueOnce(new Error('offline'))
    const wrapper = await renderHome()
    expect(wrapper.find('[role="alert"]').text()).toContain('训练计划加载失败')
    expect(wrapper.find('.post-preview').text()).toContain(post.title)
    await wrapper.find('.retry-button').trigger('click')
    await flushPromises()
    expect(wrapper.find('.plan-preview').text()).toContain(plan.title)
    expect(mocks.posts).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })

  it('ignores an older response after switching plan filters', async () => {
    let finishOld!: (value: PlanListResponse) => void
    mocks.plans.mockImplementationOnce(
      () =>
        new Promise<PlanListResponse>((resolve) => {
          finishOld = resolve
        }),
    )
    mocks.plans.mockResolvedValueOnce(planResponse([{ ...plan, id: 12, title: '社团公开训练' }]))
    const wrapper = await renderHome()
    await wrapper.findAll('.plan-filters button')[1]!.trigger('click')
    await flushPromises()
    finishOld(planResponse([plan]))
    await flushPromises()
    expect(wrapper.find('.plan-preview').text()).toContain('社团公开训练')
    expect(wrapper.find('.plan-preview').text()).not.toContain(plan.title)
    wrapper.unmount()
  })
})
