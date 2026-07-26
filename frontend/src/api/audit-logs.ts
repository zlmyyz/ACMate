import apiClient from './client'
import type { AuditLogListResponse } from '@/types/audit-log'

export async function getAuditLogs(params: Record<string, unknown> = {}): Promise<AuditLogListResponse> {
  const r = await apiClient.get<AuditLogListResponse>('/admin/audit-logs', { params })
  return r.data
}
