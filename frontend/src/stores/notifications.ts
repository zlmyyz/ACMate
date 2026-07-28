import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getNotifications, getUnreadCount, markRead, markAllRead } from '@/api/notifications'
import { useAuthStore } from '@/stores/auth'
import type { NotificationItem } from '@/types/notification'

export const useNotificationStore = defineStore('notifications', () => {
  const unreadCount = ref(0)
  const notifications = ref<NotificationItem[]>([])
  const total = ref(0)
  const loading = ref(false)
  const pollingActive = ref(false)
  let pollingTimer: ReturnType<typeof setInterval> | null = null

  async function fetchUnreadCount() {
    try { unreadCount.value = await getUnreadCount() } catch { /* ignore */ }
  }

  async function fetchNotifications(page: number = 1, size: number = 20, unreadOnly: boolean = false) {
    loading.value = true
    try {
      const r = await getNotifications(page, size, unreadOnly)
      notifications.value = r.items
      total.value = r.total
    } catch { /* ignore */ }
    finally { loading.value = false }
  }

  async function markOneRead(id: number) {
    try {
      await markRead(id)
      const n = notifications.value.find(i => i.id === id)
      if (n && !n.isRead) {
        n.isRead = true
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
    } catch { /* ignore */ }
  }

  async function markAllAsRead() {
    try {
      await markAllRead()
      notifications.value.forEach(i => i.isRead = true)
      unreadCount.value = 0
    } catch { /* ignore */ }
  }

  function startPolling() {
    const auth = useAuthStore()
    if (!auth.isLoggedIn || pollingTimer !== null) return
    pollingActive.value = true
    fetchUnreadCount()
    pollingTimer = setInterval(() => {
      if (pollingActive.value) fetchUnreadCount()
    }, 30_000)
  }

  function stopPolling() {
    pollingActive.value = false
    if (pollingTimer !== null) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
  }

  function handleVisibilityChange() {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) return
    if (document.hidden) {
      pollingActive.value = false
    } else {
      pollingActive.value = true
      fetchUnreadCount()
    }
  }

  function startVisibilityListener() {
    document.addEventListener('visibilitychange', handleVisibilityChange)
  }

  function stopVisibilityListener() {
    document.removeEventListener('visibilitychange', handleVisibilityChange)
  }

  function reset() {
    stopPolling()
    stopVisibilityListener()
    unreadCount.value = 0
    notifications.value = []
    total.value = 0
    loading.value = false
    pollingActive.value = false
  }

  return {
    unreadCount, notifications, total, loading, pollingActive,
    fetchUnreadCount, fetchNotifications, markOneRead, markAllAsRead,
    startPolling, stopPolling, handleVisibilityChange,
    startVisibilityListener, stopVisibilityListener, reset,
  }
})
