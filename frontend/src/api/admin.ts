import apiClient from './client'
import { withCsrf } from './csrf'
import type { AdminUserListResponse } from '@/types/admin'

export async function listUsers(params: { page?: number; size?: number; keyword?: string }): Promise<AdminUserListResponse> {
  const r = await apiClient.get<AdminUserListResponse>('/admin/users', { params })
  return r.data
}

export async function toggleUserStatus(id: number): Promise<void> {
  return withCsrf((h, t) => apiClient.post(`/admin/users/${id}/toggle-status`, null, { headers: { [h]: t } }))
}

export async function toggleUserAdmin(id: number): Promise<void> {
  return withCsrf((h, t) => apiClient.post(`/admin/users/${id}/toggle-admin`, null, { headers: { [h]: t } }))
}
