import apiClient from './client'
import { withCsrf } from './csrf'
import type { UserProfile, UpdateProfileRequest } from '@/types/user'

export async function getUserProfile(id: number): Promise<UserProfile> {
  const response = await apiClient.get<UserProfile>(`/users/${id}`)
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
