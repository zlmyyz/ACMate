import apiClient from './client'
import { withCsrf } from './csrf'
import type {
  PostDetail, PostListResponse, CreatePostRequest,
  UpdatePostRequest, CreateCommentRequest, CommentData,
} from '@/types/discussion'

export async function listPosts(params: {
  postType?: string; problemId?: number; keyword?: string; page?: number; size?: number
}): Promise<PostListResponse> {
  const r = await apiClient.get<PostListResponse>('/posts', { params })
  return r.data
}

export async function getPostDetail(id: number): Promise<PostDetail> {
  const r = await apiClient.get<PostDetail>(`/posts/${id}`)
  return r.data
}

export async function createPost(data: CreatePostRequest): Promise<PostDetail> {
  return withCsrf((h, t) => apiClient.post<PostDetail>('/posts', data, { headers: { [h]: t } }).then(r => r.data))
}

export async function updatePost(id: number, data: UpdatePostRequest): Promise<PostDetail> {
  return withCsrf((h, t) => apiClient.put<PostDetail>(`/posts/${id}`, data, { headers: { [h]: t } }).then(r => r.data))
}

export async function deletePost(id: number): Promise<void> {
  return withCsrf((h, t) => apiClient.delete(`/posts/${id}`, { headers: { [h]: t } }))
}

export async function addComment(postId: number, data: CreateCommentRequest): Promise<CommentData> {
  return withCsrf((h, t) => apiClient.post<CommentData>(`/posts/${postId}/comments`, data, { headers: { [h]: t } }).then(r => r.data))
}

export async function deleteComment(postId: number, commentId: number): Promise<void> {
  return withCsrf((h, t) => apiClient.delete(`/posts/${postId}/comments/${commentId}`, { headers: { [h]: t } }))
}

export async function toggleLike(postId: number): Promise<void> {
  return withCsrf((h, t) => apiClient.post(`/posts/${postId}/like`, null, { headers: { [h]: t } }))
}
