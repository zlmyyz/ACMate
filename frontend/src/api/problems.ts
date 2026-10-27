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

export interface ProblemReview { id:number; problem_id:number; author_user_id:number; original_content?:string; proposed_content?:string; status:string; reviewer_user_id:number|null; review_time:string|null; review_note:string|null; create_time:string }
export const submitProblemReview = (id:number,data:UpdateProblemRequest) => withCsrf((h,t) => apiClient.post(`/problem-reviews/${id}`,data,{headers:{[h]:t}}))
export const getProblemReviews = async (id:number,page=1):Promise<PageResponse<ProblemReview>> => (await apiClient.get(`/problem-reviews/problem/${id}`,{params:{page}})).data
export const getPendingReviews = async (page=1):Promise<PageResponse<ProblemReview>> => (await apiClient.get('/problem-reviews/pending',{params:{page}})).data
export const getProblemReviewDetail = async (id:number):Promise<{id:number;original_content:string;proposed_content:string}> => (await apiClient.get(`/problem-reviews/${id}`)).data
export const decideProblemReview = (id:number,decision:'approve'|'reject',note:string) => withCsrf((h,t) => apiClient.post(`/problem-reviews/${id}/${decision}`,{note},{headers:{[h]:t}}))

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

export async function importNowcoderProblem(url: string): Promise<import('@/types/problem').ImportedProblemPreview> {
  return withCsrf((headerName, token) => apiClient.post<import('@/types/problem').ImportedProblemPreview>(
    '/problems/import/nowcoder', { url }, { headers: { [headerName]: token } },
  ).then(response => response.data))
}
