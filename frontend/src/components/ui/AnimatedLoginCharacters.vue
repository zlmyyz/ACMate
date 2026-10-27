<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import AnimatedCharacterEye from './AnimatedCharacterEye.vue'
import { siteBrand } from '@/constants/site'

const props = withDefaults(
  defineProps<{
    isUsernameFocused?: boolean
    isPasswordHidden?: boolean
    isPasswordRevealed?: boolean
  }>(),
  {
    isUsernameFocused: false,
    isPasswordHidden: false,
    isPasswordRevealed: false,
  },
)

const stage = ref<HTMLElement | null>(null)
const pointer = ref({ x: 0, y: 0, active: false })
let frame = 0
let pendingPoint: { x: number; y: number; active: boolean } | undefined
const isPeeking = ref(false)
const lookingAtEachOther = ref(false)
let peekTimer: ReturnType<typeof setTimeout> | undefined
let returnTimer: ReturnType<typeof setTimeout> | undefined
let glanceTimer: ReturnType<typeof setTimeout> | undefined

const characters = [
  {
    name: '紫色方块',
    left: 70,
    width: 180,
    height: 400,
    color: '#6c3ff5',
    radius: '10px 10px 0 0',
    eyeGap: 32,
    eyeSize: 18,
    pupilSize: 7,
    faceX: 45,
    faceY: 40,
    awayX: 20,
    awayY: 35,
  },
  {
    name: '深色方块',
    left: 240,
    width: 120,
    height: 310,
    color: '#2d2d2d',
    radius: '8px 8px 0 0',
    eyeGap: 24,
    eyeSize: 16,
    pupilSize: 6,
    faceX: 26,
    faceY: 32,
    awayX: 10,
    awayY: 28,
  },
  {
    name: '橘色圆顶',
    left: 0,
    width: 240,
    height: 200,
    color: '#ff9b6b',
    radius: '120px 120px 0 0',
    eyeGap: 32,
    eyeSize: 12,
    pupilSize: 12,
    faceX: 82,
    faceY: 90,
    awayX: 50,
    awayY: 85,
  },
  {
    name: '黄色圆顶',
    left: 310,
    width: 140,
    height: 230,
    color: '#e8d754',
    radius: '70px 70px 0 0',
    eyeGap: 24,
    eyeSize: 12,
    pupilSize: 12,
    faceX: 52,
    faceY: 40,
    awayX: 20,
    awayY: 35,
  },
]
type Character = (typeof characters)[number]
const clamp = (value: number, maximum: number) => Math.max(-maximum, Math.min(maximum, value))

function trackPointer(event: PointerEvent) {
  pendingPoint = {
    x: event.clientX,
    y: event.clientY,
    active: true,
  }
  if (frame) return
  frame = requestAnimationFrame(() => {
    if (pendingPoint) pointer.value = pendingPoint
    pendingPoint = undefined
    frame = 0
  })
}

function characterHeight(character: Character, index: number) {
  return index === 0 && !props.isPasswordRevealed && props.isUsernameFocused
    ? 440
    : character.height
}

function position(character: Character, index: number) {
  if (!stage.value?.offsetWidth || !pointer.value.active) return { x: 0, y: 0, skew: 0 }
  const bounds = stage.value.getBoundingClientRect()
  const scale = bounds.width / stage.value.offsetWidth
  const centerX = bounds.left + (character.left + character.width / 2) * scale
  const centerY = bounds.bottom - (26 + (characterHeight(character, index) * 2) / 3) * scale
  const dx = pointer.value.x - centerX
  const dy = pointer.value.y - centerY
  return { x: clamp(dx / 20, 15), y: clamp(dy / 30, 10), skew: clamp(-dx / 120, 6) }
}

function bodyPose(character: Character, index: number) {
  let skew = position(character, index).skew
  let shift = 0
  if (props.isPasswordRevealed) skew = 0
  else if (index === 0 && props.isUsernameFocused) {
    skew -= 12
    shift = 40
  } else if (index === 1 && lookingAtEachOther.value) {
    skew = skew * 1.5 + 10
    shift = 20
  } else if (index === 1 && (props.isUsernameFocused || props.isPasswordHidden)) skew *= 1.5
  return { transform: `skewX(${skew}deg) translateX(${shift}px)` }
}

function facePose(character: Character, index: number) {
  if (props.isPasswordRevealed) return { left: `${character.awayX}px`, top: `${character.awayY}px` }
  if (lookingAtEachOther.value && index < 2)
    return { left: `${index === 0 ? 55 : 32}px`, top: `${index === 0 ? 65 : 12}px` }
  const motion = position(character, index)
  return { left: `${character.faceX + motion.x}px`, top: `${character.faceY + motion.y}px` }
}

function forceLook(index: number) {
  if (props.isPasswordRevealed) {
    if (index === 0 && isPeeking.value) return { x: 4, y: 5 }
    return { x: index < 2 ? -4 : -5, y: -4 }
  }
  if (lookingAtEachOther.value && index < 2)
    return { x: index === 0 ? 3 : 0, y: index === 0 ? 4 : -4 }
  return null
}

function mouthPose(character: Character, index: number) {
  const motion = position(character, index)
  const x = props.isPasswordRevealed ? character.awayX : character.faceX
  const y = props.isPasswordRevealed ? character.awayY : character.faceY
  const eyesWidth = character.eyeSize * 2 + character.eyeGap
  return {
    left: `${x + motion.x + eyesWidth / 2 - 30}px`,
    top: `${y + motion.y + character.eyeSize + 22}px`,
  }
}

function schedulePeek() {
  peekTimer = setTimeout(
    () => {
      isPeeking.value = true
      returnTimer = setTimeout(() => {
        isPeeking.value = false
        schedulePeek()
      }, 800)
    },
    2000 + Math.random() * 3000,
  )
}

watch(
  () => props.isPasswordRevealed,
  (revealed) => {
    clearTimeout(peekTimer)
    clearTimeout(returnTimer)
    isPeeking.value = false
    if (revealed) schedulePeek()
  },
)

watch(
  () => props.isUsernameFocused,
  (focused) => {
    clearTimeout(glanceTimer)
    lookingAtEachOther.value = focused
    if (focused)
      glanceTimer = setTimeout(() => {
        lookingAtEachOther.value = false
      }, 800)
  },
)

const subtitle = `在${siteBrand.schoolShortName}，把每一道题变成下一次进步。`

const refreshPosition = () => {
  pointer.value = { ...pointer.value }
}
onMounted(() => {
  window.addEventListener('pointermove', trackPointer, { passive: true })
  window.addEventListener('resize', refreshPosition)
})

onUnmounted(() => {
  window.removeEventListener('pointermove', trackPointer)
  window.removeEventListener('resize', refreshPosition)
  if (frame) cancelAnimationFrame(frame)
  clearTimeout(peekTimer)
  clearTimeout(returnTimer)
  clearTimeout(glanceTimer)
})
</script>

<template>
  <aside class="mascot-panel" :aria-label="`${siteBrand.schoolName}${siteBrand.clubName}介绍`">
    <div class="panel-grain" aria-hidden="true"></div>
    <div class="panel-glow panel-glow-one" aria-hidden="true"></div>
    <div class="panel-glow panel-glow-two" aria-hidden="true"></div>

    <RouterLink class="panel-brand" to="/" :aria-label="`${siteBrand.clubName}首页`">
      <span class="brand-symbol" aria-hidden="true">&lt;/&gt;</span>
      <span>{{ siteBrand.clubName }}</span>
    </RouterLink>

    <div class="panel-message">
      <p class="panel-kicker">· 一起练习，一起成长</p>
      <h1>把热爱，写进<br />每一次 <span>AC</span></h1>
      <p class="panel-description">{{ subtitle }}</p>
    </div>

    <div ref="stage" class="mascot-stage">
      <div class="stage-orbit orbit-one" aria-hidden="true"></div>
      <div class="stage-orbit orbit-two" aria-hidden="true"></div>
      <div class="stage-spark spark-one" aria-hidden="true">✳</div>
      <div class="stage-spark spark-two" aria-hidden="true">✦</div>

      <div
        v-for="(character, index) in characters"
        :key="character.name"
        class="mascot"
        :class="[`mascot-${index + 1}`, { 'mascot-revealed': isPasswordRevealed }]"
        :aria-label="character.name"
        role="img"
        :style="{
          left: `${character.left}px`,
          width: `${character.width}px`,
          height: `${characterHeight(character, index)}px`,
          backgroundColor: character.color,
          borderRadius: character.radius,
          zIndex: index + 1,
          ...bodyPose(character, index),
        }"
      >
        <div
          class="mascot-eyes"
          :style="{ ...facePose(character, index), gap: `${character.eyeGap}px` }"
        >
          <AnimatedCharacterEye
            v-for="eye in 2"
            :key="eye"
            :pointer="pointer"
            :size="character.eyeSize"
            :pupil-size="character.pupilSize"
            :white="index < 2"
            :max-distance="index === 1 ? 4 : 5"
            :force-look="forceLook(index)"
            :blink-delay="index * 1.3"
          />
        </div>
        <div
          v-if="index === 3"
          class="mascot-mouth"
          :style="mouthPose(character, index)"
          aria-hidden="true"
        ></div>
      </div>
      <div class="mascot-floor" aria-hidden="true"></div>
    </div>

    <div class="panel-footer" aria-hidden="true">
      <span class="footer-dot"></span>
      <span>为每一次认真练习喝彩</span>
    </div>
  </aside>
</template>

<style scoped>
.mascot-panel {
  position: relative;
  display: flex;
  min-width: 0;
  min-height: 100vh;
  flex-direction: column;
  overflow: hidden;
  padding: clamp(32px, 5vw, 72px);
  color: #fff;
  background: linear-gradient(145deg, #192948 1%, #273766 50%, #4354a2 100%);
  isolation: isolate;
}

.panel-grain {
  position: absolute;
  z-index: -1;
  inset: 0;
  background-image: radial-gradient(rgba(255, 255, 255, 0.2) 0.65px, transparent 0.65px);
  background-size: 18px 18px;
  opacity: 0.12;
  pointer-events: none;
}

.panel-glow {
  position: absolute;
  z-index: -1;
  width: 26rem;
  aspect-ratio: 1;
  border-radius: 50%;
  background: rgba(152, 133, 255, 0.18);
  filter: blur(86px);
  pointer-events: none;
}

.panel-glow-one {
  top: 24%;
  right: -18%;
}
.panel-glow-two {
  bottom: -17%;
  left: -18%;
  background: rgba(82, 186, 223, 0.16);
}

.panel-brand {
  z-index: 1;
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: 11px;
  color: #fff;
  font-family: var(--font-headline);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.04em;
  text-decoration: none;
}

.brand-symbol {
  display: grid;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.21);
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.09);
  font-family: var(--font-mono);
  font-size: 14px;
}

.panel-message {
  position: relative;
  z-index: 2;
  width: min(100%, 470px);
  margin: clamp(24px, 3vh, 48px) auto 0;
}

.panel-kicker {
  margin: 0 0 17px;
  color: #c4ccf4;
  font-size: 13px;
  letter-spacing: 0.12em;
}

.panel-message h1 {
  margin: 0;
  color: #fff;
  font-family: var(--font-headline);
  font-size: clamp(34px, 3.15vw, 48px);
  font-weight: 700;
  line-height: 1.22;
  letter-spacing: -0.045em;
}

.panel-message h1 span {
  color: #f0d95d;
}

.panel-description {
  margin: 16px 0 0;
  color: rgba(243, 245, 255, 0.73);
  font-size: 15px;
  letter-spacing: 0.02em;
}

.mascot-stage {
  position: relative;
  width: min(100%, 470px);
  height: 466px;
  flex: 1 0 466px;
  align-self: center;
  margin: 8px auto 0;
}

.stage-orbit {
  position: absolute;
  left: 50%;
  bottom: 36px;
  width: 405px;
  height: 265px;
  border: 1px solid rgba(223, 225, 255, 0.12);
  border-radius: 50%;
  transform: translateX(-50%) rotate(-8deg);
}

.orbit-two {
  width: 345px;
  height: 216px;
  bottom: 50px;
  transform: translateX(-50%) rotate(12deg);
}

.mascot {
  position: absolute;
  bottom: 26px;
  transform-origin: bottom center;
  transition:
    transform 0.45s cubic-bezier(0.22, 0.8, 0.25, 1),
    height 0.55s ease;
  animation: mascot-arrive 0.75s both cubic-bezier(0.22, 0.8, 0.25, 1);
}

.mascot-1 {
  animation-delay: 0.12s;
}
.mascot-2 {
  animation-delay: 0.2s;
}
.mascot-3 {
  animation-delay: 0.28s;
}
.mascot-4 {
  animation-delay: 0.36s;
}

.mascot-eyes {
  position: absolute;
  display: flex;
  align-items: center;
  transition:
    left 0.32s ease-out,
    top 0.32s ease-out;
}

.mascot-mouth {
  position: absolute;
  width: 60px;
  height: 4px;
  border-radius: 999px;
  background: #2d2d2d;
  transition:
    left 0.32s ease-out,
    top 0.32s ease-out;
}

.mascot-floor {
  position: absolute;
  right: 0;
  bottom: 24px;
  left: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(228, 232, 255, 0.36), transparent);
}

.stage-spark {
  position: absolute;
  color: #f3dd68;
  animation: spark-drift 3.8s ease-in-out infinite;
}

.spark-one {
  top: 27%;
  right: 4%;
  font-size: 23px;
}
.spark-two {
  top: 48%;
  left: 2%;
  color: #aeb7ff;
  font-size: 16px;
  animation-delay: -1.7s;
}

.panel-footer {
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  color: rgba(241, 243, 255, 0.65);
  font-size: 12px;
  letter-spacing: 0.04em;
}

.footer-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #f0d95d;
}

@keyframes mascot-arrive {
  from {
    opacity: 0;
    translate: 0 24px;
  }
  to {
    opacity: 1;
    translate: 0 0;
  }
}

@keyframes spark-drift {
  0%,
  100% {
    translate: 0 0;
    rotate: 0deg;
    opacity: 0.7;
  }
  50% {
    translate: 0 -9px;
    rotate: 20deg;
    opacity: 1;
  }
}

@media (max-width: 1120px) {
  .mascot-panel {
    padding-right: 34px;
    padding-left: 34px;
  }
  .mascot-stage {
    transform: scale(0.84);
    transform-origin: center center;
    margin-right: -36px;
    margin-left: -36px;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
