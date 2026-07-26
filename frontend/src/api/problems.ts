import apiClient from './client'
import { withCsrf } from './csrf'
import type {
  ProblemSummary,
  ProblemDetail,
  PageResponse,
  ProblemQueryParams,
  CreateProblemRequest,
  UpdateProblemRequest,
} from '@/types/problem'

export async function getProblems(
  params: ProblemQueryParams,
): Promise<PageResponse<ProblemSummary>> {
  const response = await apiClient.get<PageResponse<ProblemSummary>>('/problems', { params })
  return response.data
}

export async function getProblemDetail(id: number): Promise<ProblemDetail> {
  const response = await apiClient.get<ProblemDetail>(`/problems/${id}`)
  return response.data
}

export async function createProblem(
  data: CreateProblemRequest,
): Promise<ProblemDetail> {
  return withCsrf((headerName, token) =>
    apiClient
      .post<ProblemDetail>('/problems', data, {
        headers: { [headerName]: token },
      })
      .then((r) => r.data),
  )
}

export async function updateProblem(
  id: number,
  data: UpdateProblemRequest,
): Promise<ProblemDetail> {
  return withCsrf((headerName, token) =>
    apiClient
      .put<ProblemDetail>(`/problems/${id}`, data, {
        headers: { [headerName]: token },
      })
      .then((r) => r.data),
  )
}
