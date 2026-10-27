<script setup lang="ts">
import { ref, useId, onUnmounted } from 'vue'
import { accountError } from '@/api/account'
const props = defineProps<{ email: string; send: () => Promise<{ message: string }>; disabled?: boolean; label?: string }>()
const code = defineModel<string>({ default: '' })
const id = useId()
const sending = ref(false)
const countdown = ref(0)
const error = ref('')
const notice = ref('')
let timer: ReturnType<typeof setInterval> | undefined
onUnmounted(() => clearInterval(timer))
async function sendCode() {
  error.value = ''; notice.value = ''
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(props.email.trim())) { error.value = '请输入有效的邮箱地址'; return }
  sending.value = true
  try {
    notice.value = (await props.send()).message
    countdown.value = 60
    clearInterval(timer)
    timer = setInterval(() => { if (--countdown.value <= 0) clearInterval(timer) }, 1000)
  } catch (e) { error.value = accountError(e, '验证码发送失败') }
  finally { sending.value = false }
}
</script>
<template>
  <div class="email-code-field">
    <label :for="id">{{ label || '邮箱验证码' }} <span aria-hidden="true">*</span></label>
    <div class="code-controls">
      <input :id="id" v-model="code" type="text" inputmode="numeric" autocomplete="one-time-code" required pattern="[0-9]{6}" maxlength="6" placeholder="六位验证码" :disabled="disabled" />
      <button type="button" :disabled="disabled || sending || countdown > 0" @click="sendCode">{{ sending ? '发送中…' : countdown > 0 ? `${countdown} 秒后重发` : '获取验证码' }}</button>
    </div>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
  </div>
</template>
<style scoped>
.email-code-field { display: grid; gap: 8px; font-size: var(--text-body-md); }
label { font-weight: 600; color: var(--color-on-surface); }
label span,.error { color: var(--color-status-critical); }
.code-controls { display: flex; gap: 8px; min-width: 0; }
input { flex: 1; width: 0; min-width: 0; padding: 11px 14px; background: var(--color-surface-container-lowest); color: var(--color-on-surface); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); font: inherit; }
input:focus { outline: 2px solid var(--color-primary-container); outline-offset: 1px; }
button { padding: 8px 12px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); background: var(--color-surface-container-lowest); color: var(--color-on-surface); font: inherit; cursor: pointer; }
button:disabled { opacity: .5; cursor: not-allowed; }
p { margin: 0; font-size: var(--text-body-sm); line-height: 1.6; }
.notice { color: var(--color-on-surface-variant); }
</style>
