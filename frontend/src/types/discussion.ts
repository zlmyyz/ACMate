export type PostType = 'SOLUTION' | 'QUESTION' | 'CONTEST_SUMMARY' | 'TRAINING_EXPERIENCE' | 'ANNOUNCEMENT' | 'OTHER'

export interface PostSummary {
  id: number
  title: string
  postType: PostType
  authorUserId: number
  authorUsername: string
  authorNickname: string
  authorAvatarUrl: string | null
  problemId: number | null
  problemTitle: string | null
  likeCount: number
  commentCount: number
  viewCount: number
  pinned: boolean
  active: boolean
  createTime: string
}

export interface CommentData {
  id: number
  userId: number
  username: string
  nickname: string
  avatarUrl: string | null
  replyToUserId: number | null
  replyToUsername: string | null
  content: string
  active: boolean
  replies: CommentData[]
  createTime: string
}

export interface PostDetail {
  id: number
  title: string
  contentMd: string
  postType: PostType
  authorUserId: number
  authorUsername: string
  authorNickname: string
  authorAvatarUrl: string | null
  problemId: number | null
  problemTitle: string | null
  trainingPlanId: number | null
  active: boolean
  pinned: boolean
  likeCount: number
  commentCount: number
  viewCount: number
  likedByMe: boolean
  comments: CommentData[]
  createTime: string
  updateTime: string
}

export interface CreatePostRequest {
  title: string
  contentMd: string
  postType: PostType
  problemId?: number
  trainingPlanId?: number
  broadcast?: boolean
}

export interface UpdatePostRequest {
  title?: string
  contentMd?: string
}

export interface CreateCommentRequest {
  content: string
  parentId?: number
  replyToUserId?: number
}

export interface PostListResponse {
  posts: PostSummary[]
  total: number
  page: number
  size: number
}
