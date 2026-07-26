export interface LeaderboardEntry {
  rank: number
  userId: number
  username: string
  nickname: string
  avatarUrl: string | null
  solvedCount: number
  isMe: boolean
}
