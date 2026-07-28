import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '@/views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { requiresAuth: true },
    },
    {
      path: '/leaderboard',
      name: 'leaderboard',
      component: () => import('@/views/LeaderboardView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/settings/oj-account',
      name: 'oj-account',
      component: () => import('@/views/OJAccountView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/admin/problems',
      name: 'admin-problems',
      component: () => import('@/views/AdminProblemsView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/users',
      name: 'admin-users',
      component: () => import('@/views/AdminUsersView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/posts',
      name: 'admin-posts',
      component: () => import('@/views/AdminPostsView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/comments',
      name: 'admin-comments',
      component: () => import('@/views/AdminCommentsView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/sync-tasks',
      name: 'admin-sync-tasks',
      component: () => import('@/views/AdminSyncTasksView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/audit-logs',
      name: 'admin-audit-logs',
      component: () => import('@/views/AdminAuditLogsView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/exports',
      name: 'admin-exports',
      component: () => import('@/views/AdminExportsView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/users/:id',
      name: 'user-profile',
      component: () => import('@/views/UserProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/settings/profile',
      name: 'profile-edit',
      component: () => import('@/views/ProfileEditView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/training-plans',
      name: 'training-plans',
      component: () => import('@/views/TrainingPlanListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/training-plans/create',
      name: 'create-plan',
      component: () => import('@/views/CreatePlanView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/training-plans/:id',
      name: 'plan-detail',
      component: () => import('@/views/TrainingPlanDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/training-plans/:id/edit',
      name: 'edit-plan',
      component: () => import('@/views/EditPlanView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/posts',
      name: 'posts',
      component: () => import('@/views/PostListView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/posts/create',
      name: 'create-post',
      component: () => import('@/views/CreatePostView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/posts/:id',
      name: 'post-detail',
      component: () => import('@/views/PostDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/posts/:id/edit',
      name: 'edit-post',
      component: () => import('@/views/EditPostView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/problems',
      name: 'problems',
      component: () => import('@/views/ProblemsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('@/views/NotificationsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/my/problems',
      name: 'my-problems',
      component: () => import('@/views/MyProblemsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/problems/create',
      name: 'create-problem',
      component: () => import('@/views/CreateProblemView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/problems/:id',
      name: 'problem-detail',
      component: () => import('@/views/ProblemDetailView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/problems/:id/edit',
      name: 'edit-problem',
      component: () => import('@/views/EditProblemView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/403',
      name: 'forbidden',
      component: () => import('@/views/ForbiddenView.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
    },
  ],
})

router.beforeEach((to, _from) => {
  const auth = useAuthStore()

  if (!auth.initialized) return true

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'forbidden' }
  }

  if (to.meta.guestOnly && auth.isLoggedIn) {
    return { name: 'home' }
  }
})

export default router
