<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="error-page">
    <div class="error-card">
      <h1 class="error-code">403</h1>
      <p class="error-title">访问被拒绝</p>
      <p class="error-desc">你没有权限访问此页面。</p>
      <button class="btn-back" @click="router.push({ name: 'home' })">返回首页</button>
    </div>
  </div>
</template>

<style scoped>
.error-page {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.error-card {
  text-align: center;
}

.error-code {
  font-family: var(--font-headline);
  font-size: 72px;
  font-weight: 700;
  color: var(--color-primary-container);
  line-height: 1;
}

.error-title {
  margin-top: 12px;
  font-family: var(--font-headline);
  font-size: var(--text-headline-md);
  font-weight: 600;
  color: var(--color-on-surface);
}

.error-desc {
  margin-top: 8px;
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
}

.btn-back {
  margin-top: 24px;
  padding: 10px 24px;
  background: var(--color-primary-container);
  color: var(--color-on-primary);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-body-md);
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-back:hover {
  opacity: 0.9;
}
</style>
