<script setup lang="ts">
import PageContainer from '@/components/layout/PageContainer.vue'
import AccountBindingsPanel from '@/components/common/AccountBindingsPanel.vue'
</script>

<template>
  <PageContainer>
    <template #header>
      <RouterLink class="back-link" to="/">&larr; 返回主页</RouterLink>
    </template>
    <div class="security-heading">
      <h1>账号安全</h1>
      <p>管理邮箱、绑定账号和登录密码。</p>
    </div>
    <AccountBindingsPanel />
  </PageContainer>
</template>

<style scoped>
.back-link { color: var(--color-primary-container); }
.security-heading { max-width: 640px; }
h1 { margin: 0 0 8px; font-size: var(--text-headline-md); color: var(--color-on-surface); }
p { margin: 0; color: var(--color-on-surface-variant); }
</style>
