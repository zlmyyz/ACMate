import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AdminAuditLogsView from '@/views/AdminAuditLogsView.vue'

const mockGetAuditLogs = vi.fn()

vi.mock('@/api/audit-logs', () => ({
  getAuditLogs: (...args: unknown[]) => mockGetAuditLogs(...args),
}))

vi.mock('@/api/auth', () => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  getCsrfToken: vi.fn().mockResolvedValue({ token: 't', headerName: 'X-CSRF-TOKEN' }),
  logout: vi.fn(),
}))

function buildLog(id: number, overrides: Record<string, unknown> = {}) {
  return {
    id, actionType: 'USER_DEACTIVATED', actorUserId: 1, actorUsername: 'admin', actorNickname: 'Admin',
    targetType: 'USER', targetId: 2, beforeState: 'ACTIVE', afterState: 'DEACTIVATED', reason: 'bad', createTime: '2026-07-29T12:00:00',
    ...overrides,
  }
}

async function mountView() {
  const auth = useAuthStore()
  auth.user = { id: 1, username: 'admin', nickname: 'Admin', email: null, avatarUrl: null, bio: null, admin: true }
  auth.initialized = true
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div>home</div>' }, meta: { requiresAuth: true } },
      { path: '/admin/audit-logs', name: 'admin-audit-logs', component: AdminAuditLogsView, meta: { requiresAuth: true } },
    ],
  })
  router.push('/admin/audit-logs')
  await router.isReady()
  const wrapper = mount(AdminAuditLogsView, {
    global: { plugins: [router], stubs: { LoadingState: true, ErrorState: true, PaginationBar: true } },
  })
  return wrapper
}

describe('AdminAuditLogsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders page title', async () => {
    mockGetAuditLogs.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 })
    const wrapper = await mountView()
    await flushPromises()
    expect(wrapper.find('.page-title').exists()).toBe(true)
    expect(wrapper.find('.page-title').text()).toBe('操作日志')
  })

  it('shows empty state when no logs', async () => {
    mockGetAuditLogs.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 })
    const wrapper = await mountView()
    await flushPromises()
    expect(wrapper.find('.empty-state').exists()).toBe(true)
  })

  it('shows log items', async () => {
    mockGetAuditLogs.mockResolvedValueOnce({ items: [buildLog(1)], total: 1, page: 1, size: 20 })
    const wrapper = await mountView()
    await flushPromises()
    expect(wrapper.find('.table-row').exists()).toBe(true)
    expect(wrapper.find('.action-tag').text()).toBe('停用用户')
  })

  it('passes filter params on search', async () => {
    mockGetAuditLogs.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 })
    const wrapper = await mountView()
    await flushPromises()

    const input = wrapper.find('.search-input')
    await input.setValue('admin')
    const btn = wrapper.find('.search-btn')
    await btn.trigger('click')
    await flushPromises()

    expect(mockGetAuditLogs).toHaveBeenCalledWith(expect.objectContaining({ actorKeyword: 'admin' }))
  })

  it('passes actionType filter', async () => {
    mockGetAuditLogs.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 })
    mockGetAuditLogs.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 })
    const wrapper = await mountView()
    await flushPromises()

    const select = wrapper.findAll('.filter-select')[0]!
    ;(select.element as HTMLSelectElement).value = 'USER_RESTORED'
    await select.trigger('change')
    await flushPromises()

    expect(mockGetAuditLogs).toHaveBeenLastCalledWith(expect.objectContaining({ actionType: 'USER_RESTORED' }))
  })

  it('passes targetType filter', async () => {
    mockGetAuditLogs.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 })
    mockGetAuditLogs.mockResolvedValueOnce({ items: [], total: 0, page: 1, size: 20 })
    const wrapper = await mountView()
    await flushPromises()

    const select = wrapper.findAll('.filter-select')[1]!
    ;(select.element as HTMLSelectElement).value = 'POST'
    await select.trigger('change')
    await flushPromises()

    expect(mockGetAuditLogs).toHaveBeenLastCalledWith(expect.objectContaining({ targetType: 'POST' }))
  })

  it('error state shown on fetch failure', async () => {
    mockGetAuditLogs.mockRejectedValueOnce(new Error('network'))
    const wrapper = await mountView()
    await flushPromises()
    expect(wrapper.findComponent({ name: 'ErrorState' }).exists()).toBe(true)
  })
})
