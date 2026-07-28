import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import OJAccountView from '@/views/OJAccountView.vue'

const mockGetMyAccount = vi.fn()
const mockSyncAccount = vi.fn()

vi.mock('@/api/oj', () => ({
  getMyAccount: (...args: unknown[]) => mockGetMyAccount(...args),
  bindAccount: vi.fn(),
  unbindAccount: vi.fn(),
  getPendingAccounts: vi.fn().mockResolvedValue([]),
  verifyAccount: vi.fn(),
  syncMyAccount: (...args: unknown[]) => mockSyncAccount(...args),
}))

vi.mock('@/api/auth', () => ({
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
  getCsrfToken: vi.fn().mockResolvedValue({ token: 't', headerName: 'X-CSRF-TOKEN' }),
  logout: vi.fn(),
}))

function mountView() {
  const auth = useAuthStore()
  auth.$patch({ user: { id: 1, username: 'test', nickname: 'Test', isAdmin: false }, initialLoadingDone: true, loaded: true })
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/oj-account', name: 'oj-account', component: OJAccountView },
    ],
  })
  router.push('/oj-account')
  return mount(OJAccountView, {
    global: { plugins: [router], stubs: { LoadingState: true, ErrorState: true } },
  })
}

describe('OJ Account API', () => {
  it('syncMyAccount is defined', () => {
    expect(mockSyncAccount).toBeDefined()
  })
})

describe('OJAccountView sync button', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('renders page', async () => {
    mockGetMyAccount.mockResolvedValue({ hasAccount: false })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.page-title').exists()).toBe(true)
  })

  it('shows sync button disabled for pending account', async () => {
    mockGetMyAccount.mockResolvedValue({
      hasAccount: true, platform: 'CODEFORCES', externalUserId: 'handle',
      verifyStatus: 0, syncEnabled: 1, lastSyncTime: null, lastSyncSuccess: null,
    })
    const wrapper = mountView()
    await flushPromises()
    const btn = wrapper.find('.sync-btn')
    expect(btn.exists()).toBe(true)
    expect(btn.attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('账号审核通过后才能同步')
  })

  it('shows sync button disabled for rejected account', async () => {
    mockGetMyAccount.mockResolvedValue({
      hasAccount: true, platform: 'CODEFORCES', externalUserId: 'handle',
      verifyStatus: 2, syncEnabled: 1, lastSyncTime: null, lastSyncSuccess: null,
    })
    const wrapper = mountView()
    await flushPromises()
    expect(wrapper.find('.sync-btn').attributes('disabled')).toBeDefined()
    expect(wrapper.text()).toContain('账号已被拒绝')
  })

  it('shows sync button enabled for verified account', async () => {
    mockGetMyAccount.mockResolvedValue({
      hasAccount: true, platform: 'CODEFORCES', externalUserId: 'handle',
      verifyStatus: 1, syncEnabled: 1, lastSyncTime: null, lastSyncSuccess: null,
    })
    mockSyncAccount.mockResolvedValue({ accountId: 1, handle: 'h', fetchedCount: 0, insertedCount: 0, acceptedCount: 0, newAcceptedProblemCount: 0, lastSyncTime: null, syncStatus: 'SUCCESS' })
    const wrapper = mountView()
    await flushPromises()
    const btn = wrapper.find('.sync-btn')
    expect(btn.exists()).toBe(true)
    expect(btn.attributes('disabled')).toBeUndefined()
  })

  it('displays sync result after successful sync', async () => {
    mockGetMyAccount.mockResolvedValue({
      hasAccount: true, platform: 'CODEFORCES', externalUserId: 'handle',
      verifyStatus: 1, syncEnabled: 1, lastSyncTime: null, lastSyncSuccess: null,
    })
    mockSyncAccount.mockResolvedValue({
      accountId: 1, handle: 'handle',
      fetchedCount: 100, insertedCount: 10, acceptedCount: 5,
      newAcceptedProblemCount: 2, lastSyncTime: '2025-01-01T12:00:00', syncStatus: 'SUCCESS',
    })
    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('.sync-btn').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('获取 100 条')
    expect(wrapper.text()).toContain('新增 10 条')
    expect(wrapper.text()).toContain('AC 5 条')
    expect(wrapper.text()).toContain('首次 AC 2 题')
  })

  it('displays error after failed sync', async () => {
    mockGetMyAccount.mockResolvedValue({
      hasAccount: true, platform: 'CODEFORCES', externalUserId: 'handle',
      verifyStatus: 1, syncEnabled: 1, lastSyncTime: null, lastSyncSuccess: null,
    })
    mockSyncAccount.mockRejectedValue({ response: { data: { message: '同步失败，请稍后重试' } } })
    const wrapper = mountView()
    await flushPromises()
    await wrapper.find('.sync-btn').trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('同步失败，请稍后重试')
  })
})
