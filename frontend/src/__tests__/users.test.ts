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

const {
  mockGetSocialSummary,
  mockGetFollowList,
  mockSetFollow,
  mockGetSubmissionActivity,
  mockGetRecentSubmissions,
  mockGetOjOverview,
} = vi.hoisted(() => ({
  mockGetSocialSummary: vi.fn(),
  mockGetFollowList: vi.fn(),
  mockSetFollow: vi.fn(),
  mockGetSubmissionActivity: vi.fn(),
  mockGetRecentSubmissions: vi.fn(),
  mockGetOjOverview: vi.fn(),
}))

vi.mock('@/api/profile-social', () => ({
  getSocialSummary: mockGetSocialSummary,
  getFollowList: mockGetFollowList,
  setFollow: mockSetFollow,
  getSubmissionActivity: mockGetSubmissionActivity,
  getRecentSubmissions: mockGetRecentSubmissions,
  getOjOverview: mockGetOjOverview,
}))

// 牛客面板会自行拉取接口，个人主页测试里用占位组件隔开
vi.mock('@/components/common/NowcoderProfilePanel.vue', () => ({
  default: { name: 'NowcoderProfilePanel', template: '<div />' },
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
    // 个人主页改版后依赖社交与 OJ 概览接口，默认返回空数据
    mockGetSocialSummary.mockResolvedValue({
      followingCount: 1, followerCount: 2, following: false, ipRegion: '河北',
      contributions: { views: 10, likes: 2, favorites: 1 },
    })
    mockGetFollowList.mockResolvedValue({ records: [], total: 0, page: 1, pages: 1, size: 20 })
    mockGetSubmissionActivity.mockResolvedValue({ days: [], total: 0 })
    mockGetRecentSubmissions.mockResolvedValue([])
    mockGetOjOverview.mockResolvedValue({ accounts: [], stats: null })
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

  // --- Basic profile ---

  it('should render user profile after load', async () => {
    setupProfile()

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('Test User')
    expect(wrapper.text()).toContain('testuser')
    expect(wrapper.text()).toContain('A bio')
    expect(wrapper.text()).toContain('刷题统计')
  })

  it('should show admin badge for admin user', async () => {
    setupProfile({ admin: true })

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

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    const editLink = wrapper.find('.edit-profile')
    expect(editLink.exists()).toBe(true)
    expect(editLink.attributes('to')).toBe('/settings/profile')
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

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('账号已停用')
  })

  it('should not show disabled banner for active user', async () => {
    setupProfile({ accountStatus: 'ACTIVE' })

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).not.toContain('账号已停用')
  })

  // --- Codeforces handle ---

  it('should show Codeforces handle when present', async () => {
    setupProfile({ codeforcesHandle: 'tourist' })

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('tourist')
    expect(wrapper.find('a[href="https://codeforces.com/profile/tourist"]').exists()).toBe(true)
  })

  it('should not show Codeforces section when handle is null', async () => {
    setupProfile({ codeforcesHandle: null })

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.find('a[href*="codeforces.com/profile"]').exists()).toBe(false)
  })

  // --- OJ Stats ---

  it('should show OJ stats when present', async () => {
    setupProfile()
    mockGetOjOverview.mockResolvedValue({
      accounts: [],
      stats: {
        solvedCount: 100,
        solvedCount30d: 20,
        solvedCount7d: 5,
        lastAcceptedTime: '2024-06-01T12:00:00',
      },
    })

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('刷题统计')
    expect(wrapper.text()).toContain('累计通过')
    expect(wrapper.text()).toContain('近 30 天通过')
    expect(wrapper.text()).toContain('近 7 天通过')
    expect(wrapper.text()).toContain('100')
  })

  it('should fall back to placeholder when stats are missing', async () => {
    setupProfile()

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('累计通过')
    expect(wrapper.text()).toContain('—')
    expect(wrapper.text()).toContain('暂无已同步的通过记录。')
  })
})
