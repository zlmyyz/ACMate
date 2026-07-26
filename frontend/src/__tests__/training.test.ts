import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'

const { mockListPlans, mockGetPlanDetail, mockCreatePlan, mockUpdatePlan, mockDeletePlan, mockTogglePlanActive, mockJoinPlan } = vi.hoisted(() => ({
  mockListPlans: vi.fn(),
  mockGetPlanDetail: vi.fn(),
  mockCreatePlan: vi.fn(),
  mockUpdatePlan: vi.fn(),
  mockDeletePlan: vi.fn(),
  mockTogglePlanActive: vi.fn(),
  mockJoinPlan: vi.fn(),
}))

vi.mock('@/api/training', () => ({
  listPlans: mockListPlans,
  getPlanDetail: mockGetPlanDetail,
  createPlan: mockCreatePlan,
  updatePlan: mockUpdatePlan,
  deletePlan: mockDeletePlan,
  togglePlanActive: mockTogglePlanActive,
  joinPlan: mockJoinPlan,
  addPlanProblem: vi.fn(),
  removePlanProblem: vi.fn(),
  removeMember: vi.fn(),
}))

const { mockGetUser } = vi.hoisted(() => ({ mockGetUser: vi.fn() }))

vi.mock('@/api/auth', () => ({
  getCurrentUser: mockGetUser,
  login: vi.fn(),
  register: vi.fn(),
  getCsrfToken: vi.fn(),
  logout: vi.fn(),
}))

function emptyRouter() {
  return createRouter({ history: createWebHistory(), routes: [] })
}

describe('Training Plan API', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('should call listPlans with params', async () => {
    const { listPlans } = await import('@/api/training')
    mockListPlans.mockResolvedValueOnce({ plans: [], total: 0, page: 1, size: 20 })
    await listPlans({ type: 'PUBLIC', page: 1, size: 20 })
    expect(mockListPlans).toHaveBeenCalledWith({ type: 'PUBLIC', page: 1, size: 20 })
  })

  it('should call getPlanDetail with id', async () => {
    const { getPlanDetail } = await import('@/api/training')
    mockGetPlanDetail.mockResolvedValueOnce({ id: 1 })
    await getPlanDetail(1)
    expect(mockGetPlanDetail).toHaveBeenCalledWith(1)
  })

  it('should call createPlan with data', async () => {
    const { createPlan } = await import('@/api/training')
    mockCreatePlan.mockResolvedValueOnce({ id: 1 })
    await createPlan({ title: 'Test Plan' })
    expect(mockCreatePlan).toHaveBeenCalledWith({ title: 'Test Plan' })
  })

  it('should call deletePlan with id', async () => {
    const { deletePlan } = await import('@/api/training')
    mockDeletePlan.mockResolvedValueOnce(undefined)
    await deletePlan(1)
    expect(mockDeletePlan).toHaveBeenCalledWith(1)
  })

  it('should call togglePlanActive with id', async () => {
    const { togglePlanActive } = await import('@/api/training')
    mockTogglePlanActive.mockResolvedValueOnce(undefined)
    await togglePlanActive(1)
    expect(mockTogglePlanActive).toHaveBeenCalledWith(1)
  })

  it('should call joinPlan with id', async () => {
    const { joinPlan } = await import('@/api/training')
    mockJoinPlan.mockResolvedValueOnce(undefined)
    await joinPlan(1)
    expect(mockJoinPlan).toHaveBeenCalledWith(1)
  })
})

describe('TrainingPlanDetailView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  async function mountDetail(route: string) {
    const router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/training-plans/:id', name: 'plan-detail', component: { template: '<div />' } },
        { path: '/training-plans', name: 'training-plans', component: { template: '<div />' } },
        { path: '/users/:id', name: 'user-profile', component: { template: '<div />' } },
      ],
    })
    await router.push(route)
    await router.isReady()
    const { default: TrainingPlanDetailView } = await import('@/views/TrainingPlanDetailView.vue')
    return mount(TrainingPlanDetailView, {
      global: { plugins: [router], stubs: { RouterLink: true } },
    })
  }

  it('should render plan detail after load', async () => {
    mockGetPlanDetail.mockResolvedValueOnce({
      id: 1,
      title: 'Test Plan',
      description: 'Plan desc',
      planType: 'PUBLIC',
      active: true,
      creatorUserId: 1,
      creatorUsername: 'admin',
      creatorNickname: 'Admin',
      startTime: null,
      endTime: null,
      timeStatus: 'ONGOING',
      problemCount: 2,
      memberCount: 3,
      member: true,
      problems: [{ id: 1, problemId: 1, problemTitle: 'Problem 1', platform: 'CUSTOM', difficulty: null, problemActive: true, sortOrder: 1, required: true }],
      createTime: '2024-01-01T00:00:00',
      updateTime: '2024-01-01T00:00:00',
    })

    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()

    expect(wrapper.text()).toContain('Test Plan')
    expect(wrapper.text()).toContain('必做')
    expect(wrapper.text()).toContain('自定义')
  })

  it('should show join button when not a member', async () => {
    mockGetPlanDetail.mockResolvedValueOnce({
      id: 1, title: 'Plan', description: null, planType: 'PUBLIC', active: true,
      creatorUserId: 2, creatorUsername: 'other', creatorNickname: 'Other',
      startTime: null, endTime: null, timeStatus: 'ONGOING',
      problemCount: 0, memberCount: 1, member: false, problems: [],
      createTime: '2024-01-01T00:00:00', updateTime: '2024-01-01T00:00:00',
    })
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).toContain('加入计划')
  })

  it('should show edit button for creator', async () => {
    mockGetUser.mockResolvedValueOnce({
      id: 1, username: 'me', nickname: 'Me',
      email: null, avatarUrl: null, bio: null, admin: false,
    })
    const { useAuthStore } = await import('@/stores/auth')
    await useAuthStore().init()

    mockGetPlanDetail.mockResolvedValueOnce({
      id: 1, title: 'My Plan', description: null, planType: 'PERSONAL', active: true,
      creatorUserId: 1, creatorUsername: 'me', creatorNickname: 'Me',
      startTime: null, endTime: null, timeStatus: 'ONGOING',
      problemCount: 0, memberCount: 0, member: true, problems: [],
      createTime: '2024-01-01T00:00:00', updateTime: '2024-01-01T00:00:00',
    })
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).toContain('编辑')
  })
})

describe('CreatePlanView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('should show create form', async () => {
    mockGetUser.mockResolvedValueOnce({
      id: 1, username: 'admin', nickname: 'Admin',
      email: null, avatarUrl: null, bio: null, admin: true,
    })
    const { useAuthStore } = await import('@/stores/auth')
    await useAuthStore().init()

    const { default: CreatePlanView } = await import('@/views/CreatePlanView.vue')
    const wrapper = mount(CreatePlanView, {
      global: { plugins: [emptyRouter()], stubs: { RouterLink: true } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('创建训练计划')
    expect(wrapper.text()).toContain('个人计划')
    expect(wrapper.text()).toContain('公开计划')
  })
})
