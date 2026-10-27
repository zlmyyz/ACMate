<script setup lang="ts">
import { computed } from 'vue'
import katex from 'katex'
import 'katex/dist/katex.min.css'
import type { DataConstraint } from '@/utils/problem-constraints'

const props = defineProps<{ range: DataConstraint }>()
const rendered = computed(() => {
  if (!props.range.latex) return null
  try {
    return katex.renderToString(props.range.latex, {
      throwOnError: true,
      trust: false,
      maxExpand: 100,
      maxSize: 10,
      strict: 'ignore',
    })
  } catch {
    return null
  }
})
</script>
<template>
  <span v-if="rendered" class="constraint-formula" v-html="rendered" />
  <code v-else>{{ range.text }}</code>
</template>
