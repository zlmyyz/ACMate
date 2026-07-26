import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

const { mockGetUser, mockLogin, mockRegister, mockCsrf, mockLogoutApi } = vi.hoisted(() => ({
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  mockGetUser: vi.fn<() => any>(),
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
  getCurrentUser: mockGetUser,
  login: mockLogin,
  register: mockRegister,
  getCsrfToken: mockCsrf,
  logout: mockLogoutApi,
}))

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('init', () => {
    it('should set user on successful current user fetch', async () => {
      const user = { id: 1, username: 'test', nickname: 'Test', email: null, admin: false }
      mockGetUser.mockResolvedValueOnce(user)

      const auth = useAuthStore()
      await auth.init()

      expect(auth.user).toEqual(user)
      expect(auth.initialized).toBe(true)
      expect(auth.isLoggedIn).toBe(true)
    })

    it('should set user to null on 401', async () => {
      mockGetUser.mockRejectedValueOnce({ response: { status: 401 } })

      const auth = useAuthStore()
      await auth.init()

      expect(auth.user).toBeNull()
      expect(auth.initialized).toBe(true)
      expect(auth.isLoggedIn).toBe(false)
    })

    it('should be admin when user has admin flag', async () => {
      const adminUser = { id: 1, username: 'admin', nickname: 'Admin', email: null, admin: true }
      mockGetUser.mockResolvedValueOnce(adminUser)

      const auth = useAuthStore()
      await auth.init()

      expect(auth.isAdmin).toBe(true)
    })
  })

  describe('login', () => {
    it('should set user on successful login', async () => {
      const user = { id: 1, username: 'test', nickname: 'Test', email: null, admin: false }
      mockLogin.mockResolvedValueOnce(user)

      const auth = useAuthStore()
      await auth.login('test', 'password')

      expect(auth.user).toEqual(user)
      expect(auth.isLoggedIn).toBe(true)
    })

    it('should throw on failed login', async () => {
      mockLogin.mockRejectedValueOnce({ response: { status: 401 } })

      const auth = useAuthStore()
      await expect(auth.login('test', 'wrong')).rejects.toBeDefined()
      expect(auth.user).toBeNull()
    })

    it('should pass username and password to API', async () => {
      const user = { id: 1, username: 'test', nickname: 'Test', email: null, admin: false }
      mockLogin.mockResolvedValueOnce(user)

      const auth = useAuthStore()
      await auth.login('testuser', 'secret123')

      expect(mockLogin).toHaveBeenCalledWith({ username: 'testuser', password: 'secret123' })
    })
  })

  describe('register', () => {
    it('should not set user after registration', async () => {
      mockRegister.mockResolvedValueOnce({
        id: 2,
        username: 'new',
        nickname: 'New',
        email: null,
        admin: false,
      })

      const auth = useAuthStore()
      await auth.register('new', 'password', 'New')

      expect(auth.user).toBeNull()
      expect(auth.isLoggedIn).toBe(false)
    })
  })

  describe('logout', () => {
    it('should clear user after logout', async () => {
      const user = { id: 1, username: 'test', nickname: 'Test', email: null, admin: false }
      mockGetUser.mockResolvedValueOnce(user)
      mockCsrf.mockResolvedValueOnce({
        token: 'csrf-token',
        headerName: 'X-CSRF-TOKEN',
        parameterName: '_csrf',
      })
      mockLogoutApi.mockResolvedValueOnce(undefined)

      const auth = useAuthStore()
      await auth.init()
      expect(auth.isLoggedIn).toBe(true)

      await auth.logout()
      expect(auth.user).toBeNull()
      expect(auth.isLoggedIn).toBe(false)
    })

    it('should clear user even if logout API fails', async () => {
      const user = { id: 1, username: 'test', nickname: 'Test', email: null, admin: false }
      mockGetUser.mockResolvedValueOnce(user)
      mockCsrf.mockRejectedValueOnce(new Error('network error'))

      const auth = useAuthStore()
      await auth.init()

      await auth.logout()
      expect(auth.user).toBeNull()
    })
  })

  describe('isAdmin', () => {
    it('should be false for normal user', async () => {
      mockGetUser.mockResolvedValueOnce({
        id: 1,
        username: 'user',
        nickname: 'User',
        email: null,
        admin: false,
      })

      const auth = useAuthStore()
      await auth.init()

      expect(auth.isAdmin).toBe(false)
    })

    it('should be true for admin user', async () => {
      mockGetUser.mockResolvedValueOnce({
        id: 1,
        username: 'admin',
        nickname: 'Admin',
        email: null,
        admin: true,
      })

      const auth = useAuthStore()
      await auth.init()

      expect(auth.isAdmin).toBe(true)
    })
  })
})
