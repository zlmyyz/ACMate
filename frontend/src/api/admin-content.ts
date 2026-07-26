import apiClient from './client'
import { withCsrf } from './csrf'
import type { AdminPostListResponse, AdminCommentListResponse } from '@/types/admin-content'

export async function getAdminPosts(params: Record<string, unknown> = {}): Promise<AdminPostListResponse> {
  const r = await apiClient.get<AdminPostListResponse>('/admin/posts', { params })
  return r.data
}

export async function deactivatePost(id: number, reason: string): Promise<void> {
  await withCsrf((h, t) => apiClient.post(`/admin/posts/${id}/deactivate`, { reason }, { headers: { [h]: t } }))
}

export async function restorePost(id: number): Promise<void> {
  await withCsrf((h, t) => apiClient.post(`/admin/posts/${id}/restore`, null, { headers: { [h]: t } }))
}

export async function getAdminComments(params: Record<string, unknown> = {}): Promise<AdminCommentListResponse> {
  const r = await apiClient.get<AdminCommentListResponse>('/admin/comments', { params })
  return r.data
}

export async function deactivateComment(id: number, reason: string): Promise<void> {
  await withCsrf((h, t) => apiClient.post(`/admin/comments/${id}/deactivate`, { reason }, { headers: { [h]: t } }))
}

export async function restoreComment(id: number): Promise<void> {
  await withCsrf((h, t) => apiClient.post(`/admin/comments/${id}/restore`, null, { headers: { [h]: t } }))
}
