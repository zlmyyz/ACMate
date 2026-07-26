export interface AdminPostItem {
  id: number
  title: string
  authorUserId: number
  postType: string
  status: number
  likeCount: number
  commentCount: number
  deactivationSource: string | null
  deactivationReason: string | null
  createTime: string
}

export interface AdminPostListResponse {
  items: AdminPostItem[]
  total: number
  page: number
  size: number
}

export interface AdminCommentItem {
  id: number
  postId: number
  userId: number
  content: string
  status: number
  deactivationSource: string | null
  deactivationReason: string | null
  createTime: string
}

export interface AdminCommentListResponse {
  items: AdminCommentItem[]
  total: number
  page: number
  size: number
}
