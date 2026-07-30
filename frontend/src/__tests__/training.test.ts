import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'

const { mockListPlans, mockGetPlanDetail, mockCreatePlan, mockUpdatePlan, mockDeactivatePlan, mockRestorePlan, mockJoinPlan, mockUpdateProblemStatus } = vi.hoisted(() => ({
  mockListPlans: vi.fn(),
  mockGetPlanDetail: vi.fn(),
  mockCreatePlan: vi.fn(),
  mockUpdatePlan: vi.fn(),
  mockDeactivatePlan: vi.fn(),
  mockRestorePlan: vi.fn(),
  mockJoinPlan: vi.fn(),
  mockUpdateProblemStatus: vi.fn(),
}))

vi.mock('@/api/training', () => ({
  listPlans: mockListPlans,
  getPlanDetail: mockGetPlanDetail,
  createPlan: mockCreatePlan,
  updatePlan: mockUpdatePlan,
  deactivatePlan: mockDeactivatePlan,
  restorePlan: mockRestorePlan,
  joinPlan: mockJoinPlan,
  addPlanProblem: vi.fn(),
  removePlanProblem: vi.fn(),
  removeMember: vi.fn(),
  updateProblemStatus: mockUpdateProblemStatus,
  updateProblemNote: vi.fn(),
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

function makePlanDetail(overrides: Record<string, unknown> = {}) {
  const members = (overrides.members as Record<string, unknown>[]) || [{ userId: 1, username: 'admin', nickname: 'Admin', avatarUrl: null, joinTime: '2024-01-01T00:00:00', creator: true, completedCount: 0, totalCount: 0, requiredCompletedCount: 0, requiredTotal: 0, lastAcceptedTime: null, rank: 1, completionOrder: null }]
  const memberCount = overrides.memberCount !== undefined ? overrides.memberCount : members.length
  const joined = overrides.joined !== undefined ? overrides.joined : false
  return {
    id: 1,
    title: 'Test Plan',
    description: null,
    planType: 'PUBLIC',
    active: true,
    deactivationSource: null,
    deactivationReason: null,
    creatorUserId: 1,
    creatorUsername: 'admin',
    creatorNickname: 'Admin',
    startTime: null,
    endTime: null,
    timeStatus: 'ONGOING',
    problemCount: 0,
    memberCount,
    joined,
    creator: false,
    canEdit: false,
    canJoin: true,
    canRemoveMembers: false,
    canDeactivate: false,
    canRestore: false,
    members,
    problems: [],
    myProgress: joined ? { requiredCompletedCount: 0, requiredTotal: 0, optionalCompletedCount: 0, optionalTotal: 0 } : null,
    createTime: '2024-01-01T00:00:00',
    updateTime: '2024-01-01T00:00:00',
    ...overrides,
  }
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
    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail())
    await getPlanDetail(1)
    expect(mockGetPlanDetail).toHaveBeenCalledWith(1)
  })

  it('should call createPlan with data', async () => {
    const { createPlan } = await import('@/api/training')
    mockCreatePlan.mockResolvedValueOnce(makePlanDetail())
    await createPlan({ title: 'Test Plan' })
    expect(mockCreatePlan).toHaveBeenCalledWith({ title: 'Test Plan' })
  })

  it('should call deactivatePlan with id and reason', async () => {
    const { deactivatePlan } = await import('@/api/training')
    mockDeactivatePlan.mockResolvedValueOnce(undefined)
    await deactivatePlan(1, 'reason')
    expect(mockDeactivatePlan).toHaveBeenCalledWith(1, 'reason')
  })

  it('should call restorePlan with id', async () => {
    const { restorePlan } = await import('@/api/training')
    mockRestorePlan.mockResolvedValueOnce(undefined)
    await restorePlan(1)
    expect(mockRestorePlan).toHaveBeenCalledWith(1)
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
    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail({
      title: 'Test Plan',
      description: 'Plan desc',
      creator: true,
      joined: true,
      canEdit: true,
      memberCount: 1,
      problems: [{ id: 1, problemId: 1, problemTitle: 'Problem 1', platform: 'CUSTOM', difficulty: null, problemActive: true, sortOrder: 1, required: true }],
    }))

    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()

    expect(wrapper.text()).toContain('Test Plan')
    expect(wrapper.text()).toContain('必做')
    expect(wrapper.text()).toContain('编辑')
  })

  it('should show join button when canJoin is true', async () => {
    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail({
      id: 1, title: 'Plan', planType: 'PUBLIC',
      creatorUserId: 2, creatorUsername: 'other', creatorNickname: 'Other',
      joined: false, creator: false, canJoin: true, canEdit: false,
      memberCount: 1,
    }))
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).toContain('加入计划')
  })

  it('should show edit button when canEdit is true', async () => {
    mockGetUser.mockResolvedValueOnce({
      id: 1, username: 'me', nickname: 'Me',
      email: null, avatarUrl: null, bio: null, admin: false,
    })
    const { useAuthStore } = await import('@/stores/auth')
    await useAuthStore().init()

    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail({
      id: 1, title: 'My Plan', planType: 'PERSONAL',
      creatorUserId: 1, creatorUsername: 'me', creatorNickname: 'Me',
      joined: true, creator: true, canEdit: true, canJoin: false,
      memberCount: 1,
    }))
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).toContain('编辑')
  })

  it('should not show delete button', async () => {
    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail({
      creator: true, canEdit: true,
    }))
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.find('.delete-btn').exists()).toBe(false)
  })

  it('should show deactivate button for creator', async () => {
    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail({
      creator: true, canEdit: true, canDeactivate: true, active: true,
    }))
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).toContain('停用')
  })

  it('should show restore button when canRestore is true', async () => {
    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail({
      active: false, creator: true, canEdit: true, canRestore: true, canDeactivate: false,
    }))
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).toContain('恢复')
  })

  it('should show member list', async () => {
    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail({
      members: [
        { userId: 1, username: 'creator', nickname: 'Creator', avatarUrl: null, joinTime: '2024-01-01T00:00:00', creator: true },
        { userId: 2, username: 'member1', nickname: 'Member1', avatarUrl: null, joinTime: '2024-02-01T00:00:00', creator: false },
      ],
      memberCount: 2, joined: true, creator: true, canRemoveMembers: true,
    }))
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).toContain('成员列表')
    expect(wrapper.text()).toContain('创建者')
    expect(wrapper.text()).toContain('移除')
  })

  it('should show 404 page when plan not found', async () => {
    mockGetPlanDetail.mockRejectedValueOnce({ response: { status: 404 } })
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).toContain('计划不存在')
  })

  it('should show progress card when joined', async () => {
    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail({
      joined: true, creator: true,
      myProgress: { requiredCompletedCount: 1, requiredTotal: 2, optionalCompletedCount: 0, optionalTotal: 1 },
      members: [{ userId: 1, username: 'admin', nickname: 'Admin', avatarUrl: null, joinTime: '2024-01-01T00:00:00', creator: true, completedCount: 1, totalCount: 3, requiredCompletedCount: 1, requiredTotal: 2, lastAcceptedTime: null, rank: 1, completionOrder: null }],
    }))
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).toContain('我的进度')
    expect(wrapper.text()).toContain('1/2')
  })

  it('should show status badge on problem row', async () => {
    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail({
      joined: true,
      problems: [{ id: 1, problemId: 1, problemTitle: 'P1', platform: 'CUSTOM', difficulty: null, problemActive: true, sortOrder: 0, required: true, myStatus: 'CHALLENGING', performanceNote: 'hard' }],
      members: [{ userId: 1, username: 'admin', nickname: 'Admin', avatarUrl: null, joinTime: '2024-01-01T00:00:00', creator: false, completedCount: 0, totalCount: 1, requiredCompletedCount: 0, requiredTotal: 1, lastAcceptedTime: null, rank: 1, completionOrder: null }],
    }))
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).toContain('挑战中')
    expect(wrapper.text()).toContain('hard')
  })

  it('should show status toggle for non-ACCEPTED problems', async () => {
    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail({
      joined: true,
      problems: [{ id: 1, problemId: 1, problemTitle: 'P1', platform: 'CUSTOM', difficulty: null, problemActive: true, sortOrder: 0, required: true, myStatus: 'NOT_STARTED', performanceNote: null }],
      members: [{ userId: 1, username: 'admin', nickname: 'Admin', avatarUrl: null, joinTime: '2024-01-01T00:00:00', creator: false, completedCount: 0, totalCount: 1, requiredCompletedCount: 0, requiredTotal: 1, lastAcceptedTime: null, rank: 1, completionOrder: null }],
    }))
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).toContain('挑战中')
  })

  it('should show ACCEPTED status badge for solved problems', async () => {
    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail({
      joined: true,
      problems: [{ id: 1, problemId: 1, problemTitle: 'P1', platform: 'CUSTOM', difficulty: null, problemActive: true, sortOrder: 0, required: true, myStatus: 'ACCEPTED', performanceNote: null }],
      members: [{ userId: 1, username: 'admin', nickname: 'Admin', avatarUrl: null, joinTime: '2024-01-01T00:00:00', creator: false, completedCount: 1, totalCount: 1, requiredCompletedCount: 1, requiredTotal: 1, lastAcceptedTime: '2024-06-01T00:00:00', rank: 1, completionOrder: null }],
    }))
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).toContain('已通过')
  })

  it('should not show progress when not joined', async () => {
    mockGetPlanDetail.mockResolvedValueOnce(makePlanDetail({
      joined: false, members: [],
    }))
    const wrapper = await mountDetail('/training-plans/1')
    await flushPromises()
    expect(wrapper.text()).not.toContain('我的进度')
  })
})

describe('CreatePlanView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('should show create form with PERSONAL hint', async () => {
    mockGetUser.mockResolvedValueOnce({
      id: 1, username: 'user', nickname: 'User',
      email: null, avatarUrl: null, bio: null, admin: false,
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
    expect(wrapper.text()).toContain('仅管理员')
    expect(wrapper.text()).toContain('仅自己使用')
  })

  it('should allow admin to select PUBLIC', async () => {
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

    expect(wrapper.text()).toContain('公开计划')
    const publicRadio = wrapper.find('input[value="PUBLIC"]')
    expect((publicRadio.element as HTMLInputElement).disabled).toBe(false)
  })
})
