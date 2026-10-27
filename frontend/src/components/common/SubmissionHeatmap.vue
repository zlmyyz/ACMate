<script setup lang="ts">
import { computed, ref, nextTick, onMounted, onUnmounted } from 'vue'
import type { SubmissionActivity } from '@/api/profile-social'
const props = defineProps<{ activity: SubmissionActivity }>()
type Day = { date: string; count: number }
const streak = computed(() => {
  const days = [...props.activity.days].sort((a, b) => a.date.localeCompare(b.date))
  let index = days.length - 1
  if (days[index]?.count === 0) index--
  let count = 0
  while (index >= 0 && days[index]!.count > 0) { count++; index-- }
  return count
})
const months = computed(() => {
  const groups = new Map<string, Day[]>()
  for (const day of [...props.activity.days].sort((a, b) => a.date.localeCompare(b.date))) {
    const key = day.date.slice(0, 7)
    const group = groups.get(key) ?? []
    group.push(day); groups.set(key, group)
  }
  return [...groups].map(([key, days]) => {
    const [year, month] = key.split('-').map(Number)
    const first = new Date(days[0]!.date + 'T00:00:00Z')
    const leading = (first.getUTCDay() + 6) % 7
    const weeks = Math.ceil((leading + days.length) / 7)
    const cells: (Day | null)[] = Array.from({ length: weeks * 7 }, (_, index) => days[index - leading] ?? null)
    return { key, label: year + '年' + month + '月', caption: month + '月', weeks, cells }
  })
})
const hovered = ref<Day | null>(null)
const tooltip = ref<HTMLElement | null>(null)
const tooltipPosition = ref({ left: '0px', top: '0px' })
let tooltipTimer: ReturnType<typeof setTimeout> | undefined
function level(count: number) { return count === 0 ? 0 : count <= 2 ? 1 : count <= 5 ? 2 : count <= 9 ? 3 : 4 }
function showTooltip(day: Day, event: MouseEvent | FocusEvent) {
  hideTooltip()
  const element = event.currentTarget as HTMLElement
  const bounds = element.getBoundingClientRect()
  tooltipTimer = setTimeout(() => {
  tooltipTimer = undefined
  hovered.value = day
  void nextTick(() => {
    if (hovered.value?.date !== day.date) return
    const size = tooltip.value?.getBoundingClientRect()
    const width = size?.width || 164
    const height = size?.height || 32
    const left = Math.max(12, Math.min(bounds.left + bounds.width / 2 - width / 2, window.innerWidth - width - 12))
    const top = bounds.top >= height + 12 ? bounds.top - height - 8 : bounds.bottom + 8
    tooltipPosition.value = { left: left + 'px', top: Math.max(8, Math.min(top, window.innerHeight - height - 8)) + 'px' }
  })
  }, 400)
}
function hideTooltip() { clearTimeout(tooltipTimer); tooltipTimer = undefined; hovered.value = null }
onMounted(() => {
  window.addEventListener('scroll', hideTooltip, true)
  window.addEventListener('resize', hideTooltip)
})
onUnmounted(() => {
  hideTooltip()
  window.removeEventListener('scroll', hideTooltip, true)
  window.removeEventListener('resize', hideTooltip)
})
</script>
<template>
  <div class="activity-summary"><span class="submission-total">过去一年共提交 <strong>{{ activity.submissions }}</strong> 次</span><div class="activity-details"><span>累计提交天数：<strong>{{ activity.activeDays }}</strong></span><span>连续提交：<strong>{{ streak }}</strong></span></div></div>
  <div class="monthly-heatmaps" role="group" aria-label="按月份展示的每日提交热力图">
    <section v-for="month in months" :key="month.key" class="month-heatmap" :aria-label="month.label" :style="{ flexGrow: month.weeks }">
      <div class="month-grid" :style="{ gridTemplateColumns: `repeat(${month.weeks}, minmax(0, 1fr))` }">
        <template v-for="(cell, index) in month.cells" :key="cell?.date ?? index">
          <span v-if="!cell" class="day-padding" aria-hidden="true"></span>
          <button v-else type="button" class="day" :class="'level-' + level(cell.count)" :aria-label="cell.date + '：' + cell.count + ' 次提交'" @mouseenter="showTooltip(cell, $event)" @mouseleave="hideTooltip" @focus="showTooltip(cell, $event)" @blur="hideTooltip"></button>
        </template>
      </div>
      <div class="month-caption" :class="{ 'caption-hidden': month.weeks < 3 }" :title="month.label">{{ month.caption }}</div>
    </section>
  </div>
  <div class="heatmap-legend"><span>少</span><span v-for="n in 5" :key="n" class="legend-day" :class="'level-' + (n - 1)"></span><span>多</span></div>
  <p v-if="activity.submissions === 0" class="heatmap-empty">暂无提交数据，OJ 数据接入后将在这里展示每日提交分布。</p>
  <Teleport to="body"><div v-if="hovered" ref="tooltip" class="heatmap-tooltip" role="tooltip" :style="tooltipPosition">{{ hovered.count }} 个提交，{{ hovered.date }}</div></Teleport>
</template>
<style scoped>
.activity-summary { display: flex; justify-content: space-between; align-items: baseline; flex-wrap: wrap; gap: 12px 24px; margin-bottom: 28px; color: var(--color-on-surface-variant); font-size: 14px; }
.activity-summary strong { color: var(--color-on-surface); font-weight: 600; }
.submission-total { font-size: 17px; }.submission-total strong { font-size: 23px; }.activity-details { display: flex; flex-wrap: wrap; gap: 8px 20px; font-size: 12px; }
.monthly-heatmaps { display: flex; align-items: start; gap: clamp(3px,.8vw,12px); width: 100%; min-width: 0; }
.month-heatmap { flex-basis: 0; flex-shrink: 1; min-width: 0; }
.month-grid { display: grid; grid-auto-flow: column; grid-template-rows: repeat(7,minmax(0,1fr)); gap: clamp(1px,.2vw,3px); }
.month-caption { margin-top: 10px; text-align: center; color: var(--color-on-surface-variant); font-size: clamp(7px,.9vw,12px); white-space: nowrap; }.caption-hidden { visibility: hidden; }
.day,.day-padding { display: block; width: 100%; min-width: 0; aspect-ratio: 1; border-radius: 2px; }
.day { padding: 0; border: 0; cursor: default; color: #285038; font-family: inherit; }
.day-unavailable { color: var(--color-on-surface-variant); opacity: .32; }
.legend-day { display: block; width: 13px; height: 13px; border-radius: 3px; }
.level-0 { background: #edf0f3; }.level-1 { background: #c1e5d0; }.level-2 { background: #83c99e; }.level-3 { background: #42a871; }.level-4 { background: #227b50; }
.day.level-0 { color: #65716b; }.day.level-4 { color: #fff; }
.day:hover,.day:focus-visible { outline: 2px solid var(--color-primary-container); outline-offset: 1px; }
.heatmap-legend { display: flex; gap: 5px; justify-content: flex-end; align-items: center; margin-top: 18px; color: var(--color-on-surface-variant); font-size: 11px; }
.heatmap-empty { margin: 18px 0 0; color: var(--color-on-surface-variant); line-height: 1.7; font-size: 13px; }
.heatmap-tooltip { position: fixed; z-index: 2000; width: max-content; max-width: calc(100vw - 24px); box-sizing: border-box; padding: 7px 10px; border: 1px solid rgb(255 255 255 / 12%); border-radius: 7px; background: #333; color: #f4f4f4; font-size: 12px; line-height: 1.4; white-space: nowrap; pointer-events: none; box-shadow: 0 3px 10px rgb(0 0 0 / 12%); }
</style>

