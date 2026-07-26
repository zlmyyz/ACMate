<script setup lang="ts">
import MarkdownIt from 'markdown-it'

const props = defineProps<{
  content: string | null
}>()

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: false,
})

const defaultUrlTransform = md.validateLink
md.validateLink = (url: string) => {
  const trimmed = url.trim().toLowerCase()
  if (trimmed.startsWith('javascript:') || trimmed.startsWith('data:') || trimmed.startsWith('vbscript:')) {
    return false
  }
  return defaultUrlTransform ? defaultUrlTransform(url) : true
}

function renderMarkdown(raw: string | null): string {
  if (!raw) return ''
  return md.render(raw)
}

function rendered(): string {
  return renderMarkdown(props.content)
}
</script>

<template>
  <div v-if="content" class="markdown-content" v-html="rendered()" />
  <p v-else class="no-content">暂无内容</p>
</template>

<style scoped>
.markdown-content {
  color: var(--color-on-surface);
  font-size: var(--text-body-md);
  line-height: 1.8;
  word-break: break-word;
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3) {
  font-family: var(--font-headline);
  font-weight: 600;
  margin-top: 1.5em;
  margin-bottom: 0.5em;
  color: var(--color-on-surface);
}

.markdown-content :deep(h1) { font-size: 1.4em; }
.markdown-content :deep(h2) { font-size: 1.2em; }
.markdown-content :deep(h3) { font-size: 1.1em; }

.markdown-content :deep(p) {
  margin: 0.6em 0;
}

.markdown-content :deep(pre) {
  background: var(--color-surface-container);
  border: 1px solid var(--color-border-subtle);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  overflow-x: auto;
  font-family: var(--font-mono);
  font-size: var(--text-code-sm);
  line-height: 1.6;
}

.markdown-content :deep(code) {
  font-family: var(--font-mono);
  font-size: 0.9em;
  background: var(--color-surface-container);
  padding: 1px 6px;
  border-radius: var(--radius-sm);
}

.markdown-content :deep(pre code) {
  background: none;
  padding: 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  padding-left: 1.5em;
  margin: 0.5em 0;
}

.markdown-content :deep(li) {
  margin: 0.2em 0;
}

.markdown-content :deep(blockquote) {
  border-left: 3px solid var(--color-primary-container);
  padding: 4px 14px;
  margin: 0.6em 0;
  color: var(--color-secondary);
  background: var(--color-surface-container-low);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.markdown-content :deep(a) {
  color: var(--color-primary-container);
  text-decoration: underline;
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1em 0;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid var(--color-border-subtle);
  padding: 8px 12px;
  text-align: left;
}

.markdown-content :deep(th) {
  background: var(--color-surface-container-low);
  font-weight: 600;
}

.markdown-content :deep(img) {
  max-width: 100%;
  border-radius: var(--radius-md);
}

.no-content {
  color: var(--color-on-surface-variant);
  font-size: var(--text-body-md);
  text-align: center;
  padding: 24px;
}
</style>
