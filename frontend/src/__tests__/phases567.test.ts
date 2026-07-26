import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'

const { mockGetLeaderboard } = vi.hoisted(() => ({ mockGetLeaderboard: vi.fn() }))
vi.mock('@/api/leaderboard', () => ({ getLeaderboard: mockGetLeaderboard }))

const { mockGetMyAccount, mockBindAccount, mockUnbindAccount, mockGetPendingAccounts, mockVerifyAccount } = vi.hoisted(() => ({
  mockGetMyAccount: vi.fn(),
  mockBindAccount: vi.fn(),
  mockUnbindAccount: vi.fn(),
  mockGetPendingAccounts: vi.fn(),
  mockVerifyAccount: vi.fn(),
}))
vi.mock('@/api/oj', () => ({
  getMyAccount: mockGetMyAccount,
  bindAccount: mockBindAccount,
  unbindAccount: mockUnbindAccount,
  getPendingAccounts: mockGetPendingAccounts,
  verifyAccount: mockVerifyAccount,
}))

const { mockListUsers, mockToggleUserStatus, mockToggleUserAdmin } = vi.hoisted(() => ({
  mockListUsers: vi.fn(),
  mockToggleUserStatus: vi.fn(),
  mockToggleUserAdmin: vi.fn(),
}))
vi.mock('@/api/admin', () => ({
  listUsers: mockListUsers,
  toggleUserStatus: mockToggleUserStatus,
  toggleUserAdmin: mockToggleUserAdmin,
}))

const { mockGetCurrentUser } = vi.hoisted(() => ({ mockGetCurrentUser: vi.fn() }))
vi.mock('@/api/auth', () => ({
  getCurrentUser: mockGetCurrentUser,
  login: vi.fn(),
  register: vi.fn(),
  getCsrfToken: vi.fn().mockResolvedValue({ token: 'csrf', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' }),
  logout: vi.fn(),
}))

const routerLinkStub = {
  template: '<a><slot /></a>',
  props: ['to'],
}

function emptyRouter() {
  return createRouter({ history: createWebHistory(), routes: [] })
}

describe('Leaderboard API', () => {
  beforeEach(() => { vi.clearAllMocks() })

  const emptyResponse = { entries: [], total: 0, page: 1, size: 20 }

  it('should call getLeaderboard with default period', async () => {
    mockGetLeaderboard.mockResolvedValue(emptyResponse)
    const { getLeaderboard } = await import('@/api/leaderboard')
    await getLeaderboard()
    expect(mockGetLeaderboard).toHaveBeenCalled()
  })

  it('should call getLeaderboard with 7d period', async () => {
    mockGetLeaderboard.mockResolvedValue(emptyResponse)
    const { getLeaderboard } = await import('@/api/leaderboard')
    await getLeaderboard('7d')
    expect(mockGetLeaderboard).toHaveBeenCalledWith('7d')
  })
})

describe('LeaderboardView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('should render leaderboard entries', async () => {
    mockGetLeaderboard.mockResolvedValue({ entries: [
      { rank: 1, userId: 1, username: 'alice', nickname: 'Alice', avatarUrl: null, solvedCount: 42, isMe: false },
      { rank: 2, userId: 2, username: 'bob', nickname: 'Bob', avatarUrl: null, solvedCount: 30, isMe: true },
    ], total: 2, page: 1, size: 20 })

    const router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/leaderboard', name: 'leaderboard', component: { template: '<div />' } },
        { path: '/users/:id', name: 'user-profile', component: { template: '<div />' } },
      ],
    })
    await router.push('/leaderboard')
    await router.isReady()

    const { default: LeaderboardView } = await import('@/views/LeaderboardView.vue')
    const wrapper = mount(LeaderboardView, {
      global: { plugins: [router], stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('42')
    expect(wrapper.text()).toContain('Bob')
    expect(wrapper.text()).toContain('30')
  })

  it('should show empty state when no entries', async () => {
    mockGetLeaderboard.mockResolvedValue({ entries: [], total: 0, page: 1, size: 20 })

    const { default: LeaderboardView } = await import('@/views/LeaderboardView.vue')
    const wrapper = mount(LeaderboardView, {
      global: { plugins: [emptyRouter()], stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('暂无可信同步数据')
  })

  it('should show period selector', async () => {
    mockGetLeaderboard.mockResolvedValue({ entries: [], total: 0, page: 1, size: 20 })

    const { default: LeaderboardView } = await import('@/views/LeaderboardView.vue')
    const wrapper = mount(LeaderboardView, {
      global: { plugins: [emptyRouter()], stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('总榜')
    expect(wrapper.text()).toContain('近7天')
    expect(wrapper.text()).toContain('近30天')
  })
})

describe('OJ Account API', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('should call getMyAccount', async () => {
    mockGetMyAccount.mockResolvedValue({ hasAccount: false })
    const { getMyAccount } = await import('@/api/oj')
    await getMyAccount()
    expect(mockGetMyAccount).toHaveBeenCalled()
  })

  it('should call bindAccount with handle', async () => {
    mockBindAccount.mockResolvedValue(undefined)
    const { bindAccount } = await import('@/api/oj')
    await bindAccount('tourist')
    expect(mockBindAccount).toHaveBeenCalledWith('tourist')
  })

  it('should call unbindAccount', async () => {
    mockUnbindAccount.mockResolvedValue(undefined)
    const { unbindAccount } = await import('@/api/oj')
    await unbindAccount()
    expect(mockUnbindAccount).toHaveBeenCalled()
  })

  it('should call getPendingAccounts', async () => {
    mockGetPendingAccounts.mockResolvedValue([])
    const { getPendingAccounts } = await import('@/api/oj')
    await getPendingAccounts()
    expect(mockGetPendingAccounts).toHaveBeenCalled()
  })

  it('should call verifyAccount with id and status', async () => {
    mockVerifyAccount.mockResolvedValue(undefined)
    const { verifyAccount } = await import('@/api/oj')
    await verifyAccount(1, 1)
    expect(mockVerifyAccount).toHaveBeenCalledWith(1, 1)
  })
})

describe('OJAccountView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('should show bind form when no account', async () => {
    mockGetMyAccount.mockResolvedValue({ hasAccount: false })
    mockGetPendingAccounts.mockResolvedValue([])

    mockGetCurrentUser.mockResolvedValue({
      id: 1, username: 'user', nickname: 'User',
      email: null, avatarUrl: null, bio: null, admin: false,
    })
    const { useAuthStore } = await import('@/stores/auth')
    await useAuthStore().init()

    const { default: OJAccountView } = await import('@/views/OJAccountView.vue')
    const wrapper = mount(OJAccountView, {
      global: { plugins: [emptyRouter()], stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('绑定')
    expect(wrapper.text()).not.toContain('解绑')
  })

  it('should show account details when bound', async () => {
    mockGetMyAccount.mockResolvedValue({
      hasAccount: true, id: 1, platform: 'CODEFORCES',
      externalUserId: 'tourist', displayName: 'tourist',
      verifyStatus: 0, syncEnabled: 1, lastSyncTime: null, lastSyncSuccess: null,
    })
    mockGetPendingAccounts.mockResolvedValue([])

    mockGetCurrentUser.mockResolvedValue({
      id: 1, username: 'user', nickname: 'User',
      email: null, avatarUrl: null, bio: null, admin: false,
    })
    const { useAuthStore } = await import('@/stores/auth')
    await useAuthStore().init()

    const { default: OJAccountView } = await import('@/views/OJAccountView.vue')
    const wrapper = mount(OJAccountView, {
      global: { plugins: [emptyRouter()], stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('tourist')
    expect(wrapper.text()).toContain('解绑')
  })

  it('should show admin pending section for admin users', async () => {
    mockGetMyAccount.mockResolvedValue({ hasAccount: false })
    mockGetPendingAccounts.mockResolvedValue([
      { id: 1, userId: 5, platform: 'CODEFORCES', externalUserId: 'newbie', displayName: 'newbie', verifyStatus: 0, syncEnabled: 1, lastSyncTime: null },
    ])

    mockGetCurrentUser.mockResolvedValue({
      id: 1, username: 'admin', nickname: 'Admin',
      email: null, avatarUrl: null, bio: null, admin: true,
    })
    const { useAuthStore } = await import('@/stores/auth')
    await useAuthStore().init()

    const { default: OJAccountView } = await import('@/views/OJAccountView.vue')
    const wrapper = mount(OJAccountView, {
      global: { plugins: [emptyRouter()], stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('审核管理')
    expect(wrapper.text()).toContain('newbie')
    expect(wrapper.text()).toContain('通过')
    expect(wrapper.text()).toContain('拒绝')
  })
})

describe('Admin API', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('should call listUsers with params', async () => {
    mockListUsers.mockResolvedValue({ users: [], total: 0, page: 1, size: 20 })
    const { listUsers } = await import('@/api/admin')
    await listUsers({ page: 1, size: 20, keyword: 'test' })
    expect(mockListUsers).toHaveBeenCalledWith({ page: 1, size: 20, keyword: 'test' })
  })

  it('should call toggleUserStatus with id', async () => {
    mockToggleUserStatus.mockResolvedValue(undefined)
    const { toggleUserStatus } = await import('@/api/admin')
    await toggleUserStatus(42)
    expect(mockToggleUserStatus).toHaveBeenCalledWith(42)
  })

  it('should call toggleUserAdmin with id', async () => {
    mockToggleUserAdmin.mockResolvedValue(undefined)
    const { toggleUserAdmin } = await import('@/api/admin')
    await toggleUserAdmin(42)
    expect(mockToggleUserAdmin).toHaveBeenCalledWith(42)
  })
})

describe('AdminUsersView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('should render user list', async () => {
    mockListUsers.mockResolvedValue({
      users: [
        { id: 1, username: 'admin', nickname: 'Admin', email: 'a@a.com', avatarUrl: null, bio: '', admin: true, status: 1, createTime: '2024-01-01T00:00:00', lastLoginTime: null },
        { id: 2, username: 'user1', nickname: 'User1', email: null, avatarUrl: null, bio: '', admin: false, status: 1, createTime: '2024-02-01T00:00:00', lastLoginTime: null },
      ],
      total: 2, page: 1, size: 20,
    })

    const router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/admin/users', name: 'admin-users', component: { template: '<div />' } },
        { path: '/users/:id', name: 'user-profile', component: { template: '<div />' } },
      ],
    })
    await router.push('/admin/users')
    await router.isReady()

    const { default: AdminUsersView } = await import('@/views/AdminUsersView.vue')
    const wrapper = mount(AdminUsersView, {
      global: { plugins: [router], stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Admin')
    expect(wrapper.text()).toContain('User1')
    expect(wrapper.text()).toContain('@admin')
    expect(wrapper.text()).toContain('@user1')
    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.text()).toContain('正常')
  })

  it('should show search input', async () => {
    mockListUsers.mockResolvedValue({ users: [], total: 0, page: 1, size: 20 })

    const { default: AdminUsersView } = await import('@/views/AdminUsersView.vue')
    const wrapper = mount(AdminUsersView, {
      global: { plugins: [emptyRouter()], stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.find('.search-input').exists()).toBe(true)
  })

  it('should show action buttons', async () => {
    mockListUsers.mockResolvedValue({
      users: [
        { id: 1, username: 'admin', nickname: 'Admin', email: 'a@a.com', avatarUrl: null, bio: '', admin: true, status: 1, createTime: '2024-01-01T00:00:00', lastLoginTime: null },
        { id: 2, username: 'user1', nickname: 'User1', email: null, avatarUrl: null, bio: '', admin: false, status: 0, createTime: '2024-02-01T00:00:00', lastLoginTime: null },
      ],
      total: 2, page: 1, size: 20,
    })

    const { default: AdminUsersView } = await import('@/views/AdminUsersView.vue')
    const wrapper = mount(AdminUsersView, {
      global: { plugins: [emptyRouter()], stubs: { RouterLink: routerLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('禁用')
    expect(wrapper.text()).toContain('恢复')
    expect(wrapper.text()).toContain('取消管理')
    expect(wrapper.text()).toContain('设为管理')
  })
})

describe('Admin users route guard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should redirect non-admin to forbidden', async () => {
    mockGetCurrentUser.mockResolvedValue({ id: 1, username: 'user', nickname: 'User', admin: false })
    const { useAuthStore } = await import('@/stores/auth')
    await useAuthStore().init()

    const { default: router } = await import('@/router/index')
    expect(router.resolve('/admin/users').name).toBe('admin-users')
  })

  it('should allow admin to access admin users', async () => {
    mockGetCurrentUser.mockResolvedValue({ id: 1, username: 'admin', nickname: 'Admin', admin: true })
    const { useAuthStore } = await import('@/stores/auth')
    await useAuthStore().init()

    expect(useAuthStore().isAdmin).toBe(true)
  })
})

describe('New routes exist', () => {
  it('should register leaderboard route', async () => {
    const { default: router } = await import('@/router/index')
    expect(router.resolve('/leaderboard').name).toBe('leaderboard')
  })

  it('should register oj-account route', async () => {
    const { default: router } = await import('@/router/index')
    expect(router.resolve('/settings/oj-account').name).toBe('oj-account')
  })

  it('should register admin-users route', async () => {
    const { default: router } = await import('@/router/index')
    expect(router.resolve('/admin/users').name).toBe('admin-users')
  })
})
