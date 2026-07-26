import apiClient from './client'
import { withCsrf } from './csrf'
import type {
  ProblemSummary,
  ProblemDetail,
  PageResponse,
  ProblemQueryParams,
  CreateProblemRequest,
  UpdateProblemRequest,
  MyProblemSummary,
  MyProblemQueryParams,
  AdminProblemSummary,
  AdminProblemQueryParams,
  AdminDeactivateRequest,
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

export async function getMyProblems(
  params: MyProblemQueryParams,
): Promise<PageResponse<MyProblemSummary>> {
  const response = await apiClient.get<PageResponse<MyProblemSummary>>('/problems/mine', { params })
  return response.data
}

export async function deactivateProblem(id: number): Promise<void> {
  return withCsrf((headerName, token) =>
    apiClient.post(`/problems/${id}/deactivate`, null, {
      headers: { [headerName]: token },
    }),
  )
}

export async function restoreProblem(id: number): Promise<void> {
  return withCsrf((headerName, token) =>
    apiClient.post(`/problems/${id}/restore`, null, {
      headers: { [headerName]: token },
    }),
  )
}

export async function getAdminProblems(
  params: AdminProblemQueryParams,
): Promise<PageResponse<AdminProblemSummary>> {
  const response = await apiClient.get<PageResponse<AdminProblemSummary>>('/admin/problems', { params })
  return response.data
}

export async function adminDeactivateProblem(id: number, reason: string): Promise<void> {
  return withCsrf((headerName, token) =>
    apiClient.post(`/admin/problems/${id}/deactivate`, { reason } satisfies AdminDeactivateRequest, {
      headers: { [headerName]: token },
    }),
  )
}

export async function adminRestoreProblem(id: number): Promise<void> {
  return withCsrf((headerName, token) =>
    apiClient.post(`/admin/problems/${id}/restore`, null, {
      headers: { [headerName]: token },
    }),
  )
}
