<script setup lang="ts">
import { computed, ref, watch, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getNowcoderAccount, bindNowcoderAccount, getProfileSync, enqueueProfileSync, retryProblemImports, type NowcoderAccount, type ProfileSyncState } from '@/api/nowcoder-training'
const props=defineProps<{ userId: number; publicUid?: string }>()
const emit=defineEmits<{ updated: [] }>()
const auth=useAuthStore(), self=computed(()=>auth.user?.id===props.userId)
const account=ref<NowcoderAccount|null>(null), state=ref<ProfileSyncState|null>(null)
const uid=ref(''), error=ref(''), busy=ref(false), loading=ref(false)
const working=computed(()=>['QUEUED','RUNNING'].includes(state.value?.status??'') || (state.value?.importPending??0)>0)
const statusLabels:Record<string,string>={NOT_STARTED:'尚未同步',QUEUED:'等待同步',RUNNING:'正在同步',COMPLETED:'公开历史同步完成',FAILED:'同步失败，可重试',CANCELLED:'任务已取消'}
let timer:ReturnType<typeof setTimeout>|undefined, generation=0
const errorText=(e:unknown)=>(e as {response?:{data?:{message?:string}}}).response?.data?.message || '牛客同步信息加载失败，请稍后重试'
function schedule(token:number) {
  clearTimeout(timer)
  if(token===generation && self.value && working.value)timer=setTimeout(()=>void poll(token),4000)
}
async function poll(token:number) {
  try { const value=await getProfileSync(); if(token!==generation)return; state.value=value; error.value=''; emit('updated') }
  catch(e){if(token===generation)error.value=errorText(e)}
  finally{schedule(token)}
}
async function load() {
  const token=++generation;clearTimeout(timer);account.value=null;state.value=null;error.value=''
  if(!self.value)return
  loading.value=true
  try {
    const [binding,progress]=await Promise.all([getNowcoderAccount(),getProfileSync()]);if(token!==generation)return
    account.value=binding;state.value=progress
    if(binding.verifyStatus===1 && progress.status==='NOT_STARTED'){const value=await enqueueProfileSync();if(token===generation)state.value=value}
  }catch(e){if(token===generation)error.value=errorText(e)}
  finally{if(token===generation){loading.value=false;schedule(token)}}
}
async function bind() {
  busy.value=true;error.value=''
  try{await bindNowcoderAccount(uid.value.trim());uid.value='';await load()}
  catch(e){error.value=errorText(e)}finally{busy.value=false}
}
async function sync(retry=false) {
  const token=generation;busy.value=true;error.value=''
  try {
    if(retry){await retryProblemImports();const value=await getProfileSync();if(token===generation)state.value=value}
    else {const value=await enqueueProfileSync();if(token===generation)state.value=value}
    schedule(token)
  }catch(e){if(token===generation)error.value=errorText(e)}finally{if(token===generation)busy.value=false}
}
watch(()=>[props.userId,self.value],load,{immediate:true})
onUnmounted(()=>{generation++;clearTimeout(timer)})
</script>
<template>
  <div v-if="self || publicUid" class="nowcoder-profile" aria-label="牛客账号同步">
    <div class="nowcoder-heading"><strong>牛客</strong><a v-if="publicUid || account?.uid" :href="`https://ac.nowcoder.com/acm/contest/profile/${publicUid || account?.uid}/practice-coding`" target="_blank" rel="noopener noreferrer">UID {{ publicUid || account?.uid }} ↗</a><RouterLink v-if="self" to="/settings/oj-account">管理绑定</RouterLink></div>
    <template v-if="self">
      <p v-if="loading" role="status">正在读取牛客绑定…</p>
      <form v-else-if="account && !account.hasAccount" class="nowcoder-bind" @submit.prevent="bind"><input v-model="uid" aria-label="牛客 UID" inputmode="numeric" pattern="[1-9][0-9]{0,11}" placeholder="牛客个人主页的数字 UID" required :disabled="busy"/><button :disabled="busy || !uid.trim()">{{ busy ? '绑定中…' : '绑定牛客账号' }}</button></form>
      <p v-else-if="account?.verifyStatus !== 1 && account?.hasAccount">{{ account.verifyStatus===2?'审核未通过，请到绑定页面检查账号。':'等待管理员审核，通过后会自动同步公开编程记录。' }}</p>
      <template v-else-if="account?.verifyStatus === 1">
        <div class="sync-progress"><span role="status">{{ statusLabels[state?.status??'NOT_STARTED'] || state?.status }}<template v-if="state?.totalPages"> · {{ Math.min(state.nextPage-1,state.totalPages) }} / {{ state.totalPages }} 页</template></span><button class="sync-nowcoder" :disabled="busy || ['QUEUED','RUNNING'].includes(state?.status??'')" @click="sync()">{{ busy?'处理中…':['QUEUED','RUNNING'].includes(state?.status??'')?'同步进行中':'同步牛客记录' }}</button></div>
        <p v-if="state">本次已读取 {{ state.fetched }} 条，新增 {{ state.inserted }} 条。题面导入：{{ state.importPending }} 待处理，{{ state.importDone }} 已完成<template v-if="state.importFailed">，{{ state.importFailed }} 失败 <button class="retry-imports" :disabled="busy" @click="sync(true)">重试失败题目</button></template>。</p>
        <p v-if="state?.error" role="alert">{{ state.error }}</p>
      </template>
      <p v-if="error" role="alert">{{ error }} <button @click="load">重新加载</button></p>
    </template>
    <p class="scope-notice">统计牛客个人主页公开的编程提交；已通过的题目按牛客题号去重后在后台导入。本站自测不计入正式提交。</p>
  </div>
</template>
<style scoped>
.nowcoder-profile{margin-top:24px;padding-top:20px;border-top:1px solid var(--color-border-subtle);font-size:13px;color:var(--color-on-surface-variant)}.nowcoder-heading,.sync-progress,.nowcoder-bind{display:flex;align-items:center;flex-wrap:wrap;gap:12px}.nowcoder-heading strong{color:var(--color-on-surface);font-size:15px}.nowcoder-heading>a:last-child{margin-left:auto}.nowcoder-profile a{color:var(--color-primary)}.nowcoder-profile p{line-height:1.8;margin:12px 0 0}.sync-progress,.nowcoder-bind{margin-top:14px}.sync-progress button{margin-left:auto}button{padding:7px 11px;border:1px solid var(--color-border-subtle);border-radius:6px;background:var(--color-surface-container-low);color:var(--color-on-surface);font:inherit;cursor:pointer}button:disabled{opacity:.5;cursor:not-allowed}.nowcoder-bind input{min-width:0;flex:1;padding:9px 11px;border:1px solid var(--color-border-subtle);border-radius:6px;background:var(--color-surface-container-lowest);color:var(--color-on-surface);font:inherit}.scope-notice{font-size:12px}.retry-imports{padding:2px 6px}[role=alert]{color:var(--color-status-error)}button:focus-visible,input:focus-visible,a:focus-visible{outline:2px solid var(--color-primary);outline-offset:2px}
</style>
