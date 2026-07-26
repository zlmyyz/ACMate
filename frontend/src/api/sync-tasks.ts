import apiClient from './client'
import type { SyncTaskListResponse } from '@/types/sync-task'

export async function getSyncTasks(params: Record<string, unknown> = {}): Promise<SyncTaskListResponse> {
  const r = await apiClient.get<SyncTaskListResponse>('/admin/sync-tasks', { params })
  return r.data
}
