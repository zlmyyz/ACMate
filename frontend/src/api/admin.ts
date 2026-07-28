import apiClient from './client'
import { withCsrf } from './csrf'
import type { AdminUserListResponse, AdminUserFilterParams } from '@/types/admin'

export async function listUsers(params: AdminUserFilterParams): Promise<AdminUserListResponse> {
  const r = await apiClient.get<AdminUserListResponse>('/admin/users', { params })
  return r.data
}

export async function deactivateUser(id: number, reason: string): Promise<void> {
  return withCsrf((h, t) => apiClient.put(`/admin/users/${id}/deactivate`, { reason }, { headers: { [h]: t } }))
}

export async function reactivateUser(id: number): Promise<void> {
  return withCsrf((h, t) => apiClient.put(`/admin/users/${id}/restore`, null, { headers: { [h]: t } }))
}

export async function grantUserAdmin(id: number): Promise<void> {
  return withCsrf((h, t) => apiClient.put(`/admin/users/${id}/grant-admin`, null, { headers: { [h]: t } }))
}

export async function revokeUserAdmin(id: number): Promise<void> {
  return withCsrf((h, t) => apiClient.put(`/admin/users/${id}/revoke-admin`, null, { headers: { [h]: t } }))
}
