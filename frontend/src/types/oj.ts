export interface MyAccount {
  hasAccount: boolean
  id?: number
  platform?: string
  externalUserId?: string
  displayName?: string
  verifyStatus?: number
  syncEnabled?: number
  lastSyncTime?: string | null
  lastSyncSuccess?: number | null
}

export interface PendingAccount {
  id: number
  userId: number
  platform: string
  externalUserId: string
  displayName: string
  verifyStatus: number
  syncEnabled: number
  lastSyncTime: string | null
}
