import apiClient from './client'
import type { AuditLogListResponse } from '@/types/audit-log'

export interface AuditLogParams {
  page?: number
  size?: number
  actorKeyword?: string
  actionType?: string
  targetType?: string
  targetId?: number
  startTime?: string
  endTime?: string
}

export async function getAuditLogs(params: AuditLogParams = {}): Promise<AuditLogListResponse> {
  const r = await apiClient.get<AuditLogListResponse>('/admin/audit-logs', { params })
  return r.data
}
