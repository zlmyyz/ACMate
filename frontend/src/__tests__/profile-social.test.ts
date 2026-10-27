import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import UserProfileView from '@/views/UserProfileView.vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import SubmissionHeatmap from '@/components/common/SubmissionHeatmap.vue'
import type { UserProfile } from '@/types/user'
const calls = vi.hoisted(() => ({ getUserProfile: vi.fn(), getSocialSummary: vi.fn(), getFollowList: vi.fn(), setFollow: vi.fn(), getSubmissionActivity: vi.fn(), getRecentSubmissions: vi.fn(), getOjOverview: vi.fn(), getNowcoderHistoryAccounts: vi.fn(), getNowcoderHistoryOverview: vi.fn(), getNowcoderHistoryActivity: vi.fn(), getNowcoderHistoryRecent: vi.fn() }))
const nc=vi.hoisted(()=>({getNowcoderAccount:vi.fn(),getProfileSync:vi.fn(),enqueueProfileSync:vi.fn(),bindNowcoderAccount:vi.fn(),retryProblemImports:vi.fn()}))
vi.mock('@/api/nowcoder-training',()=>nc)
vi.mock('@/api/users', () => ({ getUserProfile: calls.getUserProfile }))
vi.mock('@/api/profile-social', () => calls)
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn(), error: vi.fn() } }))
const profile: UserProfile = { id: 1, username: 'member', nickname: '社团成员', avatarUrl: '/uploads/avatar.png', bio: '一起练习算法', admin: false, accountStatus: 'ACTIVE', createdProblemCount: 0, codeforcesHandle: null, githubLogin: 'octocat', githubName: 'Octocat', location: '河北石家庄', gender: 'MALE', ojStats: { solvedCount: 3, solvedCount30d: 2, solvedCount7d: 1, lastAcceptedTime: null }, createTime: '2026-10-01T12:00:00' }
const summary = { followingCount: 2, followerCount: 1, following: false, ipRegion: '河北', contributions: { views: 20, yesterdayViews: 3, likes: 5, yesterdayLikes: 1, favorites: 2, yesterdayFavorites: 0 } }
const activity = { start: '2026-10-24', end: '2026-10-26', submissions: 5, activeDays: 1, days: [{ date: '2026-10-24', count: 0 }, { date: '2026-10-25', count: 5 }, { date: '2026-10-26', count: 0 }] }
beforeEach(() => {
  vi.resetAllMocks()
  nc.getNowcoderAccount.mockResolvedValue({hasAccount:false,verifyStatus:null});nc.getProfileSync.mockResolvedValue({status:'NOT_STARTED',nextPage:1,totalPages:0,fetched:0,inserted:0,importPending:0,importDone:0,importFailed:0})
  calls.getNowcoderHistoryAccounts.mockResolvedValue([])
  calls.getOjOverview.mockResolvedValue({accounts:[],stats:profile.ojStats,todaySubmissions:5,todayAttempted:2,todaySolved:1})
  Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value: function(this: HTMLDialogElement) { this.open = true } })
  Object.defineProperty(HTMLDialogElement.prototype, 'close', { configurable: true, value: function(this: HTMLDialogElement) { this.open = false } })
  calls.getUserProfile.mockResolvedValue(profile); calls.getSocialSummary.mockResolvedValue(summary)
  calls.getSubmissionActivity.mockResolvedValue(activity); calls.getRecentSubmissions.mockResolvedValue([])
  calls.getFollowList.mockResolvedValue({ page: 1, size: 20, total: 1, pages: 1, records: [{ id: 2, username: 'friend', nickname: '算法伙伴', avatarUrl: null, following: true }] })
})
async function render(loggedIn = true) {
  const pinia = createPinia(); const auth = useAuthStore(pinia)
  auth.user = loggedIn ? { id: 1, username: 'member', nickname: '社团成员', email: null, avatarUrl: null, bio: null, admin: false } : null
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/users/:id', component: { template: '<div />' } },{ path: '/:pathMatch(.*)*', component: { template: '<div />' } }] })
  await router.push('/users/1'); await router.isReady()
  const wrapper = mount(UserProfileView, { global: { plugins: [pinia, router] } }); await flushPromises()
  return { wrapper, router }
}
describe('personal profile', () => {
  it('shows reference sidebar and only stats, heatmap and recent submissions on the right', async () => {
    const { wrapper } = await render()
    expect(wrapper.text()).toContain('河北石家庄'); expect(wrapper.text()).toContain('成就贡献')
    expect(wrapper.text()).toContain('获得收藏'); expect(wrapper.text()).not.toContain('全站排名'); expect(wrapper.text()).not.toContain('声望')
    expect(wrapper.get('.edit-profile').attributes('href')).toBe('/settings/profile')
    const right = wrapper.get('.profile-content').text()
    expect(right).toContain('刷题统计'); expect(right).toContain('每日提交'); expect(right).toContain('最近提交')
    expect(right).not.toContain('公开题目'); expect(right).not.toContain('训练计划')
  })
  it('lists followees in a modal and supports unfollowing', async () => {
    const { wrapper } = await render()
    await wrapper.get('.follow-counts button').trigger('click'); await flushPromises()
    expect((wrapper.get('.follow-dialog').element as HTMLDialogElement).open).toBe(true)
    expect(wrapper.get('.member-link').attributes('href')).toBe('/users/2')
    expect(wrapper.get('.member-list').text()).toContain('算法伙伴')
    await wrapper.get('.member-list button').trigger('click'); await flushPromises()
    expect(calls.setFollow).toHaveBeenCalledWith(2, false)
  })
  it('can follow another user from their profile', async () => {
    calls.getUserProfile.mockResolvedValue({ ...profile, id: 2 })
    calls.getSocialSummary.mockResolvedValueOnce(summary).mockResolvedValue({ ...summary, following: true })
    const { wrapper } = await render()
    await wrapper.get('button.edit-profile').trigger('click'); await flushPromises()
    expect(calls.setFollow).toHaveBeenCalledWith(2, true)
    expect(wrapper.get('button.edit-profile').text()).toContain('取消关注')
  })
  it('shows real recent submission rows and linked problems', async () => {
    calls.getRecentSubmissions.mockResolvedValue(Array.from({ length: 10 }, (_, index) => ({ id: index + 1, platform: 'CODEFORCES', problemKey: '1A', problemId: 7, title: '题目', verdict: 'OK', language: 'C++', submittedTime: '2026-10-26T08:00:00' })))
    const { wrapper } = await render()
    expect(wrapper.findAll('.submission-list li')).toHaveLength(10)
    expect(wrapper.get('.submission-main a').attributes('href')).toBe('/problems/7')
    expect(wrapper.get('.verdict').text()).toBe('通过')
  })
  it('shows an empty state instead of fabricated submission records', async () => {
    const { wrapper } = await render()
    expect(wrapper.get('.empty-submissions').text()).toContain('暂无提交记录')
    expect(wrapper.findAll('.submission-list li')).toHaveLength(0)
  })
  it('shows verified Nowcoder UID and switches all metrics to Nowcoder',async()=>{
    calls.getOjOverview.mockResolvedValue({accounts:[{platform:'NOWCODER',uid:'123',lastSyncTime:null}],stats:profile.ojStats,todaySubmissions:5,todayAttempted:2,todaySolved:1})
    const {wrapper}=await render(false)
    expect(wrapper.get('.profile-sidebar a[href*="nowcoder"]').attributes('href')).toContain('/123/practice-coding')
    expect(wrapper.text()).toContain('今日新增通过');expect(nc.getNowcoderAccount).not.toHaveBeenCalled()
    await wrapper.get('[aria-label="刷题统计平台"]').setValue('NOWCODER');await flushPromises()
    expect(calls.getOjOverview).toHaveBeenLastCalledWith(1,'NOWCODER');expect(calls.getSubmissionActivity).toHaveBeenLastCalledWith(1,'NOWCODER');expect(calls.getRecentSubmissions).toHaveBeenLastCalledWith(1,'NOWCODER')
    wrapper.unmount()
  })
  it('selects separate statistics for each historic Nowcoder account',async()=>{
    calls.getNowcoderHistoryAccounts.mockResolvedValue([{uid:'123',submissions:2},{uid:'456',submissions:3},{uid:'UNKNOWN',submissions:1}])
    calls.getNowcoderHistoryOverview.mockResolvedValue({accounts:[],stats:profile.ojStats,todaySubmissions:3,todayAttempted:1,todaySolved:1})
    calls.getNowcoderHistoryActivity.mockResolvedValue(activity);calls.getNowcoderHistoryRecent.mockResolvedValue([])
    const {wrapper}=await render(false);await wrapper.get('[aria-label="刷题统计平台"]').setValue('NOWCODER');await flushPromises()
    expect(wrapper.get('[aria-label="牛客历史账号"]').text()).toContain('归属未知的历史记录')
    await wrapper.get('[aria-label="牛客历史账号"]').setValue('456');await flushPromises()
    expect(calls.getNowcoderHistoryOverview).toHaveBeenLastCalledWith(1,'456');expect(calls.getNowcoderHistoryActivity).toHaveBeenLastCalledWith(1,'456');expect(calls.getNowcoderHistoryRecent).toHaveBeenLastCalledWith(1,'456')
  })
  it('enqueues verified personal history and shows async statement progress',async()=>{
    nc.getNowcoderAccount.mockResolvedValue({hasAccount:true,uid:'123',verifyStatus:1})
    nc.enqueueProfileSync.mockResolvedValue({status:'QUEUED',nextPage:1,totalPages:3,fetched:0,inserted:0,importPending:2,importDone:0,importFailed:0})
    const {wrapper}=await render()
    expect(nc.enqueueProfileSync).toHaveBeenCalledOnce();expect(wrapper.get('[aria-label="牛客账号同步"]').text()).toContain('2 待处理')
    expect(wrapper.get('.sync-nowcoder').attributes('disabled')).toBeDefined();wrapper.unmount()
  })
  it('asks guests to log in before following', async () => {
    calls.getUserProfile.mockResolvedValue({ ...profile, id: 2 })
    const { wrapper, router } = await render(false)
    await wrapper.get('button.edit-profile').trigger('click'); await flushPromises()
    expect(router.currentRoute.value.path).toBe('/login'); expect(calls.setFollow).not.toHaveBeenCalled()
  })
})
it('renders heatmap levels from submission counts, including empty dates', () => {
  const wrapper = mount(SubmissionHeatmap, { props: { activity } })
  expect(wrapper.findAll('.day')).toHaveLength(3); expect(wrapper.findAll('.level-2.day')).toHaveLength(1)
  expect(wrapper.get('.level-2.day').attributes('aria-label')).toContain('5 次提交')
})
it('separates calendar months and displays date and count on hover', async () => {
  vi.useFakeTimers()
  const wrapper = mount(SubmissionHeatmap, { props: { activity: { ...activity, start: '2026-09-30', end: '2026-10-01', days: [{ date: '2026-09-30', count: 2 }, { date: '2026-10-01', count: 0 }] } } })
  expect(wrapper.findAll('.month-heatmap')).toHaveLength(2)
  expect(wrapper.findAll('.month-heatmap').map(item => item.attributes('aria-label'))).toEqual(['2026年9月', '2026年10月'])
  expect(wrapper.findAll('.month-caption').map(item => item.text())).toEqual(['9月', '10月'])
  await wrapper.get('.day').trigger('mouseenter')
  expect(document.body.querySelector('[role="tooltip"]')).toBeNull()
  await vi.advanceTimersByTimeAsync(399)
  expect(document.body.querySelector('[role="tooltip"]')).toBeNull()
  await vi.advanceTimersByTimeAsync(1)
  expect(document.body.querySelector('[role="tooltip"]')?.textContent).toContain('2026-09-30')
  expect(document.body.querySelector('[role="tooltip"]')?.textContent).toContain('2 个提交，2026-09-30')
  await wrapper.get('.day').trigger('mouseleave')
  expect(document.body.querySelector('[role="tooltip"]')).toBeNull()
  wrapper.unmount()
  vi.useRealTimers()
})
it('cancels or restarts the hover delay when leaving or switching cells', async () => {
  vi.useFakeTimers()
  const wrapper = mount(SubmissionHeatmap, { props: { activity } })
  const days = wrapper.findAll('.day')
  await days[0]!.trigger('mouseenter')
  await vi.advanceTimersByTimeAsync(200)
  await days[0]!.trigger('mouseleave')
  await vi.advanceTimersByTimeAsync(400)
  expect(document.body.querySelector('[role="tooltip"]')).toBeNull()
  await days[0]!.trigger('mouseenter')
  await vi.advanceTimersByTimeAsync(200)
  await days[1]!.trigger('mouseenter')
  await vi.advanceTimersByTimeAsync(399)
  expect(document.body.querySelector('[role="tooltip"]')).toBeNull()
  await vi.advanceTimersByTimeAsync(1)
  expect(document.body.querySelector('[role="tooltip"]')?.textContent).toContain('2026-10-25')
  wrapper.unmount()
  vi.useRealTimers()
})
it('falls back to the default avatar on errors and refreshes after a new upload', async () => {
  const wrapper = mount(UserAvatar, { props: { src: 'https://avatars.githubusercontent.com/u/1?v=4', name: '成员' } })
  expect(wrapper.get('img').attributes('src')).toContain('avatars.githubusercontent.com')
  await wrapper.get('img').trigger('error'); expect(wrapper.get('img').attributes('src')).toBe('/default-avatar.svg')
  await wrapper.setProps({ src: '/uploads/new.png' }); expect(wrapper.get('img').attributes('src')).toBe('/uploads/new.png')
})
