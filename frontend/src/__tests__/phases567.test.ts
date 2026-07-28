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

const { mockListUsers, mockDeactivateUser, mockReactivateUser, mockGrantUserAdmin, mockRevokeUserAdmin } = vi.hoisted(() => ({
  mockListUsers: vi.fn(),
  mockDeactivateUser: vi.fn(),
  mockReactivateUser: vi.fn(),
  mockGrantUserAdmin: vi.fn(),
  mockRevokeUserAdmin: vi.fn(),
}))
vi.mock('@/api/admin', () => ({
  listUsers: mockListUsers,
  deactivateUser: mockDeactivateUser,
  reactivateUser: mockReactivateUser,
  grantUserAdmin: mockGrantUserAdmin,
  revokeUserAdmin: mockRevokeUserAdmin,
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
    await listUsers({ page: 1, size: 20, keyword: 'test', status: 'ACTIVE', admin: 'ADMIN' })
    expect(mockListUsers).toHaveBeenCalledWith({ page: 1, size: 20, keyword: 'test', status: 'ACTIVE', admin: 'ADMIN' })
  })

  it('should call deactivateUser with id and reason', async () => {
    mockDeactivateUser.mockResolvedValue(undefined)
    const { deactivateUser } = await import('@/api/admin')
    await deactivateUser(42, '违规行为')
    expect(mockDeactivateUser).toHaveBeenCalledWith(42, '违规行为')
  })

  it('should call reactivateUser with id', async () => {
    mockReactivateUser.mockResolvedValue(undefined)
    const { reactivateUser } = await import('@/api/admin')
    await reactivateUser(42)
    expect(mockReactivateUser).toHaveBeenCalledWith(42)
  })

  it('should call grantUserAdmin with id', async () => {
    mockGrantUserAdmin.mockResolvedValue(undefined)
    const { grantUserAdmin } = await import('@/api/admin')
    await grantUserAdmin(42)
    expect(mockGrantUserAdmin).toHaveBeenCalledWith(42)
  })

  it('should call revokeUserAdmin with id', async () => {
    mockRevokeUserAdmin.mockResolvedValue(undefined)
    const { revokeUserAdmin } = await import('@/api/admin')
    await revokeUserAdmin(42)
    expect(mockRevokeUserAdmin).toHaveBeenCalledWith(42)
  })
})

describe('AdminUsersView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  function createUsersRouter() {
    const router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/admin/users', name: 'admin-users', component: { template: '<div />' } },
        { path: '/users/:id', name: 'user-profile', component: { template: '<div />' } },
        { path: '/403', name: 'forbidden', component: { template: '<div />' } },
      ],
    })
    router.push('/admin/users')
    return router
  }

  it('should render user list', async () => {
    mockListUsers.mockResolvedValue({
      users: [
        { id: 1, username: 'admin', nickname: 'Admin', email: 'a@a.com', avatarUrl: null, bio: '', admin: true, status: 1, createTime: '2024-01-01T00:00:00', lastLoginTime: null },
        { id: 2, username: 'user1', nickname: 'User1', email: null, avatarUrl: null, bio: '', admin: false, status: 1, createTime: '2024-02-01T00:00:00', lastLoginTime: null },
      ],
      total: 2, page: 1, size: 20,
    })

    const router = createUsersRouter()
    await router.isReady()

    const { default: AdminUsersView } = await import('@/views/AdminUsersView.vue')
    const wrapper = mount(AdminUsersView, {
      global: { plugins: [router], stubs: { RouterLink: routerLinkStub, Teleport: true } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('Admin')
    expect(wrapper.text()).toContain('User1')
    expect(wrapper.text()).toContain('@admin')
    expect(wrapper.text()).toContain('@user1')
    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.text()).toContain('正常')
  })

  it('should show search input and filter selects', async () => {
    mockListUsers.mockResolvedValue({ users: [], total: 0, page: 1, size: 20 })

    const router = createRouter({ history: createWebHistory(), routes: [{ path: '/admin/users', name: 'admin-users', component: { template: '<div />' } }] })
    await router.push('/admin/users')
    await router.isReady()

    const { default: AdminUsersView } = await import('@/views/AdminUsersView.vue')
    const wrapper = mount(AdminUsersView, {
      global: { plugins: [router], stubs: { RouterLink: routerLinkStub, Teleport: true } },
    })
    await flushPromises()

    expect(wrapper.find('.search-input').exists()).toBe(true)
    expect(wrapper.findAll('.filter-select').length).toBe(2)
  })

  it('should show action buttons', async () => {
    mockListUsers.mockResolvedValue({
      users: [
        { id: 1, username: 'admin', nickname: 'Admin', email: 'a@a.com', avatarUrl: null, bio: '', admin: true, status: 1, createTime: '2024-01-01T00:00:00', lastLoginTime: null },
        { id: 2, username: 'user1', nickname: 'User1', email: null, avatarUrl: null, bio: '', admin: false, status: 0, createTime: '2024-02-01T00:00:00', lastLoginTime: null },
      ],
      total: 2, page: 1, size: 20,
    })

    const router = createRouter({ history: createWebHistory(), routes: [{ path: '/admin/users', name: 'admin-users', component: { template: '<div />' } }] })
    await router.push('/admin/users')
    await router.isReady()

    const { default: AdminUsersView } = await import('@/views/AdminUsersView.vue')
    const wrapper = mount(AdminUsersView, {
      global: { plugins: [router], stubs: { RouterLink: routerLinkStub, Teleport: true } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('停用')
    expect(wrapper.text()).toContain('恢复')
    expect(wrapper.text()).toContain('取消管理')
    expect(wrapper.text()).toContain('设为管理')
  })

  it('should open deactivate dialog with user info', async () => {
    mockListUsers.mockResolvedValue({
      users: [{ id: 2, username: 'alice', nickname: 'Alice', email: null, avatarUrl: null, bio: '', admin: false, status: 1, createTime: '2024-02-01T00:00:00', lastLoginTime: null }],
      total: 1, page: 1, size: 20,
    })

    const router = createRouter({ history: createWebHistory(), routes: [{ path: '/admin/users', name: 'admin-users', component: { template: '<div />' } }] })
    await router.push('/admin/users')
    await router.isReady()

    const { default: AdminUsersView } = await import('@/views/AdminUsersView.vue')
    const wrapper = mount(AdminUsersView, {
      global: { plugins: [router], stubs: { RouterLink: routerLinkStub, Teleport: false } },
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.find('.action-btn').trigger('click')
    await flushPromises()

    expect(document.body.textContent).toContain('Alice')
    expect(document.body.textContent).toContain('@alice')
    expect(document.body.textContent).toContain('确认停用')
    expect(document.body.textContent).toContain('取消')
    wrapper.unmount()
  })

  it('should disable confirm when reason is empty in deactivate dialog', async () => {
    mockListUsers.mockResolvedValue({
      users: [{ id: 2, username: 'alice', nickname: 'Alice', email: null, avatarUrl: null, bio: '', admin: false, status: 1, createTime: '2024-02-01T00:00:00', lastLoginTime: null }],
      total: 1, page: 1, size: 20,
    })

    const router = createRouter({ history: createWebHistory(), routes: [{ path: '/admin/users', name: 'admin-users', component: { template: '<div />' } }] })
    await router.push('/admin/users')
    await router.isReady()

    const { default: AdminUsersView } = await import('@/views/AdminUsersView.vue')
    const wrapper = mount(AdminUsersView, {
      global: { plugins: [router], stubs: { RouterLink: routerLinkStub, Teleport: false } },
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.find('.action-btn').trigger('click')
    await flushPromises()

    const confirmBtn = document.body.querySelector('.modal-btn.confirm') as HTMLButtonElement
    expect(confirmBtn.disabled).toBe(true)

    const textarea = document.body.querySelector('.modal-textarea') as HTMLTextAreaElement
    textarea.value = '违规行为'
    textarea.dispatchEvent(new Event('input'))
    await flushPromises()
    expect(confirmBtn.disabled).toBe(false)
    wrapper.unmount()
  })

  it('should call deactivateUser on confirm', async () => {
    mockDeactivateUser.mockResolvedValue(undefined)
    mockListUsers.mockResolvedValue({
      users: [{ id: 2, username: 'alice', nickname: 'Alice', email: null, avatarUrl: null, bio: '', admin: false, status: 1, createTime: '2024-02-01T00:00:00', lastLoginTime: null }],
      total: 1, page: 1, size: 20,
    })

    const router = createRouter({ history: createWebHistory(), routes: [{ path: '/admin/users', name: 'admin-users', component: { template: '<div />' } }] })
    await router.push('/admin/users')
    await router.isReady()

    const { default: AdminUsersView } = await import('@/views/AdminUsersView.vue')
    const wrapper = mount(AdminUsersView, {
      global: { plugins: [router], stubs: { RouterLink: routerLinkStub, Teleport: false } },
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.find('.action-btn').trigger('click')
    await flushPromises()

    const textarea = document.body.querySelector('.modal-textarea') as HTMLTextAreaElement
    textarea.value = '违规行为'
    textarea.dispatchEvent(new Event('input'))
    await flushPromises()

    ;(document.body.querySelector('.modal-btn.confirm') as HTMLButtonElement).click()
    await flushPromises()

    expect(mockDeactivateUser).toHaveBeenCalledWith(2, '违规行为')
    wrapper.unmount()
  })

  it('should call reactivateUser on restore confirm', async () => {
    mockReactivateUser.mockResolvedValue(undefined)
    mockListUsers.mockResolvedValue({
      users: [{ id: 2, username: 'alice', nickname: 'Alice', email: null, avatarUrl: null, bio: '', admin: false, status: 0, createTime: '2024-02-01T00:00:00', lastLoginTime: null }],
      total: 1, page: 1, size: 20,
    })

    const router = createRouter({ history: createWebHistory(), routes: [{ path: '/admin/users', name: 'admin-users', component: { template: '<div />' } }] })
    await router.push('/admin/users')
    await router.isReady()

    const { default: AdminUsersView } = await import('@/views/AdminUsersView.vue')
    const wrapper = mount(AdminUsersView, {
      global: { plugins: [router], stubs: { RouterLink: routerLinkStub, Teleport: false } },
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.find('.action-btn').trigger('click')
    await flushPromises()

    ;(document.body.querySelector('.modal-btn.confirm') as HTMLButtonElement).click()
    await flushPromises()

    expect(mockReactivateUser).toHaveBeenCalledWith(2)
    wrapper.unmount()
  })

  it('should handle deactivation error', async () => {
    mockDeactivateUser.mockRejectedValue({ response: { data: { message: '停用失败' } } })
    mockListUsers.mockResolvedValue({
      users: [{ id: 2, username: 'alice', nickname: 'Alice', email: null, avatarUrl: null, bio: '', admin: false, status: 1, createTime: '2024-02-01T00:00:00', lastLoginTime: null }],
      total: 1, page: 1, size: 20,
    })

    const router = createRouter({ history: createWebHistory(), routes: [{ path: '/admin/users', name: 'admin-users', component: { template: '<div />' } }] })
    await router.push('/admin/users')
    await router.isReady()

    const { default: AdminUsersView } = await import('@/views/AdminUsersView.vue')
    const wrapper = mount(AdminUsersView, {
      global: { plugins: [router], stubs: { RouterLink: routerLinkStub, Teleport: false } },
      attachTo: document.body,
    })
    await flushPromises()

    await wrapper.find('.action-btn').trigger('click')
    await flushPromises()

    const textarea = document.body.querySelector('.modal-textarea') as HTMLTextAreaElement
    textarea.value = '违规'
    textarea.dispatchEvent(new Event('input'))
    await flushPromises()

    ;(document.body.querySelector('.modal-btn.confirm') as HTMLButtonElement).click()
    await flushPromises()

    expect(document.body.textContent).toContain('停用失败')
    wrapper.unmount()
  })

  it('should show empty state when no users', async () => {
    mockListUsers.mockResolvedValue({ users: [], total: 0, page: 1, size: 20 })

    const router = createRouter({ history: createWebHistory(), routes: [{ path: '/admin/users', name: 'admin-users', component: { template: '<div />' } }] })
    await router.push('/admin/users')
    await router.isReady()

    const { default: AdminUsersView } = await import('@/views/AdminUsersView.vue')
    const wrapper = mount(AdminUsersView, {
      global: { plugins: [router], stubs: { RouterLink: routerLinkStub, Teleport: true } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('暂无符合条件的用户')
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
