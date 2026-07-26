import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import { createRouter, createWebHistory } from 'vue-router'

const { mockGetUserProfile, mockUpdateProfile, mockUploadAvatar } = vi.hoisted(() => ({
  mockGetUserProfile: vi.fn(),
  mockUpdateProfile: vi.fn(),
  mockUploadAvatar: vi.fn(),
}))

vi.mock('@/api/users', () => ({
  getUserProfile: mockGetUserProfile,
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

  it('should render user profile after load', async () => {
    mockGetUserProfile.mockResolvedValueOnce({
      id: 1, username: 'testuser', nickname: 'Test User',
      avatarUrl: null, bio: 'A bio', admin: false,
      problemCount: 5, createTime: '2024-01-15T00:00:00',
    })

    const wrapper = await mountProfile('/users/1')
    await flushPromises()

    expect(wrapper.text()).toContain('Test User')
    expect(wrapper.text()).toContain('@testuser')
    expect(wrapper.text()).toContain('A bio')
    expect(wrapper.text()).toContain('5')
  })

  it('should show admin badge for admin user', async () => {
    mockGetUserProfile.mockResolvedValueOnce({
      id: 2, username: 'admin', nickname: 'Admin',
      avatarUrl: null, bio: null, admin: true,
      problemCount: 10, createTime: '2024-01-01T00:00:00',
    })

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

    mockGetUserProfile.mockResolvedValueOnce({
      id: 1, username: 'me', nickname: 'Me',
      avatarUrl: null, bio: null, admin: false,
      problemCount: 0, createTime: '2024-01-01T00:00:00',
    })

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
})
