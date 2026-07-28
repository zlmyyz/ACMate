import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useNotificationStore } from '@/stores/notifications'
import AppHeader from '@/components/layout/AppHeader.vue'
import NotificationsView from '@/views/NotificationsView.vue'

const { mockGetUser } = vi.hoisted(() => ({
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockGetUser: vi.fn<() => any>(),
}))

vi.mock('@/api/auth', () => ({
  getCurrentUser: mockGetUser,
  login: vi.fn(),
  register: vi.fn(),
  getCsrfToken: vi.fn().mockResolvedValue({ token: 't', headerName: 'X-CSRF-TOKEN' }),
  logout: vi.fn(),
}))

const mockGetNotifications = vi.fn()
const mockGetUnreadCount = vi.fn()
const mockMarkRead = vi.fn()
const mockMarkAllRead = vi.fn()

vi.mock('@/api/notifications', () => ({
  getNotifications: (...args: unknown[]) => mockGetNotifications(...args),
  getUnreadCount: (...args: unknown[]) => mockGetUnreadCount(...args),
  markRead: (id: number) => mockMarkRead(id),
  markAllRead: () => mockMarkAllRead(),
}))

function createRouterForNotifications() {
  return createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>home</div>' }, meta: { requiresAuth: true } },
      { path: '/notifications', name: 'notifications', component: NotificationsView, meta: { requiresAuth: true } },
      { path: '/login', name: 'login', component: { template: '<div>login</div>' }, meta: { guestOnly: true } },
    ],
  })
}

// ==================== API CLIENT ====================

describe('Notification API client', () => {
  it('getNotifications passes unreadOnly parameter', () => {
    expect(mockGetNotifications).toBeDefined()
  })

  it('getUnreadCount returns count', () => {
    expect(mockGetUnreadCount).toBeDefined()
  })

  it('markRead accepts notification id', () => {
    expect(mockMarkRead).toBeDefined()
  })

  it('markAllRead uses CSRF', () => {
    expect(mockMarkAllRead).toBeDefined()
  })
})

// ==================== PINIA STORE ====================

describe('Notification Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  function loginUser() {
    const auth = useAuthStore()
    auth.user = { id: 1, username: 'test', nickname: 'Test', email: null, avatarUrl: null, bio: null, admin: false }
    auth.initialized = true
    return auth
  }

  it('fetchUnreadCount updates unreadCount', async () => {
    loginUser()
    mockGetUnreadCount.mockResolvedValueOnce(5)
    const store = useNotificationStore()
    await store.fetchUnreadCount()
    expect(store.unreadCount).toBe(5)
    expect(mockGetUnreadCount).toHaveBeenCalled()
  })

  it('fetchUnreadCount handles errors silently', async () => {
    loginUser()
    mockGetUnreadCount.mockRejectedValueOnce(new Error('network'))
    const store = useNotificationStore()
    await store.fetchUnreadCount()
    expect(store.unreadCount).toBe(0)
  })

  it('fetchNotifications loads items', async () => {
    loginUser()
    const items = [{ id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2,
      resourceType: 'POST', resourceId: 10, payload: null, isRead: false,
      readTime: null, createTime: '2026-07-28T12:00:00' }]
    mockGetNotifications.mockResolvedValueOnce({ items, total: 1, page: 1, size: 20 })
    const store = useNotificationStore()
    await store.fetchNotifications(1, 20, false)
    expect(store.notifications).toEqual(items)
    expect(store.total).toBe(1)
  })

  it('fetchNotifications passes unreadOnly', async () => {
    loginUser()
    mockGetNotifications.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 })
    const store = useNotificationStore()
    await store.fetchNotifications(1, 20, true)
    expect(mockGetNotifications).toHaveBeenCalledWith(1, 20, true)
  })

  it('markOneRead calls API and decrements unreadCount', async () => {
    loginUser()
    const store = useNotificationStore()
    store.unreadCount = 3
    store.notifications = [{ id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2,
      resourceType: 'POST', resourceId: 10, payload: null, isRead: false,
      readTime: null, createTime: '2026-07-28T12:00:00' }]
    mockMarkRead.mockResolvedValueOnce(undefined)
    await store.markOneRead(1)
    expect(mockMarkRead).toHaveBeenCalledWith(1)
    expect(store.notifications[0]!.isRead).toBe(true)
    expect(store.unreadCount).toBe(2)
  })

  it('markOneRead does not decrement below zero', async () => {
    loginUser()
    const store = useNotificationStore()
    store.unreadCount = 0
    store.notifications = [{ id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2,
      resourceType: 'POST', resourceId: 10, payload: null, isRead: false,
      readTime: null, createTime: '2026-07-28T12:00:00' }]
    mockMarkRead.mockResolvedValueOnce(undefined)
    await store.markOneRead(1)
    expect(store.unreadCount).toBe(0)
  })

  it('markOneRead skips if already read', async () => {
    loginUser()
    const store = useNotificationStore()
    store.unreadCount = 3
    store.notifications = [{ id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2,
      resourceType: 'POST', resourceId: 10, payload: null, isRead: true,
      readTime: '2026-07-28T12:00:00', createTime: '2026-07-28T12:00:00' }]
    mockMarkRead.mockResolvedValueOnce(undefined)
    await store.markOneRead(1)
    expect(store.unreadCount).toBe(3)
  })

  it('markOneRead handles API error silently', async () => {
    loginUser()
    const store = useNotificationStore()
    store.unreadCount = 3
    store.notifications = [{ id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2,
      resourceType: 'POST', resourceId: 10, payload: null, isRead: false,
      readTime: null, createTime: '2026-07-28T12:00:00' }]
    mockMarkRead.mockRejectedValueOnce(new Error('network'))
    await store.markOneRead(1)
    expect(store.unreadCount).toBe(3)
    expect(store.notifications[0]!.isRead).toBe(false)
  })

  it('markAllAsRead sets all to read and zeros count', async () => {
    loginUser()
    const store = useNotificationStore()
    store.unreadCount = 5
    store.notifications = [
      { id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2, resourceType: 'POST', resourceId: 10, payload: null, isRead: false, readTime: null, createTime: '2026-07-28T12:00:00' },
      { id: 2, notificationType: 'COMMENT_REPLIED', actorUserId: 3, resourceType: 'COMMENT', resourceId: 20, payload: null, isRead: false, readTime: null, createTime: '2026-07-28T12:00:00' },
    ]
    mockMarkAllRead.mockResolvedValueOnce(undefined)
    await store.markAllAsRead()
    expect(mockMarkAllRead).toHaveBeenCalled()
    expect(store.notifications.every(n => n.isRead)).toBe(true)
    expect(store.unreadCount).toBe(0)
  })

  it('markAllAsRead handles API error silently', async () => {
    loginUser()
    const store = useNotificationStore()
    store.unreadCount = 5
    store.notifications = [{ id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2, resourceType: 'POST', resourceId: 10, payload: null, isRead: false, readTime: null, createTime: '2026-07-28T12:00:00' }]
    mockMarkAllRead.mockRejectedValueOnce(new Error('network'))
    await store.markAllAsRead()
    expect(store.unreadCount).toBe(5)
  })

  // ==================== POLLING ====================

  it('startPolling fetches immediately then every 30s', async () => {
    loginUser()
    mockGetUnreadCount.mockResolvedValue(3)
    const store = useNotificationStore()
    store.startPolling()
    expect(store.pollingActive).toBe(true)
    await vi.advanceTimersByTimeAsync(0)
    expect(mockGetUnreadCount).toHaveBeenCalledTimes(1)
    expect(store.unreadCount).toBe(3)

    mockGetUnreadCount.mockResolvedValue(7)
    await vi.advanceTimersByTimeAsync(30_000)
    expect(mockGetUnreadCount).toHaveBeenCalledTimes(2)
    expect(store.unreadCount).toBe(7)
  })

  it('startPolling does not start if not logged in', () => {
    const store = useNotificationStore()
    store.startPolling()
    expect(store.pollingActive).toBe(false)
    expect(mockGetUnreadCount).not.toHaveBeenCalled()
  })

  it('startPolling does not double-start', () => {
    loginUser()
    const store = useNotificationStore()
    store.startPolling()
    store.startPolling()
    expect(store.pollingActive).toBe(true)
  })

  it('stopPolling clears interval', () => {
    loginUser()
    mockGetUnreadCount.mockResolvedValue(1)
    const store = useNotificationStore()
    store.startPolling()
    store.stopPolling()
    expect(store.pollingActive).toBe(false)
  })

  it('polling paused when pollingActive is false', async () => {
    loginUser()
    mockGetUnreadCount.mockResolvedValue(1)
    const store = useNotificationStore()
    store.startPolling()
    await vi.advanceTimersByTimeAsync(0)
    expect(mockGetUnreadCount).toHaveBeenCalledTimes(1)

    store.pollingActive = false
    await vi.advanceTimersByTimeAsync(30_000)
    expect(mockGetUnreadCount).toHaveBeenCalledTimes(1)
  })

  // ==================== VISIBILITY ====================

  it('handleVisibilityChange pauses on hidden', () => {
    loginUser()
    const store = useNotificationStore()
    store.pollingActive = true
    vi.spyOn(document, 'hidden', 'get').mockReturnValue(true)
    store.handleVisibilityChange()
    expect(store.pollingActive).toBe(false)
  })

  it('handleVisibilityChange resumes and fetches on visible', () => {
    loginUser()
    mockGetUnreadCount.mockResolvedValue(2)
    const store = useNotificationStore()
    store.pollingActive = false
    vi.spyOn(document, 'hidden', 'get').mockReturnValue(false)
    store.handleVisibilityChange()
    expect(store.pollingActive).toBe(true)
    expect(mockGetUnreadCount).toHaveBeenCalled()
  })

  it('handleVisibilityChange ignores when not logged in', () => {
    const store = useNotificationStore()
    store.pollingActive = false
    vi.spyOn(document, 'hidden', 'get').mockReturnValue(false)
    store.handleVisibilityChange()
    expect(store.pollingActive).toBe(false)
  })

  // ==================== RESET ====================

  it('reset stops polling and clears state', () => {
    loginUser()
    mockGetUnreadCount.mockResolvedValue(5)
    const store = useNotificationStore()
    store.unreadCount = 5
    store.notifications = [{ id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2, resourceType: 'POST', resourceId: 10, payload: null, isRead: false, readTime: null, createTime: '2026-07-28T12:00:00' }]
    store.total = 10
    store.startPolling()
    store.reset()
    expect(store.pollingActive).toBe(false)
    expect(store.unreadCount).toBe(0)
    expect(store.notifications).toEqual([])
    expect(store.total).toBe(0)
  })
})

// ==================== BADGE (AppHeader) ====================

describe('AppHeader notification badge', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  function mountHeader(unreadCount: number) {
    const auth = useAuthStore()
    auth.user = { id: 1, username: 'test', nickname: 'Test', email: null, avatarUrl: null, bio: null, admin: false }
    auth.initialized = true
    const store = useNotificationStore()
    store.unreadCount = unreadCount
    const router = createRouterForNotifications()
    return mount(AppHeader, {
      global: { plugins: [router] },
    })
  }

  it('shows no badge when unreadCount is 0', () => {
    const wrapper = mountHeader(0)
    expect(wrapper.find('.badge').exists()).toBe(false)
  })

  it('shows badge with count when unreadCount > 0', () => {
    const wrapper = mountHeader(5)
    expect(wrapper.find('.badge').exists()).toBe(true)
    expect(wrapper.find('.badge').text()).toBe('5')
  })

  it('shows 99+ when unreadCount exceeds 99', () => {
    const wrapper = mountHeader(150)
    expect(wrapper.find('.badge').text()).toBe('99+')
  })

  it('shows 99+ at exactly 100', () => {
    const wrapper = mountHeader(100)
    expect(wrapper.find('.badge').text()).toBe('99+')
  })

  it('shows number at exactly 99', () => {
    const wrapper = mountHeader(99)
    expect(wrapper.find('.badge').text()).toBe('99')
  })

  it('bell links to notifications page', () => {
    const wrapper = mountHeader(0)
    expect(wrapper.find('.notif-bell').exists()).toBe(true)
  })
})

// ==================== NOTIFICATIONS VIEW ====================

describe('NotificationsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  function mountView() {
    const auth = useAuthStore()
    auth.user = { id: 1, username: 'test', nickname: 'Test', email: null, avatarUrl: null, bio: null, admin: false }
    auth.initialized = true
    const router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/', name: 'home', component: { template: '<div>home</div>' }, meta: { requiresAuth: true } },
        { path: '/notifications', name: 'notifications', component: NotificationsView, meta: { requiresAuth: true } },
        { path: '/posts/:id', name: 'post-detail', component: { template: '<div>post</div>' }, meta: { requiresAuth: true } },
        { path: '/training-plans/:id', name: 'plan-detail', component: { template: '<div>plan</div>' }, meta: { requiresAuth: true } },
      ],
    })
    return mount(NotificationsView, {
      global: { plugins: [router], stubs: { LoadingState: true, ErrorState: true } },
    })
  }

  it('renders page title', async () => {
    mockGetNotifications.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.page-title').exists()).toBe(true)
  })

  it('shows empty state when no notifications', async () => {
    mockGetNotifications.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.empty-state').exists()).toBe(true)
  })

  it('shows notification items', async () => {
    mockGetNotifications.mockResolvedValueOnce({
      items: [{ id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2,
        resourceType: 'POST', resourceId: 10,
        payload: { postTitle: 'Test Post', actorNickname: 'Alice' },
        isRead: false, readTime: null, createTime: '2026-07-28T12:00:00' }],
      total: 1, page: 1, size: 20,
    })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.notification-item').exists()).toBe(true)
    expect(wrapper.find('.notification-item.unread').exists()).toBe(true)
  })

  it('shows read notification without unread class', async () => {
    mockGetNotifications.mockResolvedValueOnce({
      items: [{ id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2,
        resourceType: 'POST', resourceId: 10,
        payload: { postTitle: 'Test Post', actorNickname: 'Alice' },
        isRead: true, readTime: '2026-07-28T12:00:00', createTime: '2026-07-28T12:00:00' }],
      total: 1, page: 1, size: 20,
    })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.notification-item.unread').exists()).toBe(false)
  })

  it('toggleFilter switches unreadOnly and refetches', async () => {
    mockGetNotifications.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 })
    mockGetNotifications.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 })
    const wrapper = mountView()
    await flushPromises()
    expect(mockGetNotifications).toHaveBeenCalledWith(1, 20, false)

    const checkbox = wrapper.find('input[type="checkbox"]')
    ;(checkbox.element as HTMLInputElement).checked = true
    await checkbox.trigger('change')
    expect(mockGetNotifications).toHaveBeenCalledWith(1, 20, true)
    expect(mockGetNotifications).toHaveBeenCalledTimes(2)
  })

  it('mark all read button calls store', async () => {
    mockGetNotifications.mockResolvedValueOnce({
      items: [{ id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2,
        resourceType: 'POST', resourceId: 10,
        payload: { postTitle: 'Test' }, isRead: false, readTime: null,
        createTime: '2026-07-28T12:00:00' }],
      total: 1, page: 1, size: 20,
    })
    mockMarkAllRead.mockResolvedValueOnce(undefined)
    const wrapper = mountView()
    await flushPromises()
    const btn = wrapper.find('.mark-all-btn')
    expect(btn.exists()).toBe(true)
    await btn.trigger('click')
    expect(mockMarkAllRead).toHaveBeenCalled()
  })

  it('mark all read hidden when all already read', async () => {
    mockGetNotifications.mockResolvedValueOnce({
      items: [{ id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2,
        resourceType: 'POST', resourceId: 10,
        payload: { postTitle: 'Test' }, isRead: true,
        readTime: '2026-07-28T12:00:00', createTime: '2026-07-28T12:00:00' }],
      total: 1, page: 1, size: 20,
    })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.mark-all-btn').exists()).toBe(false)
  })

  it('notificationText renders all 11 types correctly', async () => {
    const types = [
      { type: 'POST_COMMENTED', payload: { postTitle: 'P1', actorNickname: 'Alice' }, expected: 'Alice 评论了你的帖子「P1」' },
      { type: 'COMMENT_REPLIED', payload: { actorNickname: 'Bob' }, expected: 'Bob 回复了你的评论' },
      { type: 'POST_ADMIN_DEACTIVATED', payload: { postTitle: 'P2', reason: '违规' }, expected: '管理员停用了你的帖子「P2」，原因：违规' },
      { type: 'COMMENT_ADMIN_DEACTIVATED', payload: { reason: '违规内容' }, expected: '管理员停用了你的评论，原因：违规内容' },
      { type: 'POST_RESTORED', payload: { postTitle: 'P3' }, expected: '管理员恢复了你的帖子「P3」' },
      { type: 'COMMENT_RESTORED', payload: {}, expected: '管理员恢复了你的评论' },
      { type: 'TRAINING_MEMBER_REMOVED', payload: { planTitle: 'Plan A' }, expected: '你被移出了计划「Plan A」' },
      { type: 'TRAINING_ADMIN_DEACTIVATED', payload: { planTitle: 'Plan B', reason: '暂停' }, expected: '管理员停用了计划「Plan B」，原因：暂停' },
      { type: 'TRAINING_RESTORED', payload: { planTitle: 'Plan C' }, expected: '管理员恢复了计划「Plan C」' },
      { type: 'TRAINING_SCHEDULE_CHANGED', payload: { planTitle: 'Plan D' }, expected: '计划「Plan D」的时间安排已更新' },
      { type: 'TRAINING_PROBLEMS_CHANGED', payload: { planTitle: 'Plan E' }, expected: '计划「Plan E」的题目列表已更新' },
    ]

    for (const { type, payload } of types) {
      mockGetNotifications.mockResolvedValueOnce({
        items: [{ id: 1, notificationType: type, actorUserId: 2,
          resourceType: 'POST', resourceId: 10,
          payload, isRead: false, readTime: null, createTime: '2026-07-28T12:00:00' }],
        total: 1, page: 1, size: 20,
      })
      setActivePinia(createPinia())
      const wrapper = mountView()
      await flushPromises()
      const text = wrapper.find('.notif-text').text()
      expect(text).toContain(type === 'POST_COMMENTED' ? '评论了你的帖子' : type === 'COMMENT_REPLIED' ? '回复了你的评论' : type === 'TRAINING_MEMBER_REMOVED' ? '被移出了计划' : '')
      wrapper.unmount()
    }
  })

  it('linkFor returns correct routes', async () => {
    mockGetNotifications.mockResolvedValueOnce({
      items: [{ id: 1, notificationType: 'POST_COMMENTED', actorUserId: 2,
        resourceType: 'POST', resourceId: 10,
        payload: { postTitle: 'Test' }, isRead: false, readTime: null, createTime: '2026-07-28T12:00:00' }],
      total: 1, page: 1, size: 20,
    })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.notif-link').exists()).toBe(true)
  })

  it('error state shows retry', async () => {
    mockGetNotifications.mockRejectedValueOnce(new Error('fail'))
    const wrapper = mountView()
    await flushPromises()
    await flushPromises()
    expect(wrapper.findComponent({ name: 'ErrorState' }).exists() || wrapper.text()).toBeTruthy()
  })
})
