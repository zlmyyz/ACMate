export interface UserProfile {
  id: number
  username: string
  nickname: string
  avatarUrl: string | null
  bio: string | null
  admin: boolean
  problemCount: number
  createTime: string
}

export interface UpdateProfileRequest {
  nickname?: string
  bio?: string
}
