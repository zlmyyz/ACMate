<script setup lang="ts">
import { computed, nextTick, ref, watch, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { UserProfile } from '@/types/user'
import { getSocialSummary, getFollowList, setFollow, type SocialSummary, type FollowPage, type SocialMember } from '@/api/profile-social'
import { accountError } from '@/api/account'
import UserAvatar from './UserAvatar.vue'
const props = defineProps<{ profile: UserProfile; nowcoderUid?: string }>()
const auth = useAuthStore()
const router = useRouter()
const self = computed(() => props.profile.id === auth.user?.id)
const summary = ref<SocialSummary | null>(null)
const error = ref('')
const busy = ref(false)
const sharing = ref(false)
const dialog = ref<HTMLDialogElement | null>(null)
const kind = ref<'following' | 'followers'>('following')
const members = ref<FollowPage | null>(null)
const listBusy = ref(false)
const listError = ref('')
const memberBusy = ref<number | null>(null)
let request = 0
let listRequest = 0
const gender = computed(() => ({ MALE: '男', FEMALE: '女', PRIVATE: '性别保密' } as Record<string, string>)[props.profile.gender ?? ''] ?? '未填写性别')
const achievements = computed(() => {
  const data = summary.value?.contributions
  return [
    { name: '阅读总数', value: data?.views, yesterday: data?.yesterdayViews, icon: 'eye', color: 'blue' },
    { name: '获得点赞', value: data?.likes, yesterday: data?.yesterdayLikes, icon: 'like', color: 'teal' },
    { name: '获得收藏', value: data?.favorites, yesterday: data?.yesterdayFavorites, icon: 'star', color: 'amber' },
  ]
})
async function loadSummary() {
  const token = ++request
  error.value = ''
  try { const data = await getSocialSummary(props.profile.id); if (token === request) summary.value = data }
  catch (e) { if (token === request) error.value = accountError(e, '个人数据加载失败') }
}
watch(() => props.profile.id, () => { summary.value = null; dialog.value?.close(); void loadSummary() }, { immediate: true })
onUnmounted(() => { request++; listRequest++ })
async function toggleFollow() {
  if (!auth.isLoggedIn) { await router.push({ path: '/login', query: { redirect: `/users/${props.profile.id}` } }); return }
  if (busy.value || !summary.value) return
  busy.value = true; error.value = ''
  try { await setFollow(props.profile.id, !summary.value.following); await loadSummary() }
  catch (e) { error.value = accountError(e, '关注操作失败') }
  finally { busy.value = false }
}
async function share() {
  sharing.value = true
  try { await navigator.clipboard.writeText(new URL(`/users/${props.profile.id}`, window.location.origin).href); ElMessage.success('个人主页链接已复制') }
  catch { ElMessage.error('复制失败，请复制浏览器地址栏中的链接') }
  finally { sharing.value = false }
}
async function loadMembers(page = 1) {
  const token = ++listRequest
  listBusy.value = true; listError.value = ''
  try { const data = await getFollowList(props.profile.id, kind.value, page); if (token === listRequest) members.value = data }
  catch (e) { if (token === listRequest) listError.value = accountError(e, '列表加载失败') }
  finally { if (token === listRequest) listBusy.value = false }
}
async function openMembers(value: 'following' | 'followers') {
  kind.value = value; members.value = null
  await nextTick()
  if (!dialog.value?.open) dialog.value?.showModal()
  await loadMembers()
}
function closeMembers() { dialog.value?.close(); listRequest++; listBusy.value = false }
async function toggleMember(member: SocialMember) {
  if (memberBusy.value) return
  memberBusy.value = member.id; listError.value = ''
  try { await setFollow(member.id, !member.following); await Promise.all([loadMembers(members.value?.page ?? 1), loadSummary()]) }
  catch (e) { listError.value = accountError(e, '关注操作失败') }
  finally { memberBusy.value = null }
}
</script>
<template>
  <aside class="profile-sidebar" aria-label="个人资料">
    <div class="identity-row">
      <div class="profile-avatar"><UserAvatar :src="profile.avatarUrl" :name="profile.nickname" /></div>
      <div class="identity-text"><h1>{{ profile.nickname || profile.username }}</h1><p>{{ profile.username }}</p><span v-if="profile.admin" class="admin-label">管理员</span></div>
    </div>
    <p v-if="profile.bio" class="profile-bio">{{ profile.bio }}</p>
    <div class="follow-counts">
      <button @click="openMembers('following')"><strong>{{ summary?.followingCount ?? '—' }}</strong> 关注</button><span aria-hidden="true">|</span>
      <button @click="openMembers('followers')"><strong>{{ summary?.followerCount ?? '—' }}</strong> 关注者</button>
    </div>
    <div class="profile-actions">
      <RouterLink v-if="self" class="edit-profile" to="/settings/profile">编辑个人资料</RouterLink>
      <button v-else class="edit-profile" :disabled="busy || !summary || profile.accountStatus === 'DISABLED'" @click="toggleFollow">{{ busy ? '处理中…' : summary?.following ? '已关注 · 取消关注' : '+ 关注' }}</button>
      <button class="share-profile" :disabled="sharing" title="复制个人主页链接" aria-label="复制个人主页链接" @click="share"><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true"><path d="M12 16V3m-5 5 5-5 5 5M5 13v7h14v-7" stroke-linecap="round" stroke-linejoin="round" /></svg></button>
    </div>
    <ul class="profile-details">
      <li><span class="ip-symbol" aria-hidden="true">IP</span><span>{{ summary?.ipRegion || 'IP 属地暂未记录' }}</span></li>
      <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z" /><circle cx="12" cy="10" r="2.5" /></svg><span>{{ profile.location || '未填写所在地' }}</span></li>
      <li><svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><circle cx="12" cy="7" r="3.5" /><path d="M12 10.5V21m-4-4h8" /></svg><span>{{ gender }}</span></li>
      <li v-if="profile.githubLogin"><span class="platform-symbol" aria-hidden="true">GH</span><span>已绑定 GitHub <a :href="`https://github.com/${encodeURIComponent(profile.githubLogin)}`" target="_blank" rel="noopener noreferrer">{{ profile.githubName || profile.githubLogin }}</a></span></li>
      <li v-if="profile.codeforcesHandle"><span class="platform-symbol" aria-hidden="true">CF</span><a :href="`https://codeforces.com/profile/${encodeURIComponent(profile.codeforcesHandle)}`" target="_blank" rel="noopener noreferrer">{{ profile.codeforcesHandle }}</a></li>
      <li v-if="nowcoderUid"><span class="platform-symbol" aria-hidden="true">NC</span><span>牛客 <a :href="`https://ac.nowcoder.com/acm/contest/profile/${nowcoderUid}/practice-coding`" target="_blank" rel="noopener noreferrer">{{ nowcoderUid }}</a></span></li>
    </ul>
    <section class="contributions" aria-label="成就贡献">
      <h2>成就贡献</h2>
      <div v-for="item in achievements" :key="item.name" class="contribution" :class="item.color">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" aria-hidden="true">
          <template v-if="item.icon === 'eye'"><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12Z" /><circle cx="12" cy="12" r="3" /></template>
          <path v-else-if="item.icon === 'like'" d="M8 21H4V10h4m0 11h11l2-11h-7l1-6-3-2-4 8v11Z" stroke-linejoin="round" />
          <path v-else d="m12 2 3 6 7 1-5 5 1 7-6-3-6 3 1-7-5-5 7-1Z" stroke-linejoin="round" />
        </svg>
        <div><p>{{ item.name }} <strong>{{ item.value?.toLocaleString('zh-CN') ?? '—' }}</strong></p><span>昨日 {{ item.yesterday?.toLocaleString('zh-CN') ?? '—' }}</span></div>
      </div>
    </section>
    <p v-if="error" class="social-error" role="alert">{{ error }} <button @click="loadSummary">重试</button></p>
    <dialog ref="dialog" class="follow-dialog" :aria-label="kind === 'following' ? '关注列表' : '关注者列表'" aria-modal="true" @cancel.prevent="closeMembers">
      <div class="dialog-heading"><h2>{{ kind === 'following' ? '关注' : '关注者' }} <span v-if="members">{{ members.total }}</span></h2><button autofocus aria-label="关闭列表" @click="closeMembers">×</button></div>
      <p v-if="listBusy" role="status" class="list-status">正在加载…</p>
      <p v-if="listError" role="alert" class="social-error">{{ listError }} <button @click="loadMembers(members?.page)">重试</button></p>
      <p v-if="!listBusy && !listError && !members?.records.length" class="list-status">{{ kind === 'following' ? '还没有关注其他用户' : '还没有关注者' }}</p>
      <ul v-if="members" class="member-list"><li v-for="member in members.records" :key="member.id">
        <RouterLink :to="`/users/${member.id}`" class="member-link" @click="closeMembers"><div class="member-avatar"><UserAvatar :src="member.avatarUrl" :name="member.nickname" /></div><div><strong>{{ member.nickname }}</strong><span>@{{ member.username }}</span></div></RouterLink>
        <button v-if="auth.isLoggedIn && member.id !== auth.user?.id" :disabled="memberBusy !== null" @click="toggleMember(member)">{{ memberBusy === member.id ? '处理中…' : member.following ? '取消关注' : '关注' }}</button>
      </li></ul>
      <div v-if="members && members.pages > 1" class="list-pagination"><button :disabled="listBusy || members.page <= 1" @click="loadMembers(members.page - 1)">上一页</button><span>{{ members.page }} / {{ members.pages }}</span><button :disabled="listBusy || members.page >= members.pages" @click="loadMembers(members.page + 1)">下一页</button></div>
    </dialog>
  </aside>
</template>
<style scoped>
.profile-sidebar { padding: 28px 24px; background: var(--color-surface-card); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); align-self: start; }
.identity-row { display: flex; align-items: center; gap: 18px; }
.profile-avatar { width: 96px; height: 96px; border-radius: 16px; flex-shrink: 0; overflow: hidden; }
.identity-text { min-width: 0; }.identity-text h1 { font: 700 24px/1.4 var(--font-headline); margin: 0 0 5px; overflow-wrap: anywhere; }.identity-text p { color: var(--color-on-surface-variant); margin: 0; overflow-wrap: anywhere; }
.admin-label { display: inline-block; margin-top: 8px; font-size: 12px; color: var(--color-primary-container); }.profile-bio { margin: 20px 0 0; line-height: 1.7; overflow-wrap: anywhere; }
button,a { -webkit-tap-highlight-color: transparent; }button { font: inherit; cursor: pointer; }button:disabled { opacity: .5; cursor: not-allowed; }button:focus-visible,a:focus-visible { outline: 2px solid var(--color-primary-container); outline-offset: 3px; }
.follow-counts { display: flex; align-items: center; gap: 16px; margin: 24px 0 16px; }.follow-counts button { background: none; border: 0; padding: 0; color: var(--color-on-surface-variant); }.follow-counts strong { color: var(--color-on-surface); font-weight: 600; }.follow-counts > span { color: var(--color-border-subtle); }
.profile-actions { display: flex; gap: 10px; }.edit-profile { flex: 1; display: grid; place-items: center; min-height: 44px; padding: 8px; border: 0; border-radius: 8px; background: #e8f3ed; color: #238257; font-weight: 600; text-decoration: none; text-align: center; }.share-profile { display: grid; place-items: center; width: 44px; border: 0; border-radius: 8px; background: var(--color-surface-container-low); color: var(--color-on-surface); }.share-profile svg { width: 23px; height: 23px; }
.profile-details { display: grid; gap: 18px; padding: 26px 0; margin: 0; list-style: none; }.profile-details li { display: flex; align-items: center; gap: 12px; color: var(--color-on-surface-variant); font-size: 14px; overflow-wrap: anywhere; }.profile-details svg { width: 22px; height: 22px; flex-shrink: 0; }.ip-symbol { display: grid; place-items: center; width: 21px; height: 21px; border: 1.7px solid currentColor; border-radius: 50%; font-size: 10px; flex-shrink: 0; }.platform-symbol { font-size: 10px; font-weight: 700; width: 22px; }.profile-details a { color: var(--color-primary-container); }
.contributions { border-top: 1px solid var(--color-border-subtle); padding-top: 24px; }.contributions h2 { font-size: 18px; margin: 0 0 26px; }.contribution { display: flex; gap: 12px; align-items: start; margin: 0 0 24px; }.contribution svg { width: 23px; height: 23px; flex-shrink: 0; }.contribution p { color: var(--color-on-surface-variant); margin: 0 0 7px; }.contribution strong { color: var(--color-on-surface); margin-left: 6px; font-weight: 600; }.contribution span { color: var(--color-on-surface-variant); font-size: 13px; }.blue svg { color: #497deb; }.teal svg { color: #20aa97; }.amber svg { color: #e7a11d; }
.social-error { color: var(--color-status-critical); font-size: 13px; line-height: 1.6; }.social-error button { border: 0; background: none; text-decoration: underline; color: inherit; }
.follow-dialog { margin: auto; width: min(500px,calc(100vw - 32px)); max-height: calc(100dvh - 48px); padding: 24px; border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); background: var(--color-surface-card); color: var(--color-on-surface); }.follow-dialog::backdrop { background: rgb(0 0 0 / 45%); backdrop-filter: blur(3px); }.dialog-heading { display: flex; justify-content: space-between; align-items: center; }.dialog-heading h2 { font-size: 20px; margin: 0; }.dialog-heading span { color: var(--color-on-surface-variant); font-size: 14px; }.dialog-heading > button { background: none; border: 0; font-size: 26px; color: inherit; }.list-status { text-align: center; color: var(--color-on-surface-variant); padding: 24px 0; }.member-list { list-style: none; margin: 16px 0 0; padding: 0; }.member-list li { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px 0; border-bottom: 1px solid var(--color-border-subtle); }.member-link { display: flex; align-items: center; gap: 12px; color: inherit; text-decoration: none; min-width: 0; }.member-link strong { display: block; overflow-wrap: anywhere; }.member-link span { display: block; color: var(--color-on-surface-variant); font-size: 12px; margin-top: 5px; }.member-avatar { width: 40px; height: 40px; border-radius: 10px; flex-shrink: 0; }.member-list button,.list-pagination button { padding: 7px 10px; background: var(--color-surface-container-low); color: inherit; border: 1px solid var(--color-border-subtle); border-radius: 6px; flex-shrink: 0; }.list-pagination { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 20px; font-size: 13px; }
</style>
