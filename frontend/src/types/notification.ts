export interface NotificationItem {
  id: number
  type: string
  title: string
  content: string | null
  resourceType: string | null
  resourceId: number | null
  isRead: boolean
  createTime: string
}

export interface NotificationListResponse {
  items: NotificationItem[]
  total: number
  page: number
  size: number
}
