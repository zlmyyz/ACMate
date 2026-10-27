<script setup lang="ts">
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { siteBrand } from '@/constants/site'
import { accountError, resetPassword, sendResetCode } from '@/api/account'
import EmailCodeField from '@/components/common/EmailCodeField.vue'
const auth = useAuthStore()
const email = ref(auth.user?.email ?? '')
const code = ref('')
const password = ref('')
const confirmation = ref('')
const busy = ref(false)
const done = ref(false)
const error = ref('')
async function reset() {
  error.value = ''
  if (password.value !== confirmation.value) { error.value = '两次输入的密码不一致'; return }
  busy.value = true
  try { await resetPassword(email.value.trim(), code.value, password.value); auth.user = null; done.value = true; password.value = ''; confirmation.value = ''; code.value = '' }
  catch (e) { error.value = accountError(e, '重置密码失败，请重新获取验证码后重试') }
  finally { busy.value = false }
}
</script>
<template>
  <main class="recovery-page"><section class="recovery-card">
    <RouterLink :to="auth.isLoggedIn ? '/' : '/login'" class="brand">{{ siteBrand.clubName }}</RouterLink>
    <h1>找回密码</h1>
    <template v-if="done"><p role="status">密码已重置，其他登录会话已失效。请使用原用户名和新密码登录，也可以使用邮箱验证码登录。</p><RouterLink to="/login">返回登录</RouterLink></template>
    <form v-else @submit.prevent="reset">
      <p class="hint">通过账号已绑定的邮箱验证身份，设置新密码。</p>
      <label for="reset-email">邮箱</label><input id="reset-email" v-model="email" type="email" autocomplete="email" maxlength="128" required :disabled="busy" placeholder="请输入已绑定的邮箱" />
      <EmailCodeField v-model="code" :email="email" :send="() => sendResetCode(email.trim())" :disabled="busy" />
      <label for="new-password">新密码</label><input id="new-password" v-model="password" type="password" autocomplete="new-password" minlength="8" maxlength="64" required :disabled="busy" placeholder="8–64 位密码" />
      <label for="confirm-password">确认新密码</label><input id="confirm-password" v-model="confirmation" type="password" autocomplete="new-password" minlength="8" maxlength="64" required :disabled="busy" placeholder="请再次输入新密码" />
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button :disabled="busy">{{ busy ? '重置中…' : '重置密码' }}</button>
      <RouterLink class="recovery-back" :to="auth.isLoggedIn ? '/' : '/login'">{{ auth.isLoggedIn ? '返回主页' : '返回登录' }}</RouterLink>
    </form>
  </section></main>
</template>
<style scoped>
.recovery-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: var(--color-surface-background); }
.recovery-card { width: 100%; max-width: 460px; padding: 36px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); background: var(--color-surface-card); box-shadow: var(--shadow-card); }
h1 { margin: 20px 0; font-size: var(--text-headline-md); } form { display: grid; gap: 12px; }
input { padding: 12px 14px; background: var(--color-surface-container-lowest); color: var(--color-on-surface); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); font: inherit; }
label { font-weight: 600; } a { color: var(--color-primary-container); } .brand { font-weight: 700; font-size: 20px; text-decoration: none; }
p { line-height: 1.7; } .hint { color: var(--color-on-surface-variant); } .error { color: var(--color-status-critical); }
button { padding: 12px; margin-top: 8px; border: 0; border-radius: var(--radius-md); background: var(--color-primary-container); color: var(--color-on-primary); font: inherit; font-weight: 600; cursor: pointer; }
button:disabled { opacity: .5; cursor: not-allowed; }
</style>
