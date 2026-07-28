export interface NotificationItem {
  id: number
  notificationType: string
  actorUserId: number | null
  resourceType: string | null
  resourceId: number | null
  payload: Record<string, unknown> | null
  isRead: boolean
  readTime: string | null
  createTime: string
}

export interface NotificationListResponse {
  items: NotificationItem[]
  total: number
  page: number
  size: number
}
