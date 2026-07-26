import apiClient from './client'
import type { LeaderboardEntry } from '@/types/leaderboard'

export async function getLeaderboard(period: string = 'total'): Promise<LeaderboardEntry[]> {
  const r = await apiClient.get<LeaderboardEntry[]>('/leaderboard', { params: { period } })
  return r.data
}
