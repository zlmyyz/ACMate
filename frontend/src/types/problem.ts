export interface ProblemSummary {
  id: number
  platform: string
  externalProblemKey: string | null
  title: string
  sourceUrl: string | null
  difficulty: string | null
  tags: string | null
  creatorUserId: number
  creatorUsername: string | null
  creatorNickname: string | null
  createTime: string
}

export interface ProblemDetail {
  id: number
  platform: string
  externalProblemKey: string | null
  title: string
  sourceUrl: string | null
  difficulty: string | null
  tags: string | null
  contentMd: string | null
  creatorUserId: number
  creatorUsername: string | null
  creatorNickname: string | null
  active: boolean
  deactivationSource: string | null
  createTime: string
  updateTime: string
}

export interface PageResponse<T> {
  page: number
  size: number
  total: number
  pages: number
  records: T[]
}

export interface ProblemQueryParams {
  page?: number
  size?: number
  platform?: string
  difficulty?: string
  keyword?: string
  creatorUserId?: number
}

export interface CreateProblemRequest {
  platform: string
  externalProblemKey?: string
  title: string
  sourceUrl?: string
  difficulty?: string
  tags?: string
  contentMd?: string
}

export type UpdateProblemRequest = CreateProblemRequest

export type ProblemStatusView = 'ACTIVE' | 'INACTIVE'

export type MineProblemStatusFilter = 'ALL' | 'ACTIVE' | 'INACTIVE'

export interface MyProblemSummary {
  id: number
  platform: string
  externalProblemKey: string | null
  title: string
  sourceUrl: string | null
  difficulty: string | null
  tags: string | null
  status: ProblemStatusView
  createTime: string
  updateTime: string
}

export interface MyProblemQueryParams {
  page?: number
  size?: number
  platform?: string
  difficulty?: string
  keyword?: string
  status?: MineProblemStatusFilter
}

export interface AdminProblemSummary {
  id: number
  platform: string
  externalProblemKey: string | null
  title: string
  sourceUrl: string | null
  difficulty: string | null
  tags: string | null
  status: ProblemStatusView
  deactivationSource: string | null
  deactivationReason: string | null
  deactivatedBy: number | null
  deactivationTime: string | null
  creatorUserId: number
  creatorUsername: string | null
  creatorNickname: string | null
  createTime: string
  updateTime: string
}

export interface AdminProblemQueryParams {
  page?: number
  size?: number
  platform?: string
  difficulty?: string
  keyword?: string
  creatorUserId?: number
  status?: MineProblemStatusFilter
}

export interface AdminDeactivateRequest {
  reason: string
}
