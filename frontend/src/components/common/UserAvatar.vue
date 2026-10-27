<script setup lang="ts">
import { computed, ref, watch } from 'vue'
const props = defineProps<{ src?: string | null; name?: string }>()
const failed = ref(false)
watch(() => props.src, () => { failed.value = false })
const source = computed(() => !failed.value && props.src ? props.src : '/default-avatar.svg')
</script>
<template><img class="user-avatar" :src="source" :alt="name ? `${name}的头像` : '用户头像'" @error="failed = true" /></template>
<style scoped>.user-avatar { display: block; width: 100%; height: 100%; object-fit: cover; border-radius: inherit; }</style>
