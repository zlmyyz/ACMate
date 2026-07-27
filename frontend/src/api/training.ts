import apiClient from './client'
import { withCsrf } from './csrf'
import type {
  PlanDetail,
  PlanListResponse,
  CreatePlanRequest,
  UpdatePlanRequest,
  AddProblemRequest,
} from '@/types/training'

export async function listPlans(params: {
  type?: string
  timeStatus?: string
  keyword?: string
  page?: number
  size?: number
}): Promise<PlanListResponse> {
  const response = await apiClient.get<PlanListResponse>('/training-plans', { params })
  return response.data
}

export async function getPlanDetail(id: number): Promise<PlanDetail> {
  const response = await apiClient.get<PlanDetail>(`/training-plans/${id}`)
  return response.data
}

export async function createPlan(data: CreatePlanRequest): Promise<PlanDetail> {
  return withCsrf((headerName, token) =>
    apiClient.post<PlanDetail>('/training-plans', data, {
      headers: { [headerName]: token },
    }).then(r => r.data),
  )
}

export async function updatePlan(id: number, data: UpdatePlanRequest): Promise<PlanDetail> {
  return withCsrf((headerName, token) =>
    apiClient.put<PlanDetail>(`/training-plans/${id}`, data, {
      headers: { [headerName]: token },
    }).then(r => r.data),
  )
}

export async function deactivatePlan(id: number, reason?: string): Promise<void> {
  return withCsrf((headerName, token) =>
    apiClient.put(`/training-plans/${id}/deactivate`,
      reason ? { reason } : {},
      { headers: { [headerName]: token } },
    ),
  )
}

export async function restorePlan(id: number): Promise<void> {
  return withCsrf((headerName, token) =>
    apiClient.put(`/training-plans/${id}/restore`, null, {
      headers: { [headerName]: token },
    }),
  )
}

export async function addPlanProblem(id: number, data: AddProblemRequest): Promise<void> {
  return withCsrf((headerName, token) =>
    apiClient.post(`/training-plans/${id}/problems`, data, {
      headers: { [headerName]: token },
    }),
  )
}

export async function removePlanProblem(planId: number, problemId: number): Promise<void> {
  return withCsrf((headerName, token) =>
    apiClient.delete(`/training-plans/${planId}/problems/${problemId}`, {
      headers: { [headerName]: token },
    }),
  )
}

export async function joinPlan(id: number): Promise<void> {
  return withCsrf((headerName, token) =>
    apiClient.post(`/training-plans/${id}/members/me`, null, {
      headers: { [headerName]: token },
    }),
  )
}

export async function removeMember(planId: number, userId: number): Promise<void> {
  return withCsrf((headerName, token) =>
    apiClient.delete(`/training-plans/${planId}/members/${userId}`, {
      headers: { [headerName]: token },
    }),
  )
}
