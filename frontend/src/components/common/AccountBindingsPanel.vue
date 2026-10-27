<script setup lang="ts">
import { computed, onMounted, ref, useId, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import EmailCodeField from './EmailCodeField.vue'
import ChangePasswordForm from './ChangePasswordForm.vue'
import { accountError, getBindings, sendBindingCode, prepareEmailBinding, confirmEmailBinding, authorizeGithubBinding,
  getPendingGithub, confirmGithubBinding, unlinkGithub, cancelBinding, type AccountBindings, type BindingPreview } from '@/api/account'
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const bindings = ref<AccountBindings | null>(null)
const pending = ref<BindingPreview | null>(null)
const githubDialog = ref<HTMLDialogElement | null>(null)
const githubDialogId = useId()
const unlinking = ref(false)
watch(() => Boolean(pending.value) || unlinking.value, (value) => {
  const dialog = githubDialog.value
  if (!dialog) return
  if (value && !dialog.open) dialog.showModal()
  else if (!value && dialog.open) dialog.close()
}, { flush: 'post' })
const email = ref('')
const code = ref('')
const currentCode = ref('')
const editing = ref(false)
const busy = ref(false)
const error = ref('')
const loaded = ref(false)
const replacing = computed(() => Boolean(bindings.value?.email) && email.value.trim().toLowerCase() !== bindings.value?.email?.toLowerCase())
async function load() {
  bindings.value = await getBindings()
  email.value = bindings.value.email ?? ''
  loaded.value = true
}
onMounted(async () => {
  try {
    await load()
    if (route.query.github === 'pending') pending.value = await getPendingGithub()
    else if (route.query.github === 'failed') error.value = 'GitHub 授权未完成或已过期，请重新绑定'
  } catch (e) { error.value = accountError(e, '加载绑定信息失败，请刷新重试') }
})
async function bindEmail() {
  busy.value = true; error.value = ''
  try {
    const preview = await prepareEmailBinding(email.value.trim(), code.value, currentCode.value)
    if (preview.mergeRequired) {
      try {
        await ElMessageBox.confirm(`该邮箱已有账号「${preview.accountName}」。是否绑定并合并？将保留邮箱账号的用户名、密码和资料，将当前账号的训练、讨论等数据迁入，今后使用同一账号登录。`, '确认绑定已有账号', { confirmButtonText: '确认绑定并合并', cancelButtonText: '取消', type: 'warning', closeOnClickModal: false })
      } catch { await cancelBinding(); code.value = ''; currentCode.value = ''; return }
    }
    auth.user = await confirmEmailBinding()
    await load(); editing.value = false; code.value = ''; currentCode.value = ''
    ElMessage.success('邮箱绑定成功')
  } catch (e) { error.value = accountError(e) }
  finally { busy.value = false }
}
async function startGithub() {
  busy.value = true; error.value = ''
  try { window.location.assign((await authorizeGithubBinding()).url) }
  catch (e) { error.value = accountError(e); busy.value = false }
}
async function finishGithub() {
  if (busy.value) return
  busy.value = true; error.value = ''
  try {
    auth.user = await confirmGithubBinding()
    pending.value = null; await load()
    await router.replace({ path: route.path })
    ElMessage.success('GitHub 绑定成功')
  } catch (e) { error.value = accountError(e) }
  finally { busy.value = false }
}
async function cancelGithub() {
  if (busy.value) return
  error.value = ''
  busy.value = true
  try {
    if (unlinking.value) { unlinking.value = false; return }
    await cancelBinding(); pending.value = null; await router.replace({ path: route.path })
  }
  catch (e) { error.value = accountError(e) }
  finally { busy.value = false }
}
function showUnlinkGithub() {
  error.value = ''; unlinking.value = true
}
async function finishUnlinkGithub() {
  if (busy.value) return
  busy.value = true; error.value = ''
  try {
    auth.user = await unlinkGithub()
    unlinking.value = false
    await load()
    ElMessage.success('GitHub 已解绑')
  } catch (e) { error.value = accountError(e, '解绑失败，请稍后重试') }
  finally { busy.value = false }
}
</script>
<template>
  <section class="bindings-panel">
    <h2>账号绑定与安全</h2>
    <p v-if="!loaded && !error" role="status">正在加载绑定信息…</p>
    <template v-if="bindings">
      <div class="binding-row">
        <div class="binding-label"><strong>邮箱</strong><span>{{ bindings.email || '未绑定邮箱' }}</span></div>
        <button type="button" :disabled="busy || !!pending" @click="editing = !editing">{{ editing ? '收起' : bindings.email ? '换绑邮箱' : '绑定邮箱' }}</button>
      </div>
      <form v-if="editing" class="binding-form" @submit.prevent="bindEmail">
        <label for="binding-email">{{ bindings.email ? '新邮箱' : '邮箱地址' }}</label>
        <input id="binding-email" v-model="email" type="email" autocomplete="email" maxlength="128" required :disabled="busy" placeholder="请输入邮箱地址" />
        <EmailCodeField v-if="replacing" v-model="currentCode" :email="bindings.email!" :send="() => sendBindingCode(bindings!.email!, true)" label="原邮箱验证码" :disabled="busy" />
        <EmailCodeField v-model="code" :email="email" :send="() => sendBindingCode(email.trim())" :disabled="busy" :label="bindings.email ? '新邮箱验证码' : '邮箱验证码'" />
        <p class="hint">换绑邮箱需要验证原邮箱与新邮箱。邮箱已有独立账号时，会先询问是否合并。</p>
        <button class="primary" type="submit" :disabled="busy">{{ busy ? '处理中…' : '验证并绑定' }}</button>
      </form>
      <div class="binding-row">
        <div class="binding-label">
          <strong>GitHub</strong>
          <span>{{ bindings.githubBound ? '已绑定 GitHub' : '未绑定 GitHub' }}</span>
          <a v-if="bindings.githubLogin" :href="`https://github.com/${encodeURIComponent(bindings.githubLogin)}`" target="_blank" rel="noopener noreferrer">{{ bindings.githubName || bindings.githubLogin }}<span v-if="bindings.githubName"> · @{{ bindings.githubLogin }}</span></a>
        </div>
        <div class="github-row-actions">
          <button v-if="!bindings.githubLogin" type="button" :disabled="busy || !!pending || unlinking" @click="startGithub">{{ bindings.githubBound ? '同步 GitHub 名称' : '绑定 GitHub' }}</button>
          <button v-if="bindings.githubBound" type="button" class="unlink-button" :disabled="busy || !!pending || unlinking" @click="showUnlinkGithub">解绑 GitHub</button>
        </div>
      </div>
      <ChangePasswordForm />
    </template>
    <dialog ref="githubDialog" class="github-dialog" aria-modal="true" :aria-labelledby="`${githubDialogId}-title`" :aria-describedby="`${githubDialogId}-description`" @cancel.prevent="cancelGithub">
      <template v-if="unlinking">
        <h3 :id="`${githubDialogId}-title`">确认解绑 GitHub？</h3>
        <div class="github-identity">
          <span class="github-identity-label">当前绑定的 GitHub</span>
          <strong>{{ bindings?.githubName || bindings?.githubLogin || 'GitHub 账号' }}</strong>
          <span v-if="bindings?.githubName && bindings?.githubLogin" class="hint">@{{ bindings.githubLogin }}</span>
        </div>
        <p :id="`${githubDialogId}-description`">解绑后，该 GitHub 将不能直接登录此账号。账号资料和训练、讨论等数据保留，其他登录会话将失效。</p>
        <p v-if="bindings?.email" class="hint">你仍可通过邮箱 {{ bindings.email }} 登录。</p>
        <p v-else class="error">请先绑定邮箱，以便解绑后继续登录。</p>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <div class="github-dialog-actions">
          <button type="button" autofocus :disabled="busy" @click="cancelGithub">取消</button>
          <button type="button" class="primary" :disabled="busy || !bindings?.email" @click="finishUnlinkGithub">{{ busy ? '解绑中…' : '确认解绑' }}</button>
        </div>
      </template>
      <template v-if="pending">
        <h3 :id="`${githubDialogId}-title`">{{ pending.mergeRequired ? '确认绑定并合并账号' : '确认绑定 GitHub' }}</h3>
        <div class="github-identity">
          <span class="github-identity-label">GitHub 账号</span>
          <strong>{{ pending.githubName || pending.githubLogin }}</strong>
          <span v-if="pending.githubName" class="hint">@{{ pending.githubLogin }}</span>
        </div>
        <p v-if="pending.mergeRequired" :id="`${githubDialogId}-description`">该 GitHub 已有独立账号「{{ pending.accountName }}」。确认后合并训练、讨论等数据，保留邮箱账号的用户名、密码和资料，此后邮箱和 GitHub 均登录同一账号。</p>
        <p v-else :id="`${githubDialogId}-description`">绑定后，可使用此 GitHub 登录当前账号。</p>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
        <div class="github-dialog-actions">
          <button type="button" autofocus :disabled="busy" @click="cancelGithub">取消</button>
          <button type="button" class="primary" :disabled="busy" @click="finishGithub">{{ busy ? '处理中…' : pending.mergeRequired ? '确认绑定并合并' : '确认绑定' }}</button>
        </div>
      </template>
    </dialog>
    <p v-if="error && !pending && !unlinking" class="error" role="alert">{{ error }}</p>
  </section>
</template>
<style scoped>
.bindings-panel { margin-top: 24px; max-width: 640px; padding: 28px 32px; background: var(--color-surface-card); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); box-shadow: var(--shadow-card); }
h2 { margin: 0 0 12px; font-size: var(--text-headline-sm); color: var(--color-on-surface); }
.binding-row,.binding-label,.github-dialog-actions { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
.binding-row { justify-content: space-between; padding: 20px 0; border-bottom: 1px solid var(--color-border-subtle); }
.binding-row:last-of-type { border-bottom: 0; }
.binding-label { min-width: 0; }
.binding-label span,.hint { color: var(--color-on-surface-variant); }
a { color: var(--color-primary-container); overflow-wrap: anywhere; }
button { padding: 9px 14px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); background: var(--color-surface-container-lowest); color: var(--color-on-surface); font: inherit; cursor: pointer; }
button:disabled { cursor: not-allowed; opacity: .5; }
button.primary { background: var(--color-primary-container); color: var(--color-on-primary); border-color: transparent; }
.binding-form { display: grid; gap: 14px; padding: 20px 0; }
.binding-form > input { width: 100%; padding: 11px 14px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); font: inherit; background: var(--color-surface-container-lowest); color: var(--color-on-surface); }
.binding-form > button { justify-self: start; }
.github-dialog { width: min(480px, calc(100vw - 32px)); max-height: calc(100dvh - 48px); margin: auto; padding: 28px; overflow-y: auto; background: var(--color-surface-card); color: var(--color-on-surface); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); box-shadow: 0 24px 80px rgb(0 0 0 / 22%); }
.github-dialog::backdrop { background: rgb(0 0 0 / 45%); backdrop-filter: blur(3px); }
.github-dialog h3 { margin: 0 0 20px; font-size: var(--text-headline-sm); }
.github-identity { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 12px; padding: 16px; border-radius: var(--radius-md); background: var(--color-surface-container-low); overflow-wrap: anywhere; }
.github-identity-label { flex-basis: 100%; font-size: var(--text-body-sm); color: var(--color-on-surface-variant); }
.github-dialog-actions { justify-content: flex-end; margin-top: 24px; }
.github-row-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.unlink-button { color: var(--color-status-critical); }
p { font-size: var(--text-body-md); line-height: 1.7; }
.error { color: var(--color-status-critical); }
@media (max-width: 600px) { .bindings-panel { padding: 22px 18px; } }
</style>
