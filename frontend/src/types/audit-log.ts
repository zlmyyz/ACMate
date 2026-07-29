export interface AuditLogResponse {
  id: number
  actionType: string
  actorUserId: number
  actorUsername: string
  actorNickname: string | null
  targetType: string
  targetId: number | null
  beforeState: string | null
  afterState: string | null
  reason: string | null
  createTime: string
}

export interface AuditLogListResponse {
  items: AuditLogResponse[]
  total: number
  page: number
  size: number
}
