<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getLoginMethods, sendEmailCode, loginWithEmail, authorizeGithub } from '@/api/auth'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { actionLabels } from '@/constants/labels'
import { siteBrand } from '@/constants/site'
import { ElMessage } from 'element-plus'
import AnimatedLoginCharacters from '@/components/ui/AnimatedLoginCharacters.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const showPassword = ref(false)
const isUsernameFocused = ref(false)
const submitting = ref(false)
const errorMsg = ref('')
const mode = ref<'password' | 'email'>('password')
const email = ref('')
const code = ref('')
const methods = ref({ email: false, github: false })
const methodsLoaded = ref(false)
const sending = ref(false)
const countdown = ref(0)
const notice = ref('')
let timer: ReturnType<typeof setInterval> | undefined

onMounted(async () => {
  if (route.query.github === 'failed') errorMsg.value = 'GitHub 授权未完成或已过期，请重新登录'
  try { methods.value = await getLoginMethods(); methodsLoaded.value = true }
  catch { errorMsg.value = '无法获取登录方式，请稍后刷新重试' }
})
onUnmounted(() => clearInterval(timer))

function changeMode(value: 'password' | 'email') {
  mode.value = value; errorMsg.value = ''; notice.value = ''; code.value = ''; password.value = ''
  showPassword.value = false
  isUsernameFocused.value = false
}

function message(error: unknown, fallback: string): string {
  const err = error as { response?: { data?: { message?: string } } }
  return err.response?.data?.message || fallback
}

async function sendCode() {
  errorMsg.value = ''; notice.value = ''
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    errorMsg.value = '请输入有效的邮箱地址'; return
  }
  sending.value = true
  try {
    notice.value = (await sendEmailCode(email.value.trim())).message
    countdown.value = 60
    clearInterval(timer)
    timer = setInterval(() => { if (--countdown.value <= 0) clearInterval(timer) }, 1000)
  } catch (e) { errorMsg.value = message(e, '验证码发送失败，请稍后重试') }
  finally { sending.value = false }
}

async function githubLogin() {
  errorMsg.value = ''; submitting.value = true
  try { window.location.assign((await authorizeGithub()).url) }
  catch (e) { errorMsg.value = message(e, 'GitHub 登录暂时不可用'); submitting.value = false }
}

async function handleLogin() {
  errorMsg.value = ''
  if (mode.value === 'email') {
    if (!email.value.trim() || !/^\d{6}$/.test(code.value)) {
      errorMsg.value = '请输入邮箱地址和六位验证码'; return
    }
  } else if (!username.value.trim() || !password.value) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  submitting.value = true
  try {
    if (mode.value === 'email') auth.user = await loginWithEmail(email.value.trim(), code.value)
    else await auth.login(username.value.trim(), password.value)
    ElMessage.success('登录成功')
    const target = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    const redirect = target.startsWith('/') && !target.startsWith('//') ? target : '/'
    router.push(redirect)
  } catch (e: unknown) {
    const err = e as { response?: { status: number } }
    if (err.response?.status === 401) {
      errorMsg.value = mode.value === 'email' ? '验证码错误、已过期或尝试次数过多，请重新获取' : '用户名或密码错误'
    } else {
      errorMsg.value = message(e, '登录失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <AnimatedLoginCharacters
      class="auth-illustration"
      :is-username-focused="isUsernameFocused"
      :is-password-hidden="Boolean(password) && !showPassword"
      :is-password-revealed="showPassword && Boolean(password)"
    />
    <main class="login-content">
    <div class="auth-card">
      <div class="auth-header">
        <RouterLink to="/" class="brand">
          <span class="brand-icon">&lt;/&gt;</span>
          <span class="brand-text">{{ siteBrand.clubName }}</span>
        </RouterLink>
        <p class="auth-subtitle"> · 算法训练与交流</p>
      </div>
      <div class="login-tabs" aria-label="登录方式">
        <button type="button" :class="{ active: mode === 'password' }" :aria-pressed="mode === 'password'" :disabled="submitting || sending" @click="changeMode('password')">账号密码</button>
        <button type="button" :class="{ active: mode === 'email' }" :aria-pressed="mode === 'email'" :disabled="submitting || sending" @click="changeMode('email')">邮箱验证码</button>
      </div>
      <form class="auth-form" @submit.prevent="handleLogin">
        <p v-if="mode === 'email'" class="login-hint">       首次使用该邮箱会自动注册并登录。</p>
        <p v-if="mode === 'email' && methodsLoaded && !methods.email" class="login-hint" role="status">邮箱登录尚未配置，请先使用账号密码登录。</p>
        <div v-if="mode === 'email'" class="form-field">
          <label for="login-email">邮箱</label>
          <input id="login-email" v-model="email" type="email" autocomplete="email" placeholder="请输入邮箱地址" maxlength="128" :disabled="submitting || sending" required />
        </div>
        <div v-if="mode === 'email'" class="form-field">
          <label for="login-code">验证码</label>
          <div class="code-row">
            <input id="login-code" v-model="code" type="text" inputmode="numeric" autocomplete="one-time-code" placeholder="六位验证码" maxlength="6" pattern="[0-9]{6}" :disabled="submitting" required />
            <button type="button" class="code-button" :disabled="!methods.email || sending || submitting || countdown > 0" @click="sendCode">{{ sending ? '发送中…' : countdown > 0 ? `${countdown} 秒后重发` : '获取验证码' }}</button>
          </div>
        </div>
        <div v-if="mode === 'password'" class="form-field">
          <label for="login-username">用户名</label>
          <input
            id="login-username"
            v-model="username"
            type="text"
            autocomplete="username"
            placeholder="请输入用户名"
            :disabled="submitting"
            @focus="isUsernameFocused = true"
            @blur="isUsernameFocused = false"
          />
        </div>
        <div v-if="mode === 'password'" class="form-field">
          <label for="login-password">密码</label>
          <input
            id="login-password"
            v-model="password"
            :type="showPassword ? 'text' : 'password'"
            autocomplete="current-password"
            placeholder="请输入密码"
            :disabled="submitting"
          />
          <button
            class="password-toggle"
            type="button"
            :aria-label="showPassword ? '隐藏密码' : '显示密码'"
            :aria-pressed="showPassword"
            @mousedown.prevent
            @click="showPassword = !showPassword"
          >
            <span aria-hidden="true">{{ showPassword ? '隐藏' : '显示' }}</span>
          </button>
        </div>
        <p v-if="notice" class="login-hint" role="status">{{ notice }}</p>
        <p v-if="errorMsg" class="form-error" role="alert">{{ errorMsg }}</p>
        <button type="submit" class="btn-primary" :disabled="submitting || sending || (mode === 'email' && !methods.email)">
          {{ submitting ? '登录中...' : actionLabels.login }}
        </button>
      </form>
      <p class="auth-footer"><RouterLink to="/forgot-password" class="auth-link">忘记密码？</RouterLink></p>
      <div class="other-login">
        <span class="login-hint">其他登录方式</span>
        <button type="button" class="github-button" :disabled="!methods.github || submitting || sending" @click="githubLogin">
          <svg class="github-icon" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M12 .297C5.37.297 0 5.67 0 12.297c0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.043-1.61-4.043-1.61-.546-1.387-1.333-1.756-1.333-1.756-1.09-.745.083-.73.083-.73 1.205.084 1.838 1.237 1.838 1.237 1.07 1.835 2.807 1.305 3.492.998.108-.776.42-1.305.763-1.605-2.665-.305-5.467-1.334-5.467-5.93 0-1.31.467-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23a11.5 11.5 0 0 1 3-.404c1.02.005 2.045.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.922.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12" />
          </svg>
          <span>使用 GitHub 登录</span>
        </button>
        <p v-if="methodsLoaded && !methods.github" class="login-hint">GitHub 登录暂未启用</p>
      </div>
      <p class="auth-footer">
        还没有账号？
        <RouterLink to="/register" class="auth-link">{{ actionLabels.register }}</RouterLink>
      </p>
    </div>
    </main>
  </div>
</template>

<style scoped>
.login-tabs { display: flex; gap: 8px; margin-bottom: 24px; border-bottom: 1px solid var(--color-border-subtle); }
.login-tabs button { flex: 1; padding: 12px 0; border: 0; border-bottom: 2px solid transparent; background: transparent; color: var(--color-on-surface-variant); cursor: pointer; font: inherit; }
.login-tabs button.active { color: var(--color-primary); border-bottom-color: var(--color-primary); font-weight: 600; }
.login-hint { color: var(--color-on-surface-variant); font-size: var(--text-body-md); line-height: 1.6; margin: 0; }
.code-row { display: flex; gap: 8px; min-width: 0; }
.code-row input { flex: 1; min-width: 0; width: 0; }
.code-button, .github-button { border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); padding: 0 12px; background: var(--color-surface-container-lowest); color: var(--color-on-surface); font: inherit; cursor: pointer; }
.other-login { margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--color-border-subtle); display: grid; gap: 10px; text-align: center; }
.github-button { min-height: 44px; color: #24292f; display: flex; align-items: center; justify-content: center; gap: 10px; }
.github-icon { width: 22px; height: 22px; flex-shrink: 0; }
.github-button:hover:not(:disabled) { background: var(--color-surface-container-low); border-color: #9aa0a6; }
button:disabled { cursor: not-allowed; opacity: 0.6; }
button:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 3px; }
.auth-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(430px, 1fr) minmax(460px, 1.06fr);
  background: var(--color-surface-background);
}

.login-content {
  display: flex;
  min-width: 0;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  padding: clamp(30px, 5vw, 76px);
  background: var(--color-surface-background);
}

.auth-card {
  width: 100%;
  max-width: 440px;
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: 0 20px 70px rgba(34, 48, 65, .07);
  padding: 42px 36px;
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-headline);
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
}

.brand-icon {
  font-family: var(--font-mono);
  font-size: 28px;
}

.auth-subtitle {
  margin-top: 8px;
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-field label {
  font-size: var(--text-body-md);
  font-weight: 500;
  color: var(--color-on-surface);
}

.form-field input {
  height: 42px;
  padding: 0 12px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
  outline: none;
  transition: border-color 0.2s;
}

.form-field:has(.password-toggle) { position: relative; }
.form-field:has(.password-toggle) input { padding-right: 66px; }
.password-toggle {
  position: absolute;
  right: 10px;
  bottom: 8px;
  display: grid;
  min-width: 46px;
  min-height: 28px;
  place-items: center;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: var(--color-on-surface-variant);
  font-size: 12px;
  cursor: pointer;
}
.password-toggle:hover { color: var(--color-primary); background: var(--color-surface-container-low); }

.form-field input:focus {
  border-color: var(--color-primary-container);
}

.form-field input:disabled {
  opacity: 0.6;
}

.form-error {
  color: var(--color-error);
  font-size: var(--text-body-md);
  text-align: center;
}

.btn-primary {
  height: 42px;
  background: var(--color-primary-container);
  color: var(--color-on-primary);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
  margin-top: 4px;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.auth-footer {
  text-align: center;
  margin-top: 24px;
  font-size: var(--text-body-md);
  color: var(--color-on-surface-variant);
}

.auth-link {
  color: var(--color-primary-container);
  font-weight: 500;
}

.auth-link:hover {
  text-decoration: underline;
}

@media (max-width: 900px) {
  .auth-page { display: block; padding: 28px 20px; }
  .auth-illustration { display: none; }
  .login-content { min-height: calc(100vh - 56px); padding: 0; background: transparent; }
  .auth-card { max-width: 460px; padding: 38px clamp(24px, 6vw, 40px); }
}

@media (max-width: 480px) {
  .auth-page { padding: 12px; }
  .login-content { min-height: calc(100vh - 24px); }
  .auth-card { padding: 30px 22px; }
  .auth-header { margin-bottom: 24px; }
  .brand { font-size: 29px; }
}
</style>
