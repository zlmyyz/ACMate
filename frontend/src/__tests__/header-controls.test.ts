import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HeaderUserControls from '@/components/layout/HeaderUserControls.vue'
const mocks = vi.hoisted(() => ({ activity: vi.fn() }))
vi.mock('@/api/profile-social', () => ({ getSubmissionActivity: mocks.activity }))
beforeEach(() => { mocks.activity.mockResolvedValue({ days: [{ date: '2026-10-24', count: 2 }, { date: '2026-10-25', count: 1 }, { date: '2026-10-26', count: 0 }] }) })
async function render() {
  const pinia = createPinia()
  const auth = useAuthStore(pinia)
  auth.user = { id: 7, username: 'member', nickname: '成员', email: null, avatarUrl: null, bio: null, admin: false }
  const router = createRouter({ history: createMemoryHistory(), routes: ['/', '/posts', '/problems', '/notifications', '/users/7', '/settings/profile'].map(path => ({ path, component: { template: '<div />' } })) })
  await router.push('/')
  const wrapper = mount(HeaderUserControls, { global: { plugins: [pinia, router] } })
  await flushPromises()
  return { wrapper, router }
}
describe('顶部用户控件', () => {
  it('切换题目与讨论搜索，并传递去空格的关键词', async () => {
    const { wrapper, router } = await render()
    await wrapper.get('input').setValue('  动态规划  ')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/problems')
    expect(router.currentRoute.value.query.keyword).toBe('动态规划')
    await wrapper.get('select').setValue('posts')
    await wrapper.get('form').trigger('submit')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/posts')
    expect(router.currentRoute.value.query.keyword).toBe('动态规划')
    wrapper.unmount()
  })
  it('今日尚未提交时保留截至昨日的真实连续天数', async () => {
    const { wrapper } = await render()
    expect(mocks.activity).toHaveBeenCalledWith(7)
    expect(wrapper.get('.streak').text()).toBe('2')
    expect(wrapper.get('.streak').attributes('title')).toBe('连续提交 2 天')
    expect(wrapper.get('summary img').attributes('src')).toBe('/default-avatar.svg')
    expect(wrapper.get('.account-menu nav').text()).toContain('编辑个人资料')
    wrapper.unmount()
  })
  it('统计加载失败时不伪造零天', async () => {
    mocks.activity.mockRejectedValueOnce(new Error('unavailable'))
    const { wrapper } = await render()
    expect(wrapper.get('.streak').text()).toBe('—')
    wrapper.unmount()
  })
})
