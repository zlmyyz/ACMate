<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { updateProfile, uploadAvatar } from '@/api/users'
import type { UpdateProfileRequest } from '@/types/user'
import PageContainer from '@/components/layout/PageContainer.vue'

const router = useRouter()
const auth = useAuthStore()

const nickname = ref(auth.user?.nickname ?? '')
const bio = ref(auth.user?.bio ?? '')
const saving = ref(false)
const saveError = ref('')
const saveSuccess = ref(false)
const avatarUploading = ref(false)
const avatarError = ref('')

const nicknameValid = computed(() => {
  if (!nickname.value) return true
  return nickname.value.trim().length >= 2 && nickname.value.trim().length <= 32
})

function handleAvatarChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const allowed = ['image/png', 'image/jpeg', 'image/gif', 'image/webp']
  if (!allowed.includes(file.type)) {
    avatarError.value = '仅支持 PNG、JPG、GIF、WebP 格式'
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    avatarError.value = '文件大小不能超过 2MB'
    return
  }

  avatarError.value = ''
  avatarUploading.value = true
  uploadAvatar(file)
    .then((updated) => {
      if (auth.user) {
        auth.user.avatarUrl = updated.avatarUrl
        auth.user.nickname = updated.nickname
      }
    })
    .catch((e: unknown) => {
      const err = e as { response?: { data?: { message?: string } } }
      avatarError.value = err.response?.data?.message ?? '头像上传失败'
    })
    .finally(() => {
      avatarUploading.value = false
      input.value = ''
    })
}

async function handleSave() {
  const trimmed = nickname.value.trim()
  if (trimmed && (trimmed.length < 2 || trimmed.length > 32)) {
    saveError.value = '昵称长度需在 2~32 个字符之间'
    return
  }
  saveError.value = ''
  saveSuccess.value = false
  saving.value = true

  const req: UpdateProfileRequest = {}
  if (trimmed !== (auth.user?.nickname ?? '')) {
    req.nickname = trimmed || undefined
  }
  const bioTrimmed = bio.value.trim()
  if (bioTrimmed !== (auth.user?.bio ?? '')) {
    req.bio = bioTrimmed || undefined
  }

  if (!req.nickname && req.bio === undefined) {
    saving.value = false
    return
  }

  try {
    await updateProfile(req)
    if (auth.user) {
      if (req.nickname !== undefined) auth.user.nickname = req.nickname
      if (req.bio !== undefined) auth.user.bio = req.bio || null
    }
    saveSuccess.value = true
    setTimeout(() => { saveSuccess.value = false }, 2000)
  } catch (e: unknown) {
    const err = e as { response?: { data?: { message?: string } } }
    saveError.value = err.response?.data?.message ?? '保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <PageContainer>
    <template #header>
      <div class="edit-header">
        <button class="back-link" @click="router.back()">&larr; 返回</button>
      </div>
    </template>

    <div class="edit-card">
      <h1 class="edit-title">编辑个人资料</h1>

      <div class="avatar-section">
        <div class="avatar-preview">
          <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" alt="头像" />
          <span v-else class="avatar-placeholder">
            {{ (auth.user?.nickname || auth.user?.username || '?').charAt(0).toUpperCase() }}
          </span>
        </div>
        <label class="upload-btn" :class="{ disabled: avatarUploading }">
          {{ avatarUploading ? '上传中...' : '更换头像' }}
          <input
            type="file"
            accept="image/png,image/jpeg,image/gif,image/webp"
            class="file-input"
            :disabled="avatarUploading"
            @change="handleAvatarChange"
          />
        </label>
        <p v-if="avatarError" class="field-error avatar-error">{{ avatarError }}</p>
      </div>

      <div class="form-section">
        <div class="field">
          <label class="field-label">用户名</label>
          <input class="field-input" disabled :value="auth.user?.username ?? ''" />
          <p class="field-hint">用户名不可修改</p>
        </div>

        <div class="field">
          <label class="field-label">昵称</label>
          <input
            v-model="nickname"
            class="field-input"
            :class="{ invalid: !nicknameValid }"
            maxlength="32"
            placeholder="输入昵称（2~32字符）"
          />
          <p v-if="!nicknameValid" class="field-error">昵称长度需在 2~32 个字符之间</p>
        </div>

        <div class="field">
          <label class="field-label">个人简介</label>
          <textarea
            v-model="bio"
            class="field-textarea"
            maxlength="500"
            rows="4"
            placeholder="介绍一下自己..."
          />
          <p class="field-hint">{{ bio.length }}/500</p>
        </div>
      </div>

      <div class="form-actions">
        <div class="save-status">
          <span v-if="saveSuccess" class="save-success">已保存</span>
          <span v-if="saveError" class="save-error">{{ saveError }}</span>
        </div>
        <button class="cancel-btn" @click="router.back()">取消</button>
        <button class="save-btn" :disabled="saving || !nicknameValid" @click="handleSave">
          {{ saving ? '保存中...' : '保存' }}
        </button>
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.edit-header {
  display: flex;
  align-items: center;
}

.back-link {
  background: none;
  border: none;
  color: var(--color-primary-container);
  font-size: var(--text-body-md);
  font-weight: 500;
  cursor: pointer;
  padding: 0;
}

.back-link:hover {
  text-decoration: underline;
}

.edit-card {
  background: var(--color-surface-card);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 32px;
  max-width: 640px;
}

.edit-title {
  font-family: var(--font-headline);
  font-size: var(--text-headline-md);
  font-weight: 700;
  color: var(--color-on-surface);
  margin-bottom: 28px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--color-border-subtle);
}

.avatar-preview {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--color-surface-container-low);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  font-family: var(--font-headline);
  font-size: 40px;
  font-weight: 700;
  color: var(--color-on-surface-variant);
}

.upload-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0 14px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  font-size: var(--text-body-sm);
  font-weight: 500;
  color: var(--color-on-surface-variant);
  cursor: pointer;
  transition: border-color 0.2s;
}

.upload-btn:hover:not(.disabled) {
  border-color: var(--color-primary-container);
  color: var(--color-primary-container);
}

.upload-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.file-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.avatar-error {
  margin-top: 0;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: var(--text-label-sm);
  font-weight: 600;
  color: var(--color-on-surface);
  letter-spacing: 0.05em;
}

.field-input,
.field-textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  color: var(--color-on-surface);
  background: var(--color-surface-container-lowest);
  transition: border-color 0.2s;
  font-family: inherit;
}

.field-input:focus,
.field-textarea:focus {
  outline: none;
  border-color: var(--color-primary-container);
}

.field-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.field-input.invalid {
  border-color: var(--color-status-critical);
}

.field-textarea {
  resize: vertical;
  min-height: 80px;
}

.field-hint {
  font-size: var(--text-body-sm);
  color: var(--color-on-surface-variant);
}

.field-error {
  font-size: var(--text-body-sm);
  color: var(--color-status-critical);
}

.form-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border-subtle);
}

.save-status {
  margin-right: auto;
  font-size: var(--text-body-sm);
}

.save-success {
  color: var(--color-status-success);
  font-weight: 600;
}

.save-error {
  color: var(--color-status-critical);
  font-weight: 500;
}

.cancel-btn {
  height: 36px;
  padding: 0 18px;
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
  font-weight: 500;
  cursor: pointer;
}

.cancel-btn:hover {
  border-color: var(--color-on-surface-variant);
}

.save-btn {
  height: 36px;
  padding: 0 20px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-primary-container);
  color: var(--color-on-primary);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.save-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.save-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
</style>
