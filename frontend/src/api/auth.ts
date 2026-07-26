import apiClient from './client'

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  nickname: string
  email?: string
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
