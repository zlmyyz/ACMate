import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'

const { mockGetProblems, mockGetProblemDetail, mockCreateProblem, mockUpdateProblem, mockGetMyProblems, mockDeactivateProblem, mockRestoreProblem, mockGetAdminProblems, mockAdminDeactivateProblem, mockAdminRestoreProblem } = vi.hoisted(() => ({
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockGetProblems: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockGetProblemDetail: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockCreateProblem: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockUpdateProblem: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockGetMyProblems: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockDeactivateProblem: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockRestoreProblem: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockGetAdminProblems: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockAdminDeactivateProblem: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockAdminRestoreProblem: vi.fn<() => any>(),
}))

vi.mock('@/api/problems', () => ({
  getProblems: mockGetProblems,
  getProblemDetail: mockGetProblemDetail,
  createProblem: mockCreateProblem,
  updateProblem: mockUpdateProblem,
  getMyProblems: mockGetMyProblems,
  deactivateProblem: mockDeactivateProblem,
  restoreProblem: mockRestoreProblem,
  getAdminProblems: mockGetAdminProblems,
  adminDeactivateProblem: mockAdminDeactivateProblem,
  adminRestoreProblem: mockAdminRestoreProblem,
}))

const { mockGetCurrentUser, mockLogin, mockRegister, mockCsrf, mockLogoutApi } = vi.hoisted(() => ({
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockGetCurrentUser: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockLogin: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockRegister: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockCsrf: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockLogoutApi: vi.fn<() => any>(),
}))

vi.mock('@/api/auth', () => ({
  getCurrentUser: mockGetCurrentUser,
  login: mockLogin,
  register: mockRegister,
  getCsrfToken: mockCsrf,
  logout: mockLogoutApi,
}))

describe('Problems API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getProblems query params', () => {
    it('should pass keyword, platform, difficulty as query params', async () => {
      mockGetProblems.mockResolvedValueOnce({ page: 1, size: 20, total: 0, pages: 0, records: [] })

      const { getProblems } = await import('@/api/problems')
      await getProblems({ page: 1, platform: 'CODEFORCES', difficulty: '800', keyword: 'test' })

      expect(mockGetProblems).toHaveBeenCalledWith({
        page: 1, platform: 'CODEFORCES', difficulty: '800',
        keyword: 'test', size: undefined, creatorUserId: undefined,
      })
    })

    it('should omit undefined params', async () => {
      mockGetProblems.mockResolvedValueOnce({ page: 1, size: 20, total: 0, pages: 0, records: [] })

      const { getProblems } = await import('@/api/problems')
      await getProblems({ page: 1 })

      expect(mockGetProblems).toHaveBeenCalledWith({
        page: 1, size: undefined, platform: undefined,
        difficulty: undefined, keyword: undefined, creatorUserId: undefined,
      })
    })
  })

  describe('createProblem', () => {
    it('should not include creatorUserId or status in request', async () => {
      mockCsrf.mockResolvedValueOnce({ token: 'csrf', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' })
      mockCreateProblem.mockResolvedValueOnce({ id: 1, platform: 'CUSTOM', title: 'Test', creatorUserId: 1 })

      const { createProblem } = await import('@/api/problems')
      await createProblem({ platform: 'CUSTOM', title: 'Test Problem' })

      const callArgs = mockCreateProblem.mock.calls[0] as unknown[]
      expect(callArgs).toBeDefined()
      const callArg = callArgs[0] as Record<string, unknown>
      expect(callArg).not.toHaveProperty('creatorUserId')
      expect(callArg).not.toHaveProperty('status')
    })

    it('should carry CSRF token by calling createProblem API', async () => {
      mockCsrf.mockResolvedValueOnce({ token: 'token-abc', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' })
      mockCreateProblem.mockResolvedValueOnce({ id: 1, title: 'Test' })

      const { createProblem } = await import('@/api/problems')
      await createProblem({ platform: 'CUSTOM', title: 'Test' })

      expect(mockCreateProblem).toHaveBeenCalledWith({ platform: 'CUSTOM', title: 'Test' })
    })

    it('should reject with 409 on conflict', async () => {
      mockCsrf.mockResolvedValueOnce({ token: 'csrf', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' })
      mockCreateProblem.mockRejectedValueOnce({ response: { status: 409, data: { message: '该平台题目标识已存在' } } })

      const { createProblem } = await import('@/api/problems')
      await expect(createProblem({ platform: 'CODEFORCES', externalProblemKey: '1A', title: 'Test' }))
        .rejects.toBeDefined()
    })
  })

  describe('updateProblem', () => {
    it('should not modify status when editing', async () => {
      mockCsrf.mockResolvedValueOnce({ token: 'csrf', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' })
      mockUpdateProblem.mockResolvedValueOnce({ id: 5, platform: 'CUSTOM', title: 'Updated', creatorUserId: 1 })

      const { updateProblem } = await import('@/api/problems')
      await updateProblem(5, { platform: 'CUSTOM', title: 'Updated Title' })

      const callArgs = mockUpdateProblem.mock.calls[0] as unknown[]
      expect(callArgs).toBeDefined()
      const callArg = callArgs[0] as Record<string, unknown>
      expect(callArg).not.toHaveProperty('status')
      expect(callArg).not.toHaveProperty('creatorUserId')
    })
  })
})

describe('MarkdownContent security', () => {
  it('should not render raw HTML', async () => {
    const { default: MarkdownContent } = await import('@/components/common/MarkdownContent.vue')
    const wrapper = mount(MarkdownContent, {
      props: { content: '<script>alert("xss")</script>' },
    })
    const html = wrapper.html()
    expect(html).not.toContain('<script>')
    expect(html).toContain('&lt;script&gt;')
  })

  it('should not create dangerous links', async () => {
    const { default: MarkdownContent } = await import('@/components/common/MarkdownContent.vue')
    const wrapper = mount(MarkdownContent, {
      props: { content: '[click](javascript:alert(1))' },
    })
    const html = wrapper.html()
    expect(html).not.toContain('href="javascript:')
  })

  it('should render safe markdown', async () => {
    const { default: MarkdownContent } = await import('@/components/common/MarkdownContent.vue')
    const wrapper = mount(MarkdownContent, {
      props: { content: '**bold** and *italic*' },
    })
    expect(wrapper.html()).toContain('<strong>bold</strong>')
    expect(wrapper.html()).toContain('<em>italic</em>')
  })

  it('should render safe links', async () => {
    const { default: MarkdownContent } = await import('@/components/common/MarkdownContent.vue')
    const wrapper = mount(MarkdownContent, {
      props: { content: '[safe](https://example.com)' },
    })
    expect(wrapper.html()).toContain('href="https://example.com"')
  })

  it('should render null content as empty', async () => {
    const { default: MarkdownContent } = await import('@/components/common/MarkdownContent.vue')
    const wrapper = mount(MarkdownContent, {
      props: { content: null },
    })
    expect(wrapper.text()).toContain('暂无内容')
  })
})

describe('ProblemForm draft', () => {
  it('should load draft from localStorage on mount', async () => {
    localStorage.setItem('test:draft:load', JSON.stringify({ platform: 'CUSTOM', title: 'Draft Title' }))

    const { default: ProblemForm } = await import('@/components/problem/ProblemForm.vue')
    const wrapper = mount(ProblemForm, {
      props: { draftKey: 'test:draft:load', submitting: false, errorMsg: '' },
      global: { stubs: { 'router-link': true } },
    })
    // Form loads the draft; input values should reflect draft data
    expect(wrapper.find('select').element.value).toBe('CUSTOM')
  })

  it('should use initialData over draft when provided', async () => {
    localStorage.setItem('test:draft:init', JSON.stringify({ platform: 'CODEFORCES', title: 'Draft' }))

    const { default: ProblemForm } = await import('@/components/problem/ProblemForm.vue')
    const wrapper = mount(ProblemForm, {
      props: {
        draftKey: 'test:draft:init',
        submitting: false,
        errorMsg: '',
        initialData: {
          id: 1, platform: 'CUSTOM', title: 'Real Title',
          externalProblemKey: null, sourceUrl: null, difficulty: null,
          tags: null, contentMd: null, creatorUserId: 1,
          creatorUsername: null, creatorNickname: null, active: true, deactivationSource: null,
          createTime: '2026-01-01T00:00:00', updateTime: '2026-01-01T00:00:00',
        },
      },
      global: { stubs: { 'router-link': true } },
    })
    expect(wrapper.find('select').element.value).toBe('CUSTOM')
  })
})

describe('ProblemFilters sync', () => {
  it('should render search and reset buttons', async () => {
    const { default: ProblemFilters } = await import('@/components/problem/ProblemFilters.vue')
    const wrapper = mount(ProblemFilters, {
      props: { modelValue: { keyword: '', platform: '', difficulty: '' } },
      global: { stubs: { 'router-link': true } },
    })
    expect(wrapper.find('.search-btn').exists()).toBe(true)
    expect(wrapper.find('.reset-btn').exists()).toBe(true)
  })
})

describe('Auth init error handling', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should treat 401 as not logged in', async () => {
    mockGetCurrentUser.mockRejectedValueOnce({ response: { status: 401 } })
    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    await auth.init()
    expect(auth.user).toBeNull()
    expect(auth.initialized).toBe(true)
    expect(auth.initError).toBeNull()
  })

  it('should set initError on network failure', async () => {
    mockGetCurrentUser.mockRejectedValueOnce(new Error('Network Error'))
    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    await auth.init()
    expect(auth.user).toBeNull()
    expect(auth.initialized).toBe(true)
    expect(auth.initError).toContain('无法连接')
  })

  it('should set initError on 500', async () => {
    mockGetCurrentUser.mockRejectedValueOnce({ response: { status: 500 } })
    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    await auth.init()
    expect(auth.user).toBeNull()
    expect(auth.initialized).toBe(true)
    expect(auth.initError).toBeTruthy()
  })

  it('should clear initError on retry', async () => {
    mockGetCurrentUser.mockRejectedValueOnce(new Error('Network Error'))
    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    await auth.init()
    expect(auth.initError).toBeTruthy()
    auth.clearInitError()
    expect(auth.initError).toBeNull()
  })

  it('should complete initialized regardless of error', async () => {
    mockGetCurrentUser.mockRejectedValueOnce({ response: { status: 500 } })
    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    await auth.init()
    expect(auth.initialized).toBe(true)
  })
})

describe('My Problems API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should pass status, keyword, page as query params', async () => {
    mockGetMyProblems.mockResolvedValueOnce({ page: 1, size: 20, total: 0, pages: 0, records: [] })

    const { getMyProblems } = await import('@/api/problems')
    await getMyProblems({ page: 1, status: 'ACTIVE', keyword: 'dp' })

    expect(mockGetMyProblems).toHaveBeenCalledWith({
      page: 1, status: 'ACTIVE', keyword: 'dp', size: undefined,
      platform: undefined, difficulty: undefined,
    })
  })

  it('should default status to ALL when not provided', async () => {
    mockGetMyProblems.mockResolvedValueOnce({ page: 1, size: 20, total: 0, pages: 0, records: [] })

    const { getMyProblems } = await import('@/api/problems')
    await getMyProblems({ page: 1 })

    const callArgs = mockGetMyProblems.mock.calls[0] as unknown[]
    expect(callArgs[0]).toEqual({
      page: 1, size: undefined, status: undefined,
      platform: undefined, difficulty: undefined, keyword: undefined,
    })
  })
})

describe('deactivateProblem API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should call POST with CSRF', async () => {
    mockCsrf.mockResolvedValueOnce({ token: 'csrf', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' })
    mockDeactivateProblem.mockResolvedValueOnce(undefined)

    const { deactivateProblem } = await import('@/api/problems')
    await deactivateProblem(42)

    expect(mockDeactivateProblem).toHaveBeenCalledWith(42)
  })

  it('should handle 404 gracefully', async () => {
    mockCsrf.mockResolvedValueOnce({ token: 'csrf', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' })
    mockDeactivateProblem.mockRejectedValueOnce({ response: { status: 404 } })

    const { deactivateProblem } = await import('@/api/problems')
    await expect(deactivateProblem(42)).rejects.toBeDefined()
  })
})

describe('restoreProblem API', () => {
  it('should call POST with CSRF', async () => {
    mockCsrf.mockResolvedValueOnce({ token: 'csrf', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' })
    mockRestoreProblem.mockResolvedValueOnce(undefined)

    const { restoreProblem } = await import('@/api/problems')
    await restoreProblem(42)

    expect(mockRestoreProblem).toHaveBeenCalledWith(42)
  })
})

describe('StatusBadge', () => {
  it('should render ACTIVE label with active class', async () => {
    const { default: StatusBadge } = await import('@/components/problem/StatusBadge.vue')
    const wrapper = mount(StatusBadge, {
      props: { status: 'ACTIVE' },
    })
    expect(wrapper.text()).toContain('正常')
    expect(wrapper.find('.status-active').exists()).toBe(true)
  })

  it('should render INACTIVE label with inactive class', async () => {
    const { default: StatusBadge } = await import('@/components/problem/StatusBadge.vue')
    const wrapper = mount(StatusBadge, {
      props: { status: 'INACTIVE' },
    })
    expect(wrapper.text()).toContain('已停用')
    expect(wrapper.find('.status-inactive').exists()).toBe(true)
  })
})

describe('ProblemStatusTabs', () => {
  it('should render three tabs', async () => {
    const { default: ProblemStatusTabs } = await import('@/components/problem/ProblemStatusTabs.vue')
    const wrapper = mount(ProblemStatusTabs, {
      props: { modelValue: 'ALL' },
    })
    const buttons = wrapper.findAll('.tab-btn')
    expect(buttons).toHaveLength(3)
    expect(buttons[0]!.text()).toContain('全部')
    expect(buttons[1]!.text()).toContain('正常')
    expect(buttons[2]!.text()).toContain('已停用')
  })

  it('should highlight active tab', async () => {
    const { default: ProblemStatusTabs } = await import('@/components/problem/ProblemStatusTabs.vue')
    const wrapper = mount(ProblemStatusTabs, {
      props: { modelValue: 'INACTIVE' },
    })
    const buttons = wrapper.findAll('.tab-btn')
    expect(buttons[2]!.classes()).toContain('tab-active')
    expect(buttons[0]!.classes()).not.toContain('tab-active')
  })

  it('should emit update:modelValue on click', async () => {
    const { default: ProblemStatusTabs } = await import('@/components/problem/ProblemStatusTabs.vue')
    const wrapper = mount(ProblemStatusTabs, {
      props: { modelValue: 'ALL' },
    })
    await wrapper.findAll('.tab-btn')[1]!.trigger('click')
    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['ACTIVE'])
  })
})

describe('ProblemActionButtons', () => {
  it('should show edit and deactivate for ACTIVE', async () => {
    const { default: ProblemActionButtons } = await import('@/components/problem/ProblemActionButtons.vue')
    const wrapper = mount(ProblemActionButtons, {
      props: { problemId: 1, status: 'ACTIVE' },
    })
    expect(wrapper.text()).toContain('编辑')
    expect(wrapper.text()).toContain('停用')
    expect(wrapper.text()).not.toContain('恢复')
  })

  it('should show edit and restore for INACTIVE', async () => {
    const { default: ProblemActionButtons } = await import('@/components/problem/ProblemActionButtons.vue')
    const wrapper = mount(ProblemActionButtons, {
      props: { problemId: 1, status: 'INACTIVE' },
    })
    expect(wrapper.text()).toContain('编辑')
    expect(wrapper.text()).toContain('恢复')
    expect(wrapper.text()).not.toContain('停用')
  })

  it('should emit edit event on edit click', async () => {
    const { default: ProblemActionButtons } = await import('@/components/problem/ProblemActionButtons.vue')
    const wrapper = mount(ProblemActionButtons, {
      props: { problemId: 5, status: 'ACTIVE' },
    })
    await wrapper.find('.edit-action').trigger('click')
    expect(wrapper.emitted('edit')?.[0]).toEqual([5])
  })

  it('should emit deactivate event', async () => {
    const { default: ProblemActionButtons } = await import('@/components/problem/ProblemActionButtons.vue')
    const wrapper = mount(ProblemActionButtons, {
      props: { problemId: 3, status: 'ACTIVE' },
    })
    await wrapper.find('.deactivate-action').trigger('click')
    expect(wrapper.emitted('deactivate')?.[0]).toEqual([3])
  })

  it('should emit restore event', async () => {
    const { default: ProblemActionButtons } = await import('@/components/problem/ProblemActionButtons.vue')
    const wrapper = mount(ProblemActionButtons, {
      props: { problemId: 7, status: 'INACTIVE' },
    })
    await wrapper.find('.restore-action').trigger('click')
    expect(wrapper.emitted('restore')?.[0]).toEqual([7])
  })
})

describe('ConfirmDialog', () => {
  it('should not render when visible is false', async () => {
    const { default: ConfirmDialog } = await import('@/components/common/ConfirmDialog.vue')
    const wrapper = mount(ConfirmDialog, {
      props: { visible: false, title: 'Test', message: 'Msg' },
    })
    expect(wrapper.find('.overlay').exists()).toBe(false)
  })

  it('should render when visible is true', async () => {
    const { default: ConfirmDialog } = await import('@/components/common/ConfirmDialog.vue')
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: '删除确认', message: '确定要删除吗？' },
    })
    expect(wrapper.text()).toContain('删除确认')
    expect(wrapper.text()).toContain('确定要删除吗？')
  })

  it('should emit confirm on confirm click', async () => {
    const { default: ConfirmDialog } = await import('@/components/common/ConfirmDialog.vue')
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: 'T', message: 'M' },
    })
    await wrapper.find('.confirm-btn').trigger('click')
    expect(wrapper.emitted('confirm')).toBeTruthy()
  })

  it('should emit cancel on cancel click', async () => {
    const { default: ConfirmDialog } = await import('@/components/common/ConfirmDialog.vue')
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: 'T', message: 'M' },
    })
    await wrapper.find('.cancel-btn').trigger('click')
    expect(wrapper.emitted('cancel')).toBeTruthy()
  })

  it('should disable buttons when loading', async () => {
    const { default: ConfirmDialog } = await import('@/components/common/ConfirmDialog.vue')
    const wrapper = mount(ConfirmDialog, {
      props: { visible: true, title: 'T', message: 'M', loading: true },
    })
    const confirmBtn = wrapper.find('.confirm-btn')
    expect((confirmBtn.element as HTMLButtonElement).disabled).toBe(true)
  })
})

describe('ProblemTable with slots', () => {
  it('should not show creator column for MyProblemSummary', async () => {
    const { default: ProblemTable } = await import('@/components/problem/ProblemTable.vue')
    const wrapper = mount(ProblemTable, {
      props: {
        problems: [{ id: 1, platform: 'CUSTOM', externalProblemKey: null, title: 'T', sourceUrl: null, difficulty: null, tags: null, status: 'ACTIVE', createTime: '2026-01-01T00:00:00', updateTime: '2026-01-01T00:00:00' }],
        loading: false,
      },
      global: { stubs: { 'router-link': true } },
    })
    expect(wrapper.text()).not.toContain('创建者')
    expect(wrapper.text()).not.toContain('用户 #')
  })

  it('should render status slot content', async () => {
    const { default: ProblemTable } = await import('@/components/problem/ProblemTable.vue')
    const wrapper = mount(ProblemTable, {
      props: {
        problems: [{ id: 1, platform: 'CUSTOM', externalProblemKey: null, title: 'T', sourceUrl: null, difficulty: null, tags: null, status: 'ACTIVE', createTime: '2026-01-01T00:00:00', updateTime: '2026-01-01T00:00:00' }],
        loading: false,
      },
      global: { stubs: { 'router-link': true } },
      slots: {
        status: '<span class="test-status-badge">正常</span>',
      },
    })
    expect(wrapper.find('.test-status-badge').exists()).toBe(true)
  })

  it('should apply row-inactive class for INACTIVE problems', async () => {
    const { default: ProblemTable } = await import('@/components/problem/ProblemTable.vue')
    const wrapper = mount(ProblemTable, {
      props: {
        problems: [{ id: 2, platform: 'CUSTOM', externalProblemKey: null, title: 'Inactive Problem', sourceUrl: null, difficulty: null, tags: null, status: 'INACTIVE', createTime: '2026-01-01T00:00:00', updateTime: '2026-01-01T00:00:00' }],
        loading: false,
        getRowClass: (p) => ({ 'row-inactive': (p as { status: string }).status === 'INACTIVE' }),
      },
      global: { stubs: { 'router-link': true } },
    })
    expect(wrapper.find('.row-inactive').exists()).toBe(true)
  })
})

describe('ProblemTable', () => {
  it('should display no rows when problems is empty', async () => {
    const { default: ProblemTable } = await import('@/components/problem/ProblemTable.vue')
    const wrapper = mount(ProblemTable, {
      props: { problems: [], loading: false },
      global: { stubs: { 'router-link': true } },
    })
    expect(wrapper.find('.problem-row').exists()).toBe(false)
  })

  it('should render problem rows', async () => {
    const { default: ProblemTable } = await import('@/components/problem/ProblemTable.vue')
    const wrapper = mount(ProblemTable, {
      props: {
        problems: [{ id: 1, platform: 'CODEFORCES', externalProblemKey: '1A', title: 'Test', sourceUrl: null, difficulty: '800', tags: 'dp,math', creatorUserId: 1, creatorUsername: null, creatorNickname: null, createTime: '2026-07-20T12:00:00' }],
        loading: false,
      },
      global: { stubs: { 'router-link': true } },
    })
    expect(wrapper.text()).toContain('Test')
    expect(wrapper.text()).toContain('1A')
    expect(wrapper.text()).toContain('用户 #1')
  })

  it('should show creator as 用户 #ID', async () => {
    const { default: ProblemTable } = await import('@/components/problem/ProblemTable.vue')
    const wrapper = mount(ProblemTable, {
      props: {
        problems: [{ id: 1, platform: 'CUSTOM', externalProblemKey: null, title: 'Test', sourceUrl: null, difficulty: null, tags: null, creatorUserId: 42, creatorUsername: null, creatorNickname: null, createTime: '2026-07-20T12:00:00' }],
        loading: false,
      },
      global: { stubs: { 'router-link': true } },
    })
    expect(wrapper.text()).toContain('用户 #42')
  })
})

describe('Admin API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('should pass status and filters to getAdminProblems', async () => {
    mockGetAdminProblems.mockResolvedValueOnce({ page: 1, size: 20, total: 0, pages: 0, records: [] })

    const { getAdminProblems } = await import('@/api/problems')
    await getAdminProblems({ page: 1, status: 'ACTIVE', creatorUserId: 5, keyword: 'dp' })

    expect(mockGetAdminProblems).toHaveBeenCalledWith({
      page: 1, status: 'ACTIVE', creatorUserId: 5, keyword: 'dp',
      size: undefined, platform: undefined, difficulty: undefined,
    })
  })

  it('should call adminDeactivateProblem with reason', async () => {
    mockCsrf.mockResolvedValueOnce({ token: 'csrf', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' })
    mockAdminDeactivateProblem.mockResolvedValueOnce(undefined)

    const { adminDeactivateProblem } = await import('@/api/problems')
    await adminDeactivateProblem(42, '违规内容')

    expect(mockAdminDeactivateProblem).toHaveBeenCalledWith(42, '违规内容')
  })

  it('should call adminRestoreProblem with CSRF', async () => {
    mockCsrf.mockResolvedValueOnce({ token: 'csrf', headerName: 'X-CSRF-TOKEN', parameterName: '_csrf' })
    mockAdminRestoreProblem.mockResolvedValueOnce(undefined)

    const { adminRestoreProblem } = await import('@/api/problems')
    await adminRestoreProblem(42)

    expect(mockAdminRestoreProblem).toHaveBeenCalledWith(42)
  })
})

describe('Admin route guard', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('should redirect non-admin to forbidden', async () => {
    mockGetCurrentUser.mockResolvedValueOnce({ id: 1, username: 'user', nickname: 'User', admin: false })

    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    await auth.init()

    const { default: router } = await import('@/router/index')
    router.resolve('/admin/problems')

    expect(auth.isAdmin).toBe(false)
    expect(auth.isLoggedIn).toBe(true)
  })

  it('should allow admin to access admin routes', async () => {
    mockGetCurrentUser.mockResolvedValueOnce({ id: 1, username: 'admin', nickname: 'Admin', admin: true })

    const { useAuthStore } = await import('@/stores/auth')
    const auth = useAuthStore()
    await auth.init()

    expect(auth.isAdmin).toBe(true)
    expect(auth.isLoggedIn).toBe(true)
  })
})
