export type PlanType = 'PERSONAL' | 'PUBLIC'
export type TimeStatus = 'NOT_STARTED' | 'ONGOING' | 'ENDED'
export type ListType = 'PUBLIC' | 'MY_CREATED' | 'MY_JOINED'
export type ProblemStatusType = 'NOT_STARTED' | 'CHALLENGING' | 'ACCEPTED'

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
  completedCount: number
  totalCount: number
  requiredCompletedCount: number
  requiredTotal: number
  lastAcceptedTime: string | null
  rank: number | null
  completionOrder: number | null
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
  myStatus: string | null
  performanceNote: string | null
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
  myProgress: ProgressSummary | null
  createTime: string
  updateTime: string
}

export interface ProgressSummary {
  requiredCompletedCount: number
  requiredTotal: number
  optionalCompletedCount: number
  optionalTotal: number
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

export interface UpdateStatusRequest {
  status: string
}

export interface UpdateNoteRequest {
  note: string | null
}

export interface MemberProgressItem {
  problemId: number
  problemTitle: string
  platform: string
  difficulty: string | null
  problemActive: boolean
  sortOrder: number
  required: boolean
  myStatus: string
  performanceNote: string | null
}

export interface MemberProgress {
  userId: number
  username: string | null
  nickname: string | null
  avatarUrl: string | null
  joinTime: string
  creator: boolean
  completedCount: number
  totalCount: number
  lastAcceptedTime: string | null
  rank: number | null
  completionOrder: number | null
  problems: MemberProgressItem[]
}

export interface PlanListResponse {
  plans: PlanSummary[]
  total: number
  page: number
  size: number
}
