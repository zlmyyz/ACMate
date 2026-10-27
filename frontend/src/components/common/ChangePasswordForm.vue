<script setup lang="ts">
import { ref, useId } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { accountError, changePassword } from '@/api/account'
const auth = useAuthStore()
const router = useRouter()
const currentPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const busy = ref(false)
const error = ref('')
const dialog = ref<HTMLDialogElement | null>(null)
const dialogId = useId()
function clearForm() {
  currentPassword.value = ''; newPassword.value = ''; confirmPassword.value = ''; error.value = ''
}
function open() {
  clearForm()
  if (!dialog.value?.open) dialog.value?.showModal()
}
function close() {
  if (busy.value) return
  dialog.value?.close()
  clearForm()
}
function recoverPassword() {
  if (busy.value) return
  close()
  router.push('/forgot-password')
}
async function submit() {
  if (busy.value) return
  error.value = ''
  if (!currentPassword.value) { error.value = '请输入原密码'; return }
  if (newPassword.value.length < 8 || newPassword.value.length > 64) { error.value = '新密码长度为 8–64 位'; return }
  if (newPassword.value !== confirmPassword.value) { error.value = '两次输入的新密码不一致'; return }
  if (currentPassword.value === newPassword.value) { error.value = '新密码不能与原密码相同'; return }
  busy.value = true
  try {
    const result = await changePassword(currentPassword.value, newPassword.value, confirmPassword.value)
    currentPassword.value = ''; newPassword.value = ''; confirmPassword.value = ''
    dialog.value?.close()
    auth.user = null
    ElMessage.success(result.message)
    await router.push('/login')
  } catch (e) { error.value = accountError(e, '修改密码失败，请稍后重试') }
  finally { busy.value = false }
}
</script>
<template>
  <section class="password-section">
    <div class="password-heading">
      <div><h3>登录密码</h3><p class="password-hint">使用原密码修改，或通过邮箱找回。</p></div>
      <div class="password-actions"><RouterLink to="/forgot-password">忘记密码？</RouterLink><button type="button" class="open-password-button" @click="open">修改密码</button></div>
    </div>
    <dialog ref="dialog" class="password-dialog" aria-modal="true" :aria-labelledby="`${dialogId}-title`" :aria-describedby="`${dialogId}-description`" @cancel.prevent="close">
    <h3 :id="`${dialogId}-title`">修改密码</h3>
    <p :id="`${dialogId}-description`" class="password-hint">请输入原密码和新密码。</p>
    <form class="password-form" @submit.prevent="submit">
      <label for="profile-current-password">原密码</label>
      <input id="profile-current-password" v-model="currentPassword" type="password" autocomplete="current-password" autofocus required maxlength="64" :disabled="busy" placeholder="请输入当前密码" />
      <label for="profile-new-password">新密码</label>
      <input id="profile-new-password" v-model="newPassword" type="password" autocomplete="new-password" required minlength="8" maxlength="64" :disabled="busy" placeholder="请输入 8–64 位新密码" />
      <label for="profile-confirm-password">确认新密码</label>
      <input id="profile-confirm-password" v-model="confirmPassword" type="password" autocomplete="new-password" required minlength="8" maxlength="64" :disabled="busy" placeholder="请再次输入新密码" />
      <p v-if="error" class="password-error" role="alert">{{ error }}</p>
      <p class="password-hint">修改成功后，需要使用新密码重新登录。</p>
      <div class="dialog-footer">
        <button type="button" class="recovery-link" :disabled="busy" @click="recoverPassword">忘记密码？</button>
        <div class="password-actions"><button type="button" class="cancel-button" :disabled="busy" @click="close">取消</button><button type="submit" :disabled="busy">{{ busy ? '修改中…' : '确认修改' }}</button></div>
      </div>
    </form>
    </dialog>
  </section>
</template>
<style scoped>
.password-section { padding-top: 24px; }
.password-heading { display: flex; flex-wrap: wrap; justify-content: space-between; align-items: center; gap: 12px; }
.password-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 12px; }
.password-dialog { width: min(460px, calc(100vw - 32px)); max-height: calc(100dvh - 48px); overflow-y: auto; margin: auto; padding: 28px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); background: var(--color-surface-card); color: var(--color-on-surface); box-shadow: 0 24px 80px rgb(0 0 0 / 22%); }
.password-dialog::backdrop { background: rgb(0 0 0 / 45%); backdrop-filter: blur(3px); }
.dialog-footer { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 12px; margin-top: 12px; }
h3 { margin: 0; font-size: var(--text-body-lg); color: var(--color-on-surface); }
a { color: var(--color-primary-container); }
.password-form { display: grid; gap: 10px; margin-top: 18px; }
label { font-size: var(--text-body-md); font-weight: 600; color: var(--color-on-surface); }
input { width: 100%; padding: 11px 14px; background: var(--color-surface-container-lowest); color: var(--color-on-surface); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); font: inherit; }
input:focus-visible { outline: 2px solid var(--color-primary-container); outline-offset: 1px; }
p { margin: 10px 0 0; font-size: var(--text-body-sm); line-height: 1.7; }
.password-hint { color: var(--color-on-surface-variant); }
.password-error { color: var(--color-status-critical); }
button { justify-self: start; margin-top: 6px; padding: 10px 18px; border: 0; border-radius: var(--radius-md); background: var(--color-primary-container); color: var(--color-on-primary); font: inherit; font-weight: 600; cursor: pointer; }
button:disabled { opacity: .5; cursor: not-allowed; }
.cancel-button { background: var(--color-surface-container-low); color: var(--color-on-surface); border: 1px solid var(--color-border-subtle); }
.recovery-link { background: transparent; color: var(--color-primary-container); padding: 0; font-weight: 400; }
</style>
