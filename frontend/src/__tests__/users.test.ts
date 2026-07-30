import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'

const {
  mockGetUserProfile,
  mockGetUserProblems,
  mockGetUserTrainingPlans,
  mockUpdateProfile,
  mockUploadAvatar,
} = vi.hoisted(() => ({
  mockGetUserProfile: vi.fn(),
  mockGetUserProblems: vi.fn(),
  mockGetUserTrainingPlans: vi.fn(),
  mockUpdateProfile: vi.fn(),
  mockUploadAvatar: vi.fn(),
}))

vi.mock('@/api/users', () => ({
  getUserProfile: mockGetUserProfile,
  getUserProblems: mockGetUserProblems,
  getUserTrainingPlans: mockGetUserTrainingPlans,
  updateProfile: mockUpdateProfile,
  uploadAvatar: mockUploadAvatar,
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

function defaultProfile(overrides = {}) {
  return {
    id: 1,
    username: 'testuser',
    nickname: 'Test User',
    avatarUrl: null,
    bio: 'A bio',
    admin: false,
    accountStatus: 'ACTIVE',
    createdProblemCount: 5,
    codeforcesHandle: null,
    ojStats: null,
    createTime: '2024-01-15T00:00:00',
    ...overrides,
  }
}

function defaultPlans() {
  return { plans: [], total: 0, page: 1, size: 20 }
}

function defaultProblems() {
  return { records: [], total: 0, pages: 1, page: 1, size: 20 }
}

describe('User Profile API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should call getUserProfile with id', async () => {
    const { getUserProfile } = await import('@/api/users')
    mockGetUserProfile.mockResolvedValueOnce({ id: 1 })
    await getUserProfile(1)
    expect(mockGetUserProfile).toHaveBeenCalledWith(1)
  })

  it('should call getUserProblems with id, page, size', async () => {
    const { getUserProblems } = await import('@/api/users')
    mockGetUserProblems.mockResolvedValueOnce({ records: [], total: 0, pages: 1, page: 1, size: 20 })
    await getUserProblems(1, 1, 20)
    expect(mockGetUserProblems).toHaveBeenCalledWith(1, 1, 20)
  })

  it('should call getUserTrainingPlans with id, page, size', async () => {
    const { getUserTrainingPlans } = await import('@/api/users')
    mockGetUserTrainingPlans.mockResolvedValueOnce({ plans: [], total: 0, page: 1, size: 20 })
    await getUserTrainingPlans(1, 2, 10)
    expect(mockGetUserTrainingPlans).toHaveBeenCalledWith(1, 2, 10)
  })

  it('should call updateProfile with nickname', async () => {
    const { updateProfile } = await import('@/api/users')
    mockUpdateProfile.mockResolvedValueOnce(undefined)
    await updateProfile({ nickname: 'NewName' })
    expect(mockUpdateProfile).toHaveBeenCalledWith({ nickname: 'NewName' })
  })

  it('should call updateProfile with bio', async () => {
    const { updateProfile } = await import('@/api/users')
    mockUpdateProfile.mockResolvedValueOnce(undefined)
    await updateProfile({ bio: 'Hello' })
    expect(mockUpdateProfile).toHaveBeenCalledWith({ bio: 'Hello' })
  })

  it('should call uploadAvatar with File', async () => {
    const { uploadAvatar } = await import('@/api/users')
    mockUploadAvatar.mockResolvedValueOnce({ avatarUrl: '/uploads/t.png' })
    const file = new File(['test'], 'test.png', { type: 'image/png' })
    await uploadAvatar(file)
    expect(mockUploadAvatar).toHaveBeenCalledWith(file)
  })
})

describe('ProfileEditView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    const pinia = createPinia()
    setActivePinia(pinia)
  })

  async function mountEdit() {
    const { default: ProfileEditView } = await import('@/views/ProfileEditView.vue')
    return mount(ProfileEditView, {
      global: { plugins: [emptyRouter()], stubs: { RouterLink: true } },
    })
  }

  it('should show disabled username field', async () => {
    mockGetUser.mockResolvedValueOnce({
      id: 1, username: 'testuser', nickname: 'Test',
      email: null, avatarUrl: null, bio: null, admin: false,
    })
    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    await auth.init()

    const wrapper = await mountEdit()
    await flushPromises()

    const input = wrapper.find('input[disabled]')
    expect(input.exists()).toBe(true)
    expect((input.element as HTMLInputElement).value).toBe('testuser')
  })

  it('should show username hint', async () => {
    mockGetUser.mockResolvedValueOnce({
      id: 1, username: 'test', nickname: '',
      email: null, avatarUrl: null, bio: null, admin: false,
    })
    const { useAuthStore } = await import('@/stores/auth')
    await useAuthStore().init()

    const wrapper = await mountEdit()
    await flushPromises()

    expect(wrapper.text()).toContain('用户名不可修改')
  })

  it('should show bio character count', async () => {
    mockGetUser.mockResolvedValueOnce({
      id: 1, username: 'test', nickname: '',
      email: null, avatarUrl: null, bio: null, admin: false,
    })
    const { useAuthStore } = await import('@/stores/auth')
    await useAuthStore().init()

    const wrapper = await mountEdit()
    await flushPromises()

    expect(wrapper.text()).toContain('0/500')
  })

  it('should have save and cancel buttons', async () => {
    mockGetUser.mockResolvedValueOnce({
      id: 1, username: 'test', nickname: '',
      email: null, avatarUrl: null, bio: null, admin: false,
    })
    const { useAuthStore } = await import('@/stores/auth')
    await useAuthStore().init()

    const wrapper = await mountEdit()
    await flushPromises()

    expect(wrapper.text()).toContain('保存')
    expect(wrapper.text()).toContain('取消')
  })
})

describe('UserProfileView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    const pinia = createPinia()
    setActivePinia(pinia)
  })

  async function mountProfile(route: string) {
    const router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/users/:id', name: 'user-profile', component: { template: '<div />' } },
        { path: '/problems', name: 'problems', component: { template: '<div />' } },
        { path: '/problems/:id', name: 'problem-detail', component: { template: '<div />' } },
        { path: '/training-plans/:id', name: 'plan-detail', component: { template: '<div />' } },
        { path: '/settings/profile', name: 'profile-edit', component: { template: '<div />' } },
      ],
    })
    await router.push(route)
    await router.isReady()

    const { default: UserProfileView } = await import('@/views/UserProfileView.vue')
    return mount(UserProfileView, {
      global: { plugins: [router], stubs: { RouterLink: true } },
    })
  }

  function setupProfile(overrides = {}) {
    mockGetUserProfile.mockResolvedValueOnce(defaultProfile(overrides))
  }

  function setupProblems(data = {}) {
    mockGetUserProblems.mockResolvedValueOnce({ ...defaultProblems(), ...data })
  }

  function setupPlans(data = {}) {
    mockGetUserTrainingPlans.mockResolvedValueOnce({ ...defaultPlans(), ...data })
  }

  // --- Basic profile ---

  it('should render user profile after load', async () => {
    setupProfile()
    setupProblems()
    setupPlans()

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('Test User')
    expect(wrapper.text()).toContain('@testuser')
    expect(wrapper.text()).toContain('A bio')
    expect(wrapper.text()).toContain('5')
  })

  it('should show admin badge for admin user', async () => {
    setupProfile({ admin: true })
    setupProblems()
    setupPlans()

    const wrapper = await mountProfile('/users/2')
    await flushPromises()

    expect(wrapper.text()).toContain('管理员')
  })

  it('should show edit button when viewing self', async () => {
    mockGetUser.mockResolvedValueOnce({
      id: 1, username: 'me', nickname: 'Me',
      email: null, avatarUrl: null, bio: null, admin: false,
    })
    const { useAuthStore } = await import('@/stores/auth')
    await useAuthStore().init()

    setupProfile()
    setupProblems()
    setupPlans()

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('编辑资料')
  })

  it('should show not found when user missing', async () => {
    mockGetUserProfile.mockRejectedValueOnce({ response: { status: 404 } })

    const wrapper = await mountProfile('/users/999')
    await flushPromises()

    expect(wrapper.text()).toContain('用户不存在')
  })

  // --- Disabled banner ---

  it('should show disabled banner for disabled user', async () => {
    setupProfile({ accountStatus: 'DISABLED' })
    setupProblems()
    setupPlans()

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('账号已停用')
  })

  it('should not show disabled banner for active user', async () => {
    setupProfile({ accountStatus: 'ACTIVE' })
    setupProblems()
    setupPlans()

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).not.toContain('账号已停用')
  })

  // --- Codeforces handle ---

  it('should show Codeforces handle when present', async () => {
    setupProfile({ codeforcesHandle: 'tourist' })
    setupProblems()
    setupPlans()

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('tourist')
    expect(wrapper.text()).toContain('Codeforces')
  })

  it('should not show Codeforces section when handle is null', async () => {
    setupProfile({ codeforcesHandle: null })
    setupProblems()
    setupPlans()

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).not.toContain('Codeforces')
  })

  // --- OJ Stats ---

  it('should show OJ stats when present', async () => {
    setupProfile({
      ojStats: {
        solvedCount: 100,
        solvedCount30d: 20,
        solvedCount7d: 5,
        lastAcceptedTime: '2024-06-01T12:00:00',
      },
    })
    setupProblems()
    setupPlans()

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('刷题统计')
    expect(wrapper.text()).toContain('100')
    expect(wrapper.text()).toContain('20')
    expect(wrapper.text()).toContain('5')
    expect(wrapper.text()).toContain('总通过')
    expect(wrapper.text()).toContain('30天通过')
    expect(wrapper.text()).toContain('7天通过')
  })

  it('should not show OJ stats section when null', async () => {
    setupProfile({ ojStats: null })
    setupProblems()
    setupPlans()

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).not.toContain('刷题统计')
  })

  // --- Public problems ---

  it('should show problem list', async () => {
    setupProfile()
    setupProblems({
      records: [
        { id: 1, title: 'Problem A', platform: 'CODEFORCES', difficulty: '800', tags: null, creatorUserId: 1, creatorUsername: 'test', creatorNickname: 'Test', createTime: '2024-01-01T00:00:00' },
        { id: 2, title: 'Problem B', platform: 'ATCODER', difficulty: '1200', tags: null, creatorUserId: 1, creatorUsername: 'test', creatorNickname: 'Test', createTime: '2024-02-01T00:00:00' },
      ],
      total: 2,
    })
    setupPlans()

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('公开题目')
    expect(wrapper.text()).toContain('Problem A')
    expect(wrapper.text()).toContain('Problem B')
  })

  it('should show empty hint when no problems', async () => {
    setupProfile()
    setupProblems({ records: [], total: 0 })
    setupPlans()

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('暂无公开题目')
  })

  // --- Public plans ---

  it('should show training plan list', async () => {
    setupProfile()
    setupProblems()
    setupPlans({
      plans: [
        { id: 1, title: 'Plan X', timeStatus: 'ONGOING', startTime: '2024-01-01T00:00:00', endTime: null, problemCount: 10, memberCount: 5, createTime: '2024-01-01T00:00:00' },
        { id: 2, title: 'Plan Y', timeStatus: 'ENDED', startTime: '2024-01-01T00:00:00', endTime: '2024-06-01T00:00:00', problemCount: 20, memberCount: 8, createTime: '2024-02-01T00:00:00' },
      ],
      total: 2,
    })

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('公开训练计划')
    expect(wrapper.text()).toContain('Plan X')
    expect(wrapper.text()).toContain('Plan Y')
    expect(wrapper.text()).toContain('进行中')
    expect(wrapper.text()).toContain('已结束')
  })

  it('should show empty hint when no plans', async () => {
    setupProfile()
    setupProblems()
    setupPlans({ plans: [], total: 0 })

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('暂无公开训练计划')
  })
})
