import api from './client'
import { withCsrf } from './csrf'
export interface EditorSettings { autocomplete: boolean; language: string }
export const getEditorSettings = async (): Promise<EditorSettings> => (await api.get('/users/me/editor-settings')).data
export const saveEditorSettings = (autocomplete: boolean,accountId:number,language:string): Promise<EditorSettings> => withCsrf(async (h,t) => (await api.put('/users/me/editor-settings',{autocomplete,accountId,language},{headers:{[h]:t}})).data)
