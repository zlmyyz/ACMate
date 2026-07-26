export interface AuditLogItem {
  id: number
  operatorId: number
  action: string
  resourceType: string
  resourceId: number | null
  reason: string | null
  beforeState: string | null
  afterState: string | null
  createTime: string
}

export interface AuditLogListResponse {
  items: AuditLogItem[]
  total: number
  page: number
  size: number
}
