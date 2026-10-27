import api from './client'
import { withCsrf } from './csrf'
export interface NowcoderAccount { hasAccount: boolean; id: number | null; uid: string | null; verifyStatus: number | null }
export interface TrainingInfo { officialUrl: string; syncSupported: boolean; syncNotice: string }
export interface OfficialRecord { remoteId: string; verdict: string; language: string; submittedTime: string }
export interface PublicSample { input: string; output: string }
export interface PracticeResult { status: string; stdout: string; stderr: string; timeMs: number; memoryKb: number }
export interface SyncResult { fetched: number; inserted: number; accepted: number; message: string }
export interface ProfileSyncState { status: string; nextPage: number; totalPages: number; fetched: number; inserted: number; error: string | null; updatedAt: string | null; importPending: number; importDone: number; importFailed: number }
export const getProfileSync = async (): Promise<ProfileSyncState> => (await api.get('/nowcoder/profile-sync')).data
export const enqueueProfileSync = (): Promise<ProfileSyncState> => withCsrf(async (h,t) => (await api.post('/nowcoder/profile-sync',null,{headers:{[h]:t}})).data)
export const retryProblemImports = () => withCsrf(async (h,t) => { await api.post('/nowcoder/profile-sync/retry-imports',null,{headers:{[h]:t}}) })
export const getNowcoderAccount = async (): Promise<NowcoderAccount> => (await api.get('/nowcoder/account')).data
export const bindNowcoderAccount = (uid: string) => withCsrf(async (h,t) => { await api.post('/nowcoder/account',{uid},{headers:{[h]:t}}) })
export const unbindNowcoderAccount = () => withCsrf(async (h,t) => { await api.delete('/nowcoder/account',{headers:{[h]:t}}) })
export const getTrainingInfo = async (id: number): Promise<TrainingInfo> => (await api.get(`/nowcoder/problems/${id}`)).data
export const getOfficialRecords = async (id: number): Promise<OfficialRecord[]> => (await api.get(`/nowcoder/problems/${id}/submissions`)).data
export const syncOfficialRecords = (id: number): Promise<SyncResult> => withCsrf(async (h,t) => (await api.post(`/nowcoder/problems/${id}/sync`,null,{headers:{[h]:t}})).data)
export const getPublicSamples = async (id: number): Promise<PublicSample[]> => (await api.get(`/practice/problems/${id}/samples`)).data
export const getPracticeStatus = async (): Promise<{ available: boolean }> => (await api.get('/practice/status')).data
export const runPractice = (problemId: number,sampleIndex: number,language: string,sourceCode: string,input?:string): Promise<PracticeResult> => withCsrf(async (h,t) => (await api.post('/practice/run',input===undefined?{problemId,sampleIndex,language,sourceCode,inputMode:'PUBLIC_SAMPLE'}:{problemId,language,sourceCode,inputMode:'CUSTOM',input},{headers:{[h]:t}})).data)
