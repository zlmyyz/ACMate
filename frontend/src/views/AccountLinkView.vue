<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getGithubLoginLink, confirmGithubLoginLink, skipGithubLoginLink, sendGithubLinkCode, accountError } from '@/api/account'
import EmailCodeField from '@/components/common/EmailCodeField.vue'
const router = useRouter()
const auth = useAuthStore()
const info = ref<{ githubLogin: string; email: string } | null>(null)
const code = ref('')
const busy = ref(false)
const error = ref('')
onMounted(async () => { try { info.value = await getGithubLoginLink() } catch (e) { error.value = accountError(e) } })
async function finish(bind: boolean) {
  busy.value = true; error.value = ''
  try { auth.user = bind ? await confirmGithubLoginLink(code.value) : await skipGithubLoginLink(); await router.replace('/') }
  catch (e) { error.value = accountError(e) }
  finally { busy.value = false }
}
</script>
<template>
  <main class="link-page"><section class="link-card">
    <h1>是否绑定已有邮箱账号？</h1>
    <template v-if="info">
      <p>GitHub <strong>@{{ info.githubLogin }}</strong> 的已验证邮箱 <strong>{{ info.email }}</strong> 对应一个已有账号。</p>
      <p>绑定后将保留邮箱账号的用户名、密码和资料，合并两个账号的训练、讨论等数据，邮箱与 GitHub 均可登录同一账号。</p>
      <form @submit.prevent="finish(true)">
        <EmailCodeField v-model="code" :email="info.email" :send="sendGithubLinkCode" :disabled="busy" />
        <button class="primary" :disabled="busy">{{ busy ? '处理中…' : '验证邮箱并确认绑定' }}</button>
      </form>
      <button :disabled="busy" @click="finish(false)">暂不绑定，继续 GitHub 登录</button>
    </template>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <RouterLink to="/login">返回登录</RouterLink>
  </section></main>
</template>
<style scoped>
.link-page { min-height: 100vh; display: grid; place-items: center; padding: 24px; background: var(--color-surface-background); }
.link-card { width: 100%; max-width: 520px; padding: 32px; background: var(--color-surface-card); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); box-shadow: var(--shadow-card); }
h1 { font-size: var(--text-headline-sm); margin: 0 0 18px; } p { line-height: 1.8; color: var(--color-on-surface-variant); } form { display: grid; gap: 14px; margin: 20px 0 12px; }
button { width: 100%; padding: 12px; margin-bottom: 12px; background: var(--color-surface-container-lowest); color: var(--color-on-surface); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-md); font: inherit; cursor: pointer; }
button.primary { background: var(--color-primary-container); color: var(--color-on-primary); border-color: transparent; } button:disabled { opacity: .5; cursor: not-allowed; }
.error { color: var(--color-status-critical); } a { color: var(--color-primary-container); }
</style>
