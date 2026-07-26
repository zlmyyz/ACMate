export interface ProblemSummary {
  id: number
  platform: string
  externalProblemKey: string | null
  title: string
  sourceUrl: string | null
  difficulty: string | null
  tags: string | null
  creatorUserId: number
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
