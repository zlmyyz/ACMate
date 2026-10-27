<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getUserProfile } from '@/api/users'
import type { UserProfile } from '@/types/user'
import { getSubmissionActivity, getRecentSubmissions, getOjOverview, getNowcoderHistoryAccounts, getNowcoderHistoryOverview, getNowcoderHistoryActivity, getNowcoderHistoryRecent, type NowcoderHistoryAccount, type OjOverview, type OjPlatform, type SubmissionActivity, type RecentSubmission } from '@/api/profile-social'
import PageContainer from '@/components/layout/PageContainer.vue'
import LoadingState from '@/components/common/LoadingState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import ProfileSidebar from '@/components/common/ProfileSidebar.vue'
import SubmissionHeatmap from '@/components/common/SubmissionHeatmap.vue'
import NowcoderProfilePanel from '@/components/common/NowcoderProfilePanel.vue'
const route = useRoute()
const profile = ref<UserProfile | null>(null)
const activity = ref<SubmissionActivity | null>(null)
const recent = ref<RecentSubmission[]>([])
const overview=ref<OjOverview|null>(null), platform=ref<OjPlatform>('ALL'), metricsLoading=ref(false), overviewError=ref(false)
const historyAccounts=ref<NowcoderHistoryAccount[]>([]), selectedUid=ref('ALL'), historyError=ref(false)
const nowcoderUid=computed(()=>overview.value?.accounts.find(a=>a.platform==='NOWCODER')?.uid)
const stats=computed(()=>overview.value?.stats ?? (platform.value==='ALL'?profile.value?.ojStats:null))
let metricsRequest=0
const loading = ref(true)
const error = ref('')
const activityError = ref(false)
const recentError = ref(false)
const userId = computed(() => Number(route.params.id))
let request = 0
async function load() {
  const token = ++request
  loading.value = true; error.value = ''; activityError.value = false; recentError.value = false
  const results = await Promise.allSettled([getUserProfile(userId.value), getSubmissionActivity(userId.value,platform.value), getRecentSubmissions(userId.value,platform.value), getOjOverview(userId.value,platform.value)])
  if (token !== request) return
  if (results[0].status === 'fulfilled') profile.value = results[0].value
  else { profile.value = null; error.value = (results[0].reason as { response?: { status: number } }).response?.status === 404 ? '用户不存在' : '个人主页加载失败，请稍后重试' }
  if (results[1].status === 'fulfilled') activity.value = results[1].value
  else { activity.value = null; activityError.value = true }
  if (results[2].status === 'fulfilled') recent.value = results[2].value
  else { recent.value = []; recentError.value = true }
  if(results[3].status==='fulfilled'){overview.value=results[3].value;overviewError.value=false}else{overview.value=null;overviewError.value=true}
  loading.value = false
}
async function refreshMetrics() {
  const token=++metricsRequest, id=userId.value, selected=platform.value, uid=selectedUid.value
  metricsLoading.value=true
  const results=selected==='NOWCODER' && uid!=='ALL'
    ? await Promise.allSettled([getNowcoderHistoryOverview(id,uid),getNowcoderHistoryActivity(id,uid),getNowcoderHistoryRecent(id,uid)])
    : await Promise.allSettled([getOjOverview(id,selected),getSubmissionActivity(id,selected),getRecentSubmissions(id,selected)])
  if(token!==metricsRequest || id!==userId.value || selected!==platform.value || uid!==selectedUid.value)return
  overviewError.value=results[0].status==='rejected';overview.value=results[0].status==='fulfilled'?results[0].value:null
  activityError.value=results[1].status==='rejected';activity.value=results[1].status==='fulfilled'?results[1].value:null
  recentError.value=results[2].status==='rejected';recent.value=results[2].status==='fulfilled'?results[2].value:[]
  metricsLoading.value=false
}
watch(platform,async value=>{
  selectedUid.value='ALL'
  if(value==='NOWCODER'){
    const id=userId.value;historyError.value=false
    try{const accounts=await getNowcoderHistoryAccounts(id);if(id!==userId.value||platform.value!==value)return;historyAccounts.value=accounts;selectedUid.value=accounts.find(a=>a.uid===nowcoderUid.value)?.uid || accounts[0]?.uid || 'ALL'}
    catch{historyError.value=true;historyAccounts.value=[]}
  }
  void refreshMetrics()
})
watch(selectedUid,()=>{void refreshMetrics()})
function formatTime(value: string) { return new Date(value).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) }
function relativeTime(value: string) {
  const difference = Math.max(0, Date.now() - new Date(value).getTime())
  if (difference < 60_000) return '刚刚'
  if (difference < 3_600_000) return Math.floor(difference / 60_000) + ' 分钟前'
  if (difference < 86_400_000) return Math.floor(difference / 3_600_000) + ' 小时前'
  return Math.floor(difference / 86_400_000) + ' 天前'
}
function accepted(verdict: string) { return ['OK', 'AC', 'ACCEPTED'].includes(verdict.toUpperCase()) }
watch(userId, () => {metricsRequest++;platform.value='ALL';selectedUid.value='ALL';historyAccounts.value=[];void load()}, { immediate: true })
onUnmounted(() => {request++;metricsRequest++})
</script>
<template>
  <PageContainer>
    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" @retry="load" />
    <div v-else-if="profile" class="profile-layout">
      <ProfileSidebar :key="profile.id" :profile="profile" :nowcoder-uid="nowcoderUid" />
      <main class="profile-content">
        <p v-if="profile.accountStatus === 'DISABLED'" class="disabled-banner">该账号已停用</p>
        <section class="profile-section" aria-labelledby="profile-stats-title">
          <div class="stats-heading"><h2 id="profile-stats-title">刷题统计</h2><select v-model="platform" aria-label="刷题统计平台"><option value="ALL">全部平台</option><option value="NOWCODER">牛客</option><option value="CODEFORCES">Codeforces</option></select><select v-if="platform==='NOWCODER' && historyAccounts.length" v-model="selectedUid" aria-label="牛客历史账号"><option value="ALL">全部牛客账号</option><option v-for="account in historyAccounts" :key="account.uid" :value="account.uid">{{ account.uid === 'UNKNOWN' ? '归属未知的历史记录' : '牛客 UID ' + account.uid }}（{{ account.submissions }} 次）</option></select></div>
          <p v-if="historyError" role="alert">牛客账号历史加载失败，请切换平台后重试。</p>
          <p v-if="overviewError" role="alert" class="section-error">统计数据加载失败，<button @click="refreshMetrics">重试</button></p>
          <div class="solve-stats"><div><strong>{{ stats?.solvedCount ?? '—' }}</strong><span>累计通过</span></div><div><strong>{{ stats?.solvedCount30d ?? '—' }}</strong><span>近 30 天通过</span></div><div><strong>{{ stats?.solvedCount7d ?? '—' }}</strong><span>近 7 天通过</span></div></div>
          <p class="last-accepted">{{ stats?.lastAcceptedTime ? '最近通过：' + formatTime(stats.lastAcceptedTime) : '暂无已同步的通过记录。' }}</p>
          <div v-if="overview" class="today-stats"><span>今日提交 <strong>{{ overview.todaySubmissions }}</strong> 次</span><span>今日尝试 <strong>{{ overview.todayAttempted }}</strong> 题</span><span>今日新增通过 <strong>{{ overview.todaySolved }}</strong> 题</span></div>
          <p class="stats-note">{{ metricsLoading?'正在更新统计…':'通过数按首次 AC 去重，提交和热力图按官方提交日期统计。' }}</p>
          <NowcoderProfilePanel :key="userId" :user-id="userId" :public-uid="nowcoderUid" @updated="refreshMetrics" />
        </section>
        <section class="profile-section" aria-labelledby="profile-activity-title">
          <h2 id="profile-activity-title">每日提交</h2>
          <p v-if="activityError" role="alert" class="section-error">热力图加载失败，<button @click="load">重试</button></p>
          <SubmissionHeatmap v-else-if="activity" :activity="activity" />
        </section>
        <section class="profile-section recent-section" aria-labelledby="recent-submissions-title">
          <div class="recent-heading"><h2 id="recent-submissions-title">最近提交</h2><span>最近 10 条</span></div>
          <p v-if="recentError" role="alert" class="section-error">提交记录加载失败，<button @click="load">重试</button></p>
          <p v-else-if="!recent.length" class="empty-submissions">暂无提交记录，OJ 数据接入后会显示最近 10 条提交。</p>
          <ul v-else class="submission-list"><li v-for="submission in recent" :key="submission.id">
            <div class="submission-main"><RouterLink v-if="submission.problemId" :to="'/problems/' + submission.problemId">{{ submission.problemKey }}. {{ submission.title }}</RouterLink><a v-else-if="submission.platform==='NOWCODER' && /^NC[1-9][0-9]*$/.test(submission.problemKey)" class="submission-title" :href="`https://ac.nowcoder.com/acm/problem/${submission.problemKey.slice(2)}`" target="_blank" rel="noopener noreferrer">{{ submission.problemKey }}. {{ submission.title }}</a><span v-else class="submission-title">{{ submission.title === submission.problemKey ? submission.problemKey : submission.problemKey + '. ' + submission.title }}</span><span class="submission-platform">{{ submission.platform === 'NOWCODER' ? '牛客' : submission.platform === 'CODEFORCES' ? 'Codeforces' : submission.platform }}<template v-if="submission.language"> · {{ submission.language }}</template></span></div>
            <div class="submission-result"><span class="verdict" :class="{ accepted: accepted(submission.verdict) }">{{ accepted(submission.verdict) ? '通过' : submission.verdict }}</span><time :datetime="submission.submittedTime" :title="formatTime(submission.submittedTime)">{{ relativeTime(submission.submittedTime) }}</time></div>
          </li></ul>
        </section>
      </main>
    </div>
  </PageContainer>
</template>
<style scoped>
.profile-layout { display: grid; grid-template-columns: minmax(260px,320px) minmax(0,1fr); align-items: start; gap: 28px; }
.profile-content { display: grid; min-width: 0; gap: 22px; }
.profile-section { padding: 26px; min-width: 0; border-radius: var(--radius-lg); border: 1px solid var(--color-border-subtle); background: var(--color-surface-card); }
.profile-section h2 { margin: 0 0 24px; font: 600 20px/1.4 var(--font-headline); color: var(--color-on-surface); }
.solve-stats { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 20px; }.solve-stats > div { display: grid; gap: 8px; }.solve-stats strong { color: var(--color-on-surface); font: 700 32px/1.2 var(--font-headline); }
.solve-stats span,.last-accepted { color: var(--color-on-surface-variant); font-size: 13px; }.last-accepted { margin: 24px 0 0; line-height: 1.6; }
.disabled-banner { padding: 12px 18px; background: #fff1e6; color: #9a5b20; border-radius: 8px; margin: 0; }
.recent-heading { display: flex; justify-content: space-between; align-items: center; gap: 16px; margin-bottom: 20px; }.recent-heading h2 { margin: 0; }.recent-heading > span { font-size: 13px; color: var(--color-on-surface-variant); }
.submission-list { list-style: none; padding: 0; margin: 0; }.submission-list li { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 18px; min-height: 78px; border-radius: 8px; }.submission-list li:nth-child(odd) { background: var(--color-surface-container-low); }
.submission-main { min-width: 0; display: grid; gap: 7px; }.submission-main a,.submission-title { font-weight: 600; color: var(--color-on-surface); text-decoration: none; overflow-wrap: anywhere; line-height: 1.5; }.submission-main a:hover { color: var(--color-primary-container); }
.submission-platform { font-size: 12px; color: var(--color-on-surface-variant); }.submission-result { display: grid; justify-items: end; gap: 8px; flex-shrink: 0; }.verdict { font-size: 12px; color: var(--color-on-surface-variant); }.verdict.accepted { color: #238257; }.submission-result time { color: var(--color-on-surface-variant); font-size: 12px; }
.empty-submissions { padding: 32px 8px; text-align: center; line-height: 1.8; color: var(--color-on-surface-variant); font-size: 14px; }.section-error { color: var(--color-status-critical); font-size: 14px; }.section-error button { background: none; border: 0; color: inherit; cursor: pointer; text-decoration: underline; }
@media(max-width:900px) { .profile-layout { grid-template-columns: minmax(230px,270px) minmax(0,1fr); gap: 18px; }.profile-section { padding: 20px; } }
@media(max-width:700px) { .profile-layout { grid-template-columns: minmax(0,1fr); }.solve-stats { gap: 12px; }.solve-stats strong { font-size: 28px; }.profile-section { padding: 20px 16px; }.submission-list li { padding: 16px 12px; } }
.stats-heading{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:24px}.stats-heading h2{margin:0}.stats-heading select{padding:7px 11px;background:var(--color-surface-container-low);color:var(--color-on-surface);border:1px solid var(--color-border-subtle);border-radius:6px;font:inherit;font-size:13px}.today-stats{display:flex;flex-wrap:wrap;gap:12px 20px;margin-top:18px;font-size:13px;color:var(--color-on-surface-variant)}.today-stats strong{color:var(--color-on-surface);font-weight:600}.stats-note{font-size:12px;color:var(--color-on-surface-variant);line-height:1.7;margin:12px 0 0}
</style>

