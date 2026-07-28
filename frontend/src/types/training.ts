export type PlanType = 'PERSONAL' | 'PUBLIC'
export type TimeStatus = 'NOT_STARTED' | 'ONGOING' | 'ENDED'
export type ListType = 'PUBLIC' | 'MY_CREATED' | 'MY_JOINED'

export interface PlanSummary {
  id: number
  title: string
  planType: PlanType
  active: boolean
  creatorUsername: string
  creatorNickname: string
  startTime: string | null
  endTime: string | null
  timeStatus: TimeStatus
  problemCount: number
  memberCount: number
  createTime: string
}

export interface PlanMember {
  userId: number
  username: string | null
  nickname: string | null
  avatarUrl: string | null
  joinTime: string
  creator: boolean
}

export interface PlanProblem {
  id: number
  problemId: number
  problemTitle: string
  platform: string
  difficulty: string | null
  problemActive: boolean
  sortOrder: number
  required: boolean
}

export interface PlanDetail {
  id: number
  title: string
  description: string | null
  planType: PlanType
  active: boolean
  deactivationSource: string | null
  deactivationReason: string | null
  creatorUserId: number
  creatorUsername: string
  creatorNickname: string
  startTime: string | null
  endTime: string | null
  timeStatus: TimeStatus
  problemCount: number
  memberCount: number
  joined: boolean
  creator: boolean
  canEdit: boolean
  canJoin: boolean
  canRemoveMembers: boolean
  canDeactivate: boolean
  canRestore: boolean
  members: PlanMember[]
  problems: PlanProblem[]
  createTime: string
  updateTime: string
}

export interface CreatePlanRequest {
  title: string
  description?: string
  startTime?: string
  endTime?: string
  planType?: PlanType
  problemIds?: number[]
}

export interface UpdatePlanRequest {
  title?: string
  description?: string
  startTime?: string
  endTime?: string
}

export interface PlanProblemRequest {
  problemId: number
  sortOrder?: number
}

export interface UpdateProblemsRequest {
  problems: PlanProblemRequest[]
}

export interface DeactivateRequest {
  reason?: string
}

export interface AddProblemRequest {
  problemId: number
  sortOrder: number
  requiredFlag: number
}

export interface PlanListResponse {
  plans: PlanSummary[]
  total: number
  page: number
  size: number
}
