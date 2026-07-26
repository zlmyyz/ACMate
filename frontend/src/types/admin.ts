export interface AdminUser {
  id: number
  username: string
  nickname: string
  email: string
  avatarUrl: string | null
  bio: string
  admin: boolean
  status: number
  createTime: string | null
  lastLoginTime: string | null
}

export interface AdminUserListResponse {
  users: AdminUser[]
  total: number
  page: number
  size: number
}
