<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getNowcoderAccount, bindNowcoderAccount, unbindNowcoderAccount, type NowcoderAccount } from '@/api/nowcoder-training'
const account = ref<NowcoderAccount | null>(null)
const uid = ref(''), error = ref(''), busy = ref(false)
async function load() { try { account.value = await getNowcoderAccount() } catch { error.value = '牛客绑定信息加载失败' } }
async function bind() {
  busy.value = true; error.value = ''
  try { await bindNowcoderAccount(uid.value.trim()); uid.value = ''; await load() }
  catch(e: unknown) { error.value = (e as {response?:{data?:{message?:string}}}).response?.data?.message || '绑定失败' }
  finally { busy.value = false }
}
async function unbind() {
  busy.value = true; error.value = ''
  try { await unbindNowcoderAccount(); await load() }
  catch { error.value = '解绑失败，请稍后重试' }
  finally { busy.value = false }
}
onMounted(load)
</script>
<template>
<section class="nowcoder-account">
  <h2>我的牛客账号</h2>
  <p class="hint">绑定牛客个人主页中的数字 UID。管理员审核通过后，可同步公开竞赛题的正式提交结果。</p>
  <div v-if="account?.hasAccount" class="account-detail">
    <a :href="`https://ac.nowcoder.com/acm/contest/profile/${account.uid}`" target="_blank" rel="noopener noreferrer">牛客 UID：{{ account.uid }} ↗</a>
    <span>{{ account.verifyStatus === 1 ? '已通过审核' : account.verifyStatus === 2 ? '审核未通过' : '等待管理员审核' }}</span>
    <button type="button" :disabled="busy" @click="unbind">解绑</button>
  </div>
  <form v-else class="bind-row" @submit.prevent="bind"><input v-model="uid" aria-label="牛客 UID" inputmode="numeric" placeholder="如：123456789" pattern="[1-9][0-9]{0,11}" required :disabled="busy"/><button :disabled="busy || !uid.trim()">{{ busy ? '绑定中…' : '绑定牛客账号' }}</button></form>
  <p v-if="error" role="alert">{{ error }}</p>
</section>
</template>
<style scoped>
.nowcoder-account{padding:22px;border:1px solid var(--color-border-subtle);border-radius:var(--radius-lg);background:var(--color-surface-card);margin-bottom:24px}.nowcoder-account h2{font-size:var(--text-headline-sm);margin-bottom:12px}.hint{color:var(--color-on-surface-variant);line-height:1.7;margin-bottom:16px}.account-detail,.bind-row{display:flex;align-items:center;gap:16px;flex-wrap:wrap}.bind-row input{padding:10px 12px;border:1px solid var(--color-border-subtle);border-radius:6px;background:var(--color-surface-container-lowest);color:var(--color-on-surface)}button{padding:9px 14px;background:var(--color-primary);color:var(--color-on-primary);border-radius:6px}a{color:var(--color-primary)}[role=alert]{color:var(--color-status-error);margin-top:12px}
</style>
