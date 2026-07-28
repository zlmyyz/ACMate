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

export interface SyncResult {
  accountId: number
  handle: string
  fetchedCount: number
  insertedCount: number
  acceptedCount: number
  newAcceptedProblemCount: number
  lastSyncTime: string | null
  syncStatus: string
}
