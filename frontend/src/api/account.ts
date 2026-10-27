import apiClient from './client'
import { loginPost, type UserInfo } from './auth'

export interface AccountBindings { email: string | null; githubBound: boolean; githubLogin: string | null; githubName: string | null }
export interface BindingPreview { mergeRequired: boolean; accountName: string; githubLogin?: string; githubName?: string }
export const getBindings = async (): Promise<AccountBindings> => (await apiClient.get('/users/me/bindings')).data
export const sendRegistrationCode = (email: string) => loginPost<{ message: string }>('/auth/register/code', { email })
export const sendResetCode = (email: string) => loginPost<{ message: string }>('/auth/password/code', { email })
export const resetPassword = (email: string, code: string, password: string) => loginPost<{ message: string }>('/auth/password/reset', { email, code, password })
export const changePassword = (currentPassword: string, newPassword: string, confirmPassword: string) => loginPost<{ message: string }>('/users/me/password', { currentPassword, newPassword, confirmPassword })
export const sendBindingCode = (email: string, current = false) => loginPost<{ message: string }>('/users/me/email/code', { email, current })
export const prepareEmailBinding = (email: string, code: string, currentCode: string) => loginPost<BindingPreview>('/users/me/email/prepare', { email, code, currentCode })
export const confirmEmailBinding = () => loginPost<UserInfo>('/users/me/email/confirm', { confirm: true })
export const authorizeGithubBinding = () => loginPost<{ url: string }>('/users/me/github/authorize', {})
export const getPendingGithub = async (): Promise<BindingPreview> => (await apiClient.get('/users/me/github/pending')).data
export const confirmGithubBinding = () => loginPost<UserInfo>('/users/me/github/confirm', { confirm: true })
export const unlinkGithub = () => loginPost<UserInfo>('/users/me/github/unlink', { confirm: true })
export const cancelBinding = () => loginPost('/users/me/bindings/cancel', {})
export const getGithubLoginLink = async (): Promise<{ githubLogin: string; email: string }> => (await apiClient.get('/auth/github/link')).data
export const sendGithubLinkCode = () => loginPost<{ message: string }>('/auth/github/link/code', {})
export const confirmGithubLoginLink = (code: string) => loginPost<UserInfo>('/auth/github/link/confirm', { code, confirm: true })
export const skipGithubLoginLink = () => loginPost<UserInfo>('/auth/github/link/skip', {})
export function accountError(error: unknown, fallback = '操作失败，请稍后重试'): string {
  return (error as { response?: { data?: { message?: string } } }).response?.data?.message || fallback
}
