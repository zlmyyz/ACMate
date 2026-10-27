import { ref, watch, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getEditorSettings, saveEditorSettings } from '@/api/editor-settings'
import { practiceLanguages } from '@/constants/practice-languages'

export function useEditorSettings() {
  const auth=useAuthStore(), autocomplete=ref(true), language=ref('CPP17'), settingsMessage=ref('')
  const validLanguage=(value:unknown): value is string => typeof value==='string' && Object.prototype.hasOwnProperty.call(practiceLanguages,value)
  let generation=0, revision=0, saving=false, disposed=false, dirty=false
  const cacheKey=(id:number)=>`acmate:editor-settings:${id}`
  function cache(id:number){try{localStorage.setItem(cacheKey(id),JSON.stringify({autocomplete:autocomplete.value,language:language.value}))}catch{}}
  watch(()=>auth.user?.id,async id=>{
    const token=++generation, change=++revision;dirty=false;autocomplete.value=true;language.value='CPP17';settingsMessage.value=''
    if(!id)return
    try{const saved=JSON.parse(localStorage.getItem(cacheKey(id))??'null');if(typeof saved?.autocomplete==='boolean')autocomplete.value=saved.autocomplete;if(validLanguage(saved?.language))language.value=saved.language}catch{}
    try{const settings=await getEditorSettings();if(token===generation&&change===revision&&!disposed){autocomplete.value=settings.autocomplete;if(validLanguage(settings.language))language.value=settings.language;cache(id)}}
    catch{if(token===generation&&!disposed)settingsMessage.value='暂时无法读取账号设置，使用本机记忆'}
  },{immediate:true})
  async function persist() {
    if(saving||disposed)return
    const id=auth.user?.id;if(!id)return
    saving=true
    const token=generation
    try {
      let change:number
      do{
        change=revision;const value=autocomplete.value, selectedLanguage=language.value;settingsMessage.value='编辑器设置同步中…'
        await saveEditorSettings(value,id,selectedLanguage)
        if(token!==generation||auth.user?.id!==id)return
      }while(change!==revision)
      dirty=false;settingsMessage.value='编辑器设置已同步到账号'
    }catch{if(token===generation&&!disposed)settingsMessage.value='设置已在本机记忆，账号同步失败，点击重试'}
    finally{saving=false;if(token!==generation&&dirty&&!disposed&&auth.user?.id)void persist()}
  }
  function changed(){revision++;dirty=true;const id=auth.user?.id;if(id)cache(id);void persist()}
  function setAutocomplete(value:boolean){autocomplete.value=value;changed()}
  function setLanguage(value:string){if(validLanguage(value)&&value!==language.value){language.value=value;changed()}}
  // Finish a pending save after navigating away, but never carry it into a new account.
  onUnmounted(()=>{disposed=true})
  return {autocomplete,language,settingsMessage,setAutocomplete,setLanguage,retrySettings:persist}
}

