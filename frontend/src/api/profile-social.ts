import api from './client'
import { withCsrf } from './csrf'
export interface Contributions { views: number; yesterdayViews: number; likes: number; yesterdayLikes: number; favorites: number; yesterdayFavorites: number }
export interface SocialSummary { followingCount: number; followerCount: number; following: boolean; ipRegion: string | null; contributions: Contributions }
export interface SocialMember { id: number; username: string; nickname: string; avatarUrl: string | null; following: boolean }
export interface FollowPage { page: number; size: number; total: number; pages: number; records: SocialMember[] }
export interface SubmissionActivity { start: string; end: string; submissions: number; activeDays: number; days: { date: string; count: number }[] }
export interface RecentSubmission { id: number; platform: string; problemKey: string; problemId: number | null; title: string; verdict: string; language: string | null; submittedTime: string }
export const getSocialSummary = async (id: number): Promise<SocialSummary> => (await api.get(`/users/${id}/summary`)).data
export const getFollowList = async (id: number, kind: 'following' | 'followers', page = 1): Promise<FollowPage> => (await api.get(`/users/${id}/${kind}`, { params: { page, size: 20 } })).data
export const setFollow = (id: number, follow: boolean): Promise<void> => withCsrf(async (h, t) => { await api.request({ method: follow ? 'PUT' : 'DELETE', url: `/users/${id}/follow`, headers: { [h]: t } }) })
export type OjPlatform = 'ALL' | 'CODEFORCES' | 'NOWCODER'
export interface OjOverview { accounts: { platform: string; uid: string; lastSyncTime: string | null }[]; stats: import('@/types/user').OjStats; todaySubmissions: number; todayAttempted: number; todaySolved: number }
export const getOjOverview = async (id: number, platform: OjPlatform = 'ALL'): Promise<OjOverview> => (await api.get(`/users/${id}/oj-overview`, { params: { platform } })).data
export const getSubmissionActivity = async (id: number, platform?: OjPlatform): Promise<SubmissionActivity> => (await api.get(`/users/${id}/activity`, { params: { platform } })).data
export const getRecentSubmissions = async (id: number, platform?: OjPlatform): Promise<RecentSubmission[]> => (await api.get(`/users/${id}/submissions/recent`, { params: { platform } })).data
export interface NowcoderHistoryAccount { uid: string; submissions: number }
export const getNowcoderHistoryAccounts = async (id:number):Promise<NowcoderHistoryAccount[]> => (await api.get(`/users/${id}/nowcoder-history`)).data
export const getNowcoderHistoryOverview = async (id:number,uid:string):Promise<OjOverview> => (await api.get(`/users/${id}/nowcoder-history/${uid}/overview`)).data
export const getNowcoderHistoryActivity = async (id:number,uid:string):Promise<SubmissionActivity> => (await api.get(`/users/${id}/nowcoder-history/${uid}/activity`)).data
export const getNowcoderHistoryRecent = async (id:number,uid:string):Promise<RecentSubmission[]> => (await api.get(`/users/${id}/nowcoder-history/${uid}/recent`)).data
export interface FavoriteState { favorited: boolean; count: number }
export const getFavorite = async (id: number): Promise<FavoriteState> => (await api.get(`/posts/${id}/favorite`)).data
export const setFavorite = (id: number, value: boolean): Promise<FavoriteState> => withCsrf(async (h, t) => (await api.request<FavoriteState>({ method: value ? 'PUT' : 'DELETE', url: `/posts/${id}/favorite`, headers: { [h]: t } })).data)
