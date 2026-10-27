import apiClient from './client'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname: string
  email: string
  emailCode: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  email: string | null
  avatarUrl: string | null
  bio: string | null
  admin: boolean
}

export interface CsrfToken {
  token: string
  headerName: string
  parameterName: string
}

export async function login(data: LoginRequest): Promise<UserInfo> {
  const response = await apiClient.post<UserInfo>('/auth/login', data)
  return response.data
}

export async function getLoginMethods(): Promise<{ email: boolean; github: boolean; registration: boolean }> {
  return (await apiClient.get('/auth/methods')).data
}

export async function loginPost<T>(path: string, data: unknown): Promise<T> {
  const csrf = await getCsrfToken()
  return (await apiClient.post<T>(path, data, { headers: { [csrf.headerName]: csrf.token } })).data
}

export function sendEmailCode(email: string): Promise<{ message: string }> {
  return loginPost('/auth/email/code', { email })
}

export function loginWithEmail(email: string, code: string): Promise<UserInfo> {
  return loginPost('/auth/email/login', { email, code })
}

export function authorizeGithub(): Promise<{ url: string }> {
  return loginPost('/auth/github/authorize', {})
}

export async function register(data: RegisterRequest): Promise<UserInfo> {
  const response = await apiClient.post<UserInfo>('/auth/register', data)
  return response.data
}

export async function getCurrentUser(): Promise<UserInfo> {
  const response = await apiClient.get<UserInfo>('/users/me')
  return response.data
}

export async function getCsrfToken(): Promise<CsrfToken> {
  const response = await apiClient.get<CsrfToken>('/auth/csrf')
  return response.data
}

export async function logout(csrfHeader: string, csrfToken: string): Promise<void> {
  await apiClient.post('/auth/logout', null, {
    headers: {
      [csrfHeader]: csrfToken,
    },
  })
}
