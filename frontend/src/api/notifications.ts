import apiClient from './client'
import { withCsrf } from './csrf'
import type { NotificationListResponse } from '@/types/notification'

export async function getNotifications(page: number = 1, size: number = 20): Promise<NotificationListResponse> {
  const r = await apiClient.get<NotificationListResponse>('/notifications', { params: { page, size } })
  return r.data
}

export async function getUnreadCount(): Promise<number> {
  const r = await apiClient.get<{ count: number }>('/notifications/unread-count')
  return r.data.count
}

export async function markRead(id: number): Promise<void> {
  await withCsrf((headerName, token) =>
    apiClient.post(`/notifications/${id}/read`, null, { headers: { [headerName]: token } }))
}

export async function markAllRead(): Promise<void> {
  await withCsrf((headerName, token) =>
    apiClient.post('/notifications/read-all', null, { headers: { [headerName]: token } }))
}
