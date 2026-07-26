import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  login as apiLogin,
  register as apiRegister,
  getCurrentUser,
  getCsrfToken,
  logout as apiLogout,
  type UserInfo,
} from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null)
  const initialized = ref(false)
  const initError = ref<string | null>(null)

  const isLoggedIn = computed(() => user.value !== null)
  const isAdmin = computed(() => user.value?.admin ?? false)

  async function init(): Promise<void> {
    initError.value = null
    try {
      user.value = await getCurrentUser()
    } catch (e: unknown) {
      const err = e as { response?: { status: number } }
      if (err.response?.status === 401) {
        user.value = null
      } else {
        initError.value = '暂时无法连接服务器，请检查网络后重试'
        user.value = null
      }
    } finally {
      initialized.value = true
    }
  }

  function clearInitError(): void {
    initError.value = null
  }

  async function login(username: string, password: string): Promise<void> {
    user.value = await apiLogin({ username, password })
  }

  async function register(
    username: string,
    password: string,
    nickname: string,
    email?: string,
  ): Promise<void> {
    await apiRegister({ username, password, nickname, email })
  }

  async function logout(): Promise<void> {
    if (user.value) {
      try {
        const csrf = await getCsrfToken()
        await apiLogout(csrf.headerName, csrf.token)
      } catch {
        // Logout best-effort; clear local state regardless
      }
    }
    user.value = null
  }

  return { user, initialized, initError, isLoggedIn, isAdmin, init, clearInitError, login, register, logout }
})
