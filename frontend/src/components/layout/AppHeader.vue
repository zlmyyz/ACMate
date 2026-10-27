<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { navLabels } from '@/constants/labels'
import { siteBrand } from '@/constants/site'
import HeaderUserControls from './HeaderUserControls.vue'
const auth = useAuthStore()
const route = useRoute()
const moreMenu = ref<HTMLDetailsElement>()
watch(() => route.fullPath, () => { if (moreMenu.value) moreMenu.value.open = false })
function closeOnFocusOut(event: FocusEvent) {
  if (moreMenu.value && !moreMenu.value.contains(event.relatedTarget as Node)) moreMenu.value.open = false
}
</script>

<template>
  <header class="app-header">
    <div class="header-inner">
      <div class="header-left">
        <RouterLink
          to="/"
          class="brand"
          :title="`${siteBrand.schoolName} · ${siteBrand.clubName}`"
          :aria-label="`${siteBrand.clubName}首页`"
        >
          <span class="brand-icon">&lt;/&gt;</span>
          <span class="brand-text">{{ siteBrand.clubName }}</span>
        </RouterLink>
        <nav class="main-nav">
          <RouterLink to="/problems" class="nav-link" active-class="nav-link-active">
            {{ navLabels.problems }}
          </RouterLink>
          <RouterLink to="/my/problems" class="nav-link nav-secondary" active-class="nav-link-active">
            {{ navLabels.myProblems }}
          </RouterLink>
          <RouterLink to="/training-plans" class="nav-link nav-training" active-class="nav-link-active">
            {{ navLabels.trainingPlans }}
          </RouterLink>
          <RouterLink to="/posts" class="nav-link" active-class="nav-link-active">
            {{ navLabels.discussions }}
          </RouterLink>
          <RouterLink to="/leaderboard" class="nav-link nav-secondary" active-class="nav-link-active">
            {{ navLabels.leaderboard }}
          </RouterLink>
          <details ref="moreMenu" class="more-menu" :class="{ 'has-admin': auth.isAdmin }" @keydown.esc="moreMenu && (moreMenu.open = false)" @focusout="closeOnFocusOut">
            <summary :class="{ 'more-active': route.path.startsWith('/admin/') }">显示更多 <svg class="chevron" viewBox="0 0 16 16" aria-hidden="true"><path d="m4 6 4 4 4-4" /></svg></summary>
            <div class="more-dropdown">
              <RouterLink to="/problems" class="more-link compact-link" active-class="nav-link-active">{{ navLabels.problems }}</RouterLink>
              <RouterLink to="/posts" class="more-link compact-link" active-class="nav-link-active">{{ navLabels.discussions }}</RouterLink>
              <RouterLink to="/my/problems" class="more-link secondary-link" active-class="nav-link-active">{{ navLabels.myProblems }}</RouterLink>
              <RouterLink to="/training-plans" class="more-link training-link" active-class="nav-link-active">{{ navLabels.trainingPlans }}</RouterLink>
              <RouterLink to="/leaderboard" class="more-link secondary-link" active-class="nav-link-active">{{ navLabels.leaderboard }}</RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/problems" class="more-link" active-class="nav-link-active">
            {{ navLabels.adminProblems }}
          </RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/users" class="more-link" active-class="nav-link-active">
            {{ navLabels.adminUsers }}
          </RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/posts" class="more-link" active-class="nav-link-active">
            {{ navLabels.adminPosts }}
          </RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/sync-tasks" class="more-link" active-class="nav-link-active">
            {{ navLabels.adminSyncTasks }}
          </RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/audit-logs" class="more-link" active-class="nav-link-active">
            {{ navLabels.adminAuditLogs }}
          </RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin/exports" class="more-link" active-class="nav-link-active">
            {{ navLabels.adminExports }}
          </RouterLink>
            </div>
          </details>
        </nav>
      </div>
      <HeaderUserControls />
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 50;
  min-height: var(--header-height);
  background: var(--color-surface-container-lowest);
  border-bottom: 1px solid var(--color-border-subtle);
  box-shadow: var(--shadow-header);
}

.header-inner {
  max-width: var(--container-max);
  margin: 0 auto;
  padding: 0 var(--space-margin-page);
  min-height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
  flex: 1;
  min-width: 0;
  margin-right: 24px;
}

.brand {
  flex-shrink: 0;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-headline);
  font-size: var(--text-headline-md);
  font-weight: 700;
  color: var(--color-primary);
}

.brand-icon {
  font-family: var(--font-mono);
  font-size: 24px;
}

.main-nav {
  min-width: 0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 24px;
  height: 100%;
}

.nav-link {
  flex-shrink: 0;
  white-space: nowrap;
  color: var(--color-on-surface-variant);
  font-weight: 500;
  font-size: var(--text-body-md);
  transition: color 0.2s;
  height: 100%;
  display: flex;
  align-items: center;
}

.nav-link:hover {
  color: var(--color-primary);
}

.nav-link-active {
  color: var(--color-primary);
  border-bottom: 2px solid var(--color-primary);
  font-weight: 700;
}

.more-menu { position: relative; display: none; flex-shrink: 0; }
.more-menu.has-admin { display: block; }
.more-menu summary { display: flex; align-items: center; gap: 6px; list-style: none; cursor: pointer; white-space: nowrap; padding: 10px 0; color: var(--color-on-surface-variant); font-size: var(--text-body-md); font-weight: 500; transition: color .18s ease; }
.more-menu summary::-webkit-details-marker { display: none; }
.more-menu summary:hover, .more-menu[open] summary, .more-menu summary.more-active { color: var(--color-primary); }
.chevron { display: block; width: 16px; height: 16px; flex-shrink: 0; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; transform-origin: center; transition: transform .24s cubic-bezier(.2,.8,.2,1); }
.more-menu[open] .chevron { transform: rotate(180deg); }
@media (prefers-reduced-motion: reduce) { .chevron, .more-menu summary { transition: none; } }
.more-dropdown { position: absolute; top: calc(100% + 10px); right: 0; z-index: 60; min-width: 170px; max-width: calc(100vw - 24px); padding: 8px; background: var(--color-surface-container-lowest); border: 1px solid var(--color-border-subtle); border-radius: 12px; box-shadow: 0 8px 30px #0002; }
.more-link { display: block; padding: 10px 12px; white-space: nowrap; border-radius: 6px; color: var(--color-on-surface-variant); font-size: var(--text-body-md); }
.more-link:hover { background: var(--color-surface-container-low); color: var(--color-primary); }
.more-link.nav-link-active { border-bottom: 0; color: var(--color-primary); }
.compact-link, .secondary-link, .training-link { display: none; }
@media (max-width: 1200px) {
  .main-nav { gap: 16px; }
  .header-left { gap: 18px; margin-right: 18px; }
  .nav-secondary { display: none; }
  .more-menu { display: block; }
  .secondary-link { display: block; }
}
@media (max-width: 950px) {
  .nav-training { display: none; }
  .training-link { display: block; }
  .brand-text { font-size: 18px; }
}
@media (max-width: 800px) {
  .main-nav > .nav-link { display: none; }
  .compact-link { display: block; }
  .header-inner { padding-left: 16px; padding-right: 16px; }
  .header-left { gap: 12px; margin-right: 12px; }
  .brand-text { display: none; }
}
@media (max-width: 600px) {
  .header-inner { flex-wrap: wrap; gap: 6px; padding-top: 8px; padding-bottom: 8px; }
  .header-left { flex-basis: 100%; margin-right: 0; justify-content: space-between; }
  .brand-text { display: inline; }
  .header-inner > :deep(.user-controls) { width: 100%; justify-content: flex-end; }
  .more-dropdown { right: 0; }
}
</style>


