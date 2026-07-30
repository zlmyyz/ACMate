import apiClient from './client'
import { withCsrf } from './csrf'
import type { UserProfile, UpdateProfileRequest, PublicPlanSummary } from '@/types/user'
import type { ProblemSummary, PageResponse } from '@/types/problem'

export async function getUserProfile(id: number): Promise<UserProfile> {
  const response = await apiClient.get<UserProfile>(`/users/${id}`)
  return response.data
}

export async function getUserProblems(id: number, page = 1, size = 20): Promise<PageResponse<ProblemSummary>> {
  const response = await apiClient.get<PageResponse<ProblemSummary>>(`/users/${id}/problems`, {
    params: { page, size },
  })
  return response.data
}

export async function getUserTrainingPlans(id: number, page = 1, size = 20): Promise<{
  plans: PublicPlanSummary[]
  total: number
  page: number
  size: number
}> {
  const response = await apiClient.get<{
    plans: PublicPlanSummary[]
    total: number
    page: number
    size: number
  }>(`/users/${id}/training-plans`, {
    params: { page, size },
  })
  return response.data
}

export async function updateProfile(data: UpdateProfileRequest): Promise<void> {
  return withCsrf((headerName, token) =>
    apiClient.put('/users/me/profile', data, {
      headers: { [headerName]: token },
    }),
  )
}

export async function uploadAvatar(file: File): Promise<UserProfile> {
  return withCsrf((headerName, token) => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient
      .post<UserProfile>('/users/me/avatar', formData, {
        headers: { [headerName]: token },
      })
      .then((r) => r.data)
  })
}
