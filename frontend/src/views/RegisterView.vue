<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { actionLabels } from '@/constants/labels'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()
const router = useRouter()

const username = ref('')
const nickname = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const submitting = ref(false)
const errorMsg = ref('')

async function handleRegister() {
  errorMsg.value = ''
  if (!username.value.trim() || !nickname.value.trim() || !password.value) {
    errorMsg.value = '请填写必填字段'
    return
  }
  if (password.value !== confirmPassword.value) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }
  submitting.value = true
  try {
    await auth.register(
      username.value.trim(),
      password.value,
      nickname.value.trim(),
      email.value.trim() || undefined,
    )
    ElMessage.success('注册成功，请登录')
    router.push({ name: 'login' })
  } catch (e: unknown) {
    const err = e as { response?: { status: number; data?: { message?: string } } }
    const status = err.response?.status
    if (status === 409) {
      errorMsg.value = err.response?.data?.message || '用户名或邮箱已被使用'
    } else if (status === 400) {
      errorMsg.value = err.response?.data?.message || '请求参数有误'
    } else {
      errorMsg.value = '注册失败，请稍后重试'
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <RouterLink to="/" class="brand">
          <span class="brand-icon">&lt;/&gt;</span>
          <span class="brand-text">ACMate</span>
        </RouterLink>
        <p class="auth-subtitle">创建你的账号</p>
      </div>
      <form class="auth-form" @submit.prevent="handleRegister">
        <div class="form-field">
          <label for="reg-username">用户名 <span class="required">*</span></label>
          <input
            id="reg-username"
            v-model="username"
            type="text"
            autocomplete="username"
            placeholder="请输入用户名"
            :disabled="submitting"
          />
        </div>
        <div class="form-field">
          <label for="reg-nickname">昵称 <span class="required">*</span></label>
          <input
            id="reg-nickname"
            v-model="nickname"
            type="text"
            placeholder="请输入昵称"
            :disabled="submitting"
          />
        </div>
        <div class="form-field">
          <label for="reg-email">邮箱</label>
          <input
            id="reg-email"
            v-model="email"
            type="email"
            autocomplete="email"
            placeholder="请输入邮箱（选填）"
            :disabled="submitting"
          />
        </div>
        <div class="form-field">
          <label for="reg-password">密码 <span class="required">*</span></label>
          <input
            id="reg-password"
            v-model="password"
            type="password"
            autocomplete="new-password"
            placeholder="请输入密码"
            :disabled="submitting"
          />
        </div>
        <div class="form-field">
          <label for="reg-confirm-password">确认密码 <span class="required">*</span></label>
          <input
            id="reg-confirm-password"
            v-model="confirmPassword"
            type="password"
            autocomplete="new-password"
            placeholder="请再次输入密码"
            :disabled="submitting"
          />
        </div>
        <p v-if="errorMsg" class="form-error">{{ errorMsg }}</p>
        <button type="submit" class="btn-primary" :disabled="submitting">
          {{ submitting ? '注册中...' : actionLabels.register }}
        </button>
      </form>
      <p class="auth-footer">
        已有账号？
        <RouterLink to="/login" class="auth-link">{{ actionLabels.login }}</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-surface-background);
  padding: 24px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 40px 32px;
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
  gap: 14px;
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

.required {
  color: var(--color-error);
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
</style>
