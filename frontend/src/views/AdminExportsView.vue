<script setup lang="ts">
import PageContainer from '@/components/layout/PageContainer.vue'

const exportUrl = '/api/admin/exports'
</script>

<template>
  <PageContainer>
    <template #header><h1 class="page-title">数据导出</h1></template>

    <div class="export-grid">
      <div class="export-card">
        <h3>题目列表</h3>
        <p>导出所有题目（含停用题目）的 CSV 文件。</p>
        <a :href="`${exportUrl}/problems`" class="export-btn" download>导出 CSV</a>
      </div>
      <div class="export-card">
        <h3>排行榜</h3>
        <p>导出当前排行榜数据。</p>
        <div class="export-options">
          <a :href="`${exportUrl}/leaderboard?period=total`" class="export-btn" download>总榜</a>
          <a :href="`${exportUrl}/leaderboard?period=7d`" class="export-btn" download>近7天</a>
          <a :href="`${exportUrl}/leaderboard?period=30d`" class="export-btn" download>近30天</a>
        </div>
      </div>
    </div>
  </PageContainer>
</template>

<style scoped>
.page-title { font-family: var(--font-headline); font-size: var(--text-display-lg); font-weight: 700; color: var(--color-on-surface); }
.export-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: var(--space-stack-md); }
.export-card { background: var(--color-surface-card); border: 1px solid var(--color-border-subtle); border-radius: var(--radius-lg); padding: 20px; }
.export-card h3 { font-size: var(--text-headline-sm); font-weight: 600; margin-bottom: 8px; }
.export-card p { color: var(--color-on-surface-variant); font-size: var(--text-body-md); margin-bottom: 12px; }
.export-options { display: flex; gap: 8px; flex-wrap: wrap; }
.export-btn {
  display: inline-block; padding: 8px 20px; background: var(--color-primary-container); color: var(--color-on-primary);
  border-radius: var(--radius-md); font-size: var(--text-body-md); font-weight: 500;
}
.export-btn:hover { opacity: 0.85; }
</style>
