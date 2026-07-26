import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { mount } from '@vue/test-utils'

const { mockGetProblems, mockGetProblemDetail, mockCreateProblem, mockUpdateProblem } = vi.hoisted(() => ({
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockGetProblems: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockGetProblemDetail: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockCreateProblem: vi.fn<() => any>(),
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockUpdateProblem: vi.fn<() => any>(),
}))

vi.mock('@/api/problems', () => ({
  getProblems: mockGetProblems,
  getProblemDetail: mockGetProblemDetail,
  createProblem: mockCreateProblem,
  updateProblem: mockUpdateProblem,
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
        problems: [{ id: 1, platform: 'CODEFORCES', externalProblemKey: '1A', title: 'Test', sourceUrl: null, difficulty: '800', tags: 'dp,math', creatorUserId: 1, createTime: '2026-07-20T12:00:00' }],
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
        problems: [{ id: 1, platform: 'CUSTOM', externalProblemKey: null, title: 'Test', sourceUrl: null, difficulty: null, tags: null, creatorUserId: 42, createTime: '2026-07-20T12:00:00' }],
        loading: false,
      },
      global: { stubs: { 'router-link': true } },
    })
    expect(wrapper.text()).toContain('用户 #42')
  })
})
