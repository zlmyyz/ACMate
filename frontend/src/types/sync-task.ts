export interface SyncTaskItem {
  id: number
  ojAccountId: number
  platform: string
  triggerType: string
  taskStatus: string
  fetchedCount: number
  insertedCount: number
  firstAcCount: number
  errorMessage: string | null
  startTime: string | null
  endTime: string | null
}

export interface SyncTaskListResponse {
  items: SyncTaskItem[]
  total: number
  page: number
  size: number
}
