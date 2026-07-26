export interface LeaderboardEntry {
  rank: number
  userId: number
  username: string
  nickname: string
  avatarUrl: string | null
  solvedCount: number
  isMe: boolean
}

export interface LeaderboardResponse {
  entries: LeaderboardEntry[]
  total: number
  page: number
  size: number
}
