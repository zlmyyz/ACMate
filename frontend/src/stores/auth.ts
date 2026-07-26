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

  const isLoggedIn = computed(() => user.value !== null)
  const isAdmin = computed(() => user.value?.admin ?? false)

  async function init(): Promise<void> {
    try {
      user.value = await getCurrentUser()
    } catch {
      user.value = null
    } finally {
      initialized.value = true
    }
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
    // Registration does not auto-login
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

  return { user, initialized, isLoggedIn, isAdmin, init, login, register, logout }
})
