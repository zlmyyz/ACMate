import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import LoginView from '@/views/LoginView.vue'
import AnimatedLoginCharacters from '@/components/ui/AnimatedLoginCharacters.vue'

vi.mock('@/api/auth', async (original) => ({
  ...await original<typeof import('@/api/auth')>(),
  getLoginMethods: vi.fn().mockResolvedValue({ email: true, github: true }),
}))

let wrapper: VueWrapper | undefined
beforeEach(() => {
  vi.useFakeTimers()
  vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => window.setTimeout(() => callback(0), 16))
  vi.stubGlobal('cancelAnimationFrame', (id: number) => window.clearTimeout(id))
})
afterEach(() => {
  wrapper?.unmount(); wrapper = undefined
  vi.restoreAllMocks(); vi.unstubAllGlobals(); vi.useRealTimers()
})

async function login() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/login', component: LoginView }] })
  await router.push('/login'); await router.isReady()
  wrapper = mount(LoginView, { attachTo: document.body, global: { plugins: [createPinia(), router] } })
  await flushPromises()
  return wrapper
}

it('only username focus stretches the purple character and triggers the mutual glance', async () => {
  const view = await login()
  const characters = view.getComponent(AnimatedLoginCharacters)
  await view.get('#login-password').trigger('focus')
  expect(characters.props('isUsernameFocused')).toBe(false)
  expect((characters.get('.mascot-1').element as HTMLElement).style.height).toBe('400px')
  await view.get('#login-username').trigger('focus')
  expect(characters.props('isUsernameFocused')).toBe(true)
  expect((characters.get('.mascot-1').element as HTMLElement).style.height).toBe('440px')
  expect((characters.get('.mascot-1 .mascot-eyes').element as HTMLElement).style.top).toBe('65px')
  await view.get('#login-username').trigger('blur')
  await view.get('#login-password').trigger('focus')
  expect((characters.get('.mascot-1').element as HTMLElement).style.height).toBe('400px')
  expect((characters.get('.mascot-1 .mascot-eyes').element as HTMLElement).style.top).toBe('40px')
})

it('switching to email login clears the username focus animation', async () => {
  const view = await login()
  await view.get('#login-username').trigger('focus')
  await view.findAll('.login-tabs button')[1]!.trigger('click')
  expect(view.getComponent(AnimatedLoginCharacters).props('isUsernameFocused')).toBe(false)
  expect(view.find('#login-username').exists()).toBe(false)
})

it('only the yellow character has a mouth, which follows pointer movement together with its face', async () => {
  wrapper = mount(AnimatedLoginCharacters, { global: { stubs: { RouterLink: true } } })
  const stage = wrapper.get('.mascot-stage').element as HTMLElement
  Object.defineProperty(stage, 'offsetWidth', { configurable: true, value: 470 })
  vi.spyOn(stage, 'getBoundingClientRect').mockReturnValue({ left: 0, top: 0, bottom: 466, right: 470, width: 470, height: 466, x: 0, y: 0, toJSON: () => ({}) })
  expect(wrapper.findAll('.mascot-mouth')).toHaveLength(1)
  const mouth = wrapper.get('.mascot-4 .mascot-mouth').element as HTMLElement
  const eyes = wrapper.get('.mascot-4 .mascot-eyes').element as HTMLElement
  window.dispatchEvent(new MouseEvent('pointermove', { clientX: 0, clientY: 0 }))
  await vi.advanceTimersByTimeAsync(20)
  const left = Number.parseFloat(mouth.style.left); const top = Number.parseFloat(mouth.style.top)
  window.dispatchEvent(new MouseEvent('pointermove', { clientX: 1000, clientY: 900 }))
  await vi.advanceTimersByTimeAsync(20)
  expect(Number.parseFloat(mouth.style.left)).toBeGreaterThan(left)
  expect(Number.parseFloat(mouth.style.top)).toBeGreaterThan(top)
  expect(Number.parseFloat(eyes.style.left) - Number.parseFloat(mouth.style.left)).toBe(6)
  expect(Number.parseFloat(mouth.style.top) - Number.parseFloat(eyes.style.top)).toBe(34)
})
