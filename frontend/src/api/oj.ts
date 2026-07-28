import apiClient from './client'
import { withCsrf } from './csrf'
import type { MyAccount, PendingAccount, SyncResult } from '@/types/oj'

export async function getMyAccount(): Promise<MyAccount> {
  const r = await apiClient.get<MyAccount>('/oj-accounts/me')
  return r.data
}

export async function bindAccount(handle: string): Promise<void> {
  return withCsrf((h, t) => apiClient.post('/oj-accounts', { handle }, { headers: { [h]: t } }))
}

export async function unbindAccount(): Promise<void> {
  return withCsrf((h, t) => apiClient.delete('/oj-accounts/me', { headers: { [h]: t } }))
}

export async function getPendingAccounts(): Promise<PendingAccount[]> {
  const r = await apiClient.get<PendingAccount[]>('/oj-accounts/admin')
  return r.data
}

export async function verifyAccount(id: number, status: number): Promise<void> {
  return withCsrf((h, t) => apiClient.post(`/oj-accounts/admin/${id}/verify`, null, {
    params: { status },
    headers: { [h]: t },
  }))
}

export async function syncMyAccount(): Promise<SyncResult> {
  return withCsrf((h, t) => apiClient.post<SyncResult>('/oj-accounts/me/sync', null, { headers: { [h]: t } }))
}
