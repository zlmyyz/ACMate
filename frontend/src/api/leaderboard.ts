import apiClient from './client'
import type { LeaderboardResponse } from '@/types/leaderboard'

export async function getLeaderboard(period: string = 'total', page: number = 1, size: number = 20): Promise<LeaderboardResponse> {
  const r = await apiClient.get<LeaderboardResponse>('/leaderboard', { params: { period, page, size } })
  return r.data
}
