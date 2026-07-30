export interface OjStats {
  solvedCount: number
  solvedCount30d: number
  solvedCount7d: number
  lastAcceptedTime: string | null
}

export interface PublicPlanSummary {
  id: number
  title: string
  timeStatus: string
  startTime: string | null
  endTime: string | null
  problemCount: number
  memberCount: number
  createTime: string
}

export interface UserProfile {
  id: number
  username: string
  nickname: string
  avatarUrl: string | null
  bio: string | null
  admin: boolean
  accountStatus: string
  createdProblemCount: number
  codeforcesHandle: string | null
  ojStats: OjStats | null
  createTime: string
}

export interface UpdateProfileRequest {
  nickname?: string
  bio?: string
}
