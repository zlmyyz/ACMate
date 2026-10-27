<script setup lang="ts">
import { computed, ref, watch, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import CodeEditor from '@/components/editor/CodeEditor.vue'
import { practiceLanguages } from '@/constants/practice-languages'
import { useEditorSettings } from '@/composables/useEditorSettings'
import { getNowcoderAccount, getTrainingInfo, getOfficialRecords, getPublicSamples, getPracticeStatus, runPractice, syncOfficialRecords, type TrainingInfo, type NowcoderAccount, type OfficialRecord, type PublicSample, type PracticeResult } from '@/api/nowcoder-training'
const props = defineProps<{ problemId: number }>()
const auth = useAuthStore()
const languages = Object.fromEntries(Object.entries(practiceLanguages).map(([key,value])=>[key,value.label]))
const templates:Record<string,string> = Object.fromEntries(Object.entries(practiceLanguages).map(([key,value])=>[key,value.template]))
const {autocomplete,language,settingsMessage,setAutocomplete,setLanguage,retrySettings}=useEditorSettings()
type TestCase = { id: number; input: string; expected: string; compare: boolean; sampleIndex: number | null }
type CaseResult = { test: TestCase; result: PracticeResult | null; error: string; matches: boolean | null }
let nextCaseId = 0, runGeneration = 0
const newCase = (input='', expected='', compare=false, sampleIndex: number | null=null): TestCase => ({id: ++nextCaseId, input, expected, compare, sampleIndex})
const source = ref(''), cases = ref<TestCase[]>([]), activeCase = ref(0), caseResults = ref<CaseResult[]>([])
const currentCase = computed(() => cases.value[activeCase.value])
const info = ref<TrainingInfo | null>(null), account = ref<NowcoderAccount | null>(null)
const records = ref<OfficialRecord[]>([]), samples = ref<PublicSample[]>([])
const available = ref(false), running = ref(false), syncing = ref(false), loadingSamples = ref(false)
const error = ref(''), message = ref(''), awaitingReturn = ref(false)
const testTab = ref<'input' | 'result'>('input')
const normalize = (text: string) => text.replace(/\r\n?/g,'\n').split('\n').map(line => line.trimEnd()).join('\n').replace(/\n+$/,'')
const labels: Record<string,string> = { OK:'AC · 官方通过', WRONG_ANSWER:'WA · 答案错误', TIME_LIMIT_EXCEEDED:'TLE · 超时', MEMORY_LIMIT_EXCEEDED:'MLE · 内存超限', COMPILATION_ERROR:'CE · 编译错误', RUNTIME_ERROR:'RE · 运行错误', OUTPUT_LIMIT_EXCEEDED:'OLE · 输出超限', PRESENTATION_ERROR:'PE · 格式错误' }
const draftKey = () => `acmate:practice:${auth.user?.id ?? 'guest'}:${props.problemId}:${language.value}`
function loadDraft() { try { source.value = localStorage.getItem(draftKey()) ?? templates[language.value] ?? '' } catch { source.value = templates[language.value] ?? '' } }
watch(source, value => { try { localStorage.setItem(draftKey(), value) } catch { /* Editing remains available when storage is full. */ } })
watch(language, () => { loadDraft(); caseResults.value = [] })
watch(() => auth.user?.id, () => { loadDraft(); caseResults.value = [] })
watch(() => props.problemId, async () => {
  runGeneration++; running.value = false; loadDraft(); info.value = null; records.value = []; caseResults.value = []; samples.value = []; cases.value = []; activeCase.value = 0; testTab.value = 'input'; error.value = ''; awaitingReturn.value = false; loadingSamples.value = true
  const id = props.problemId
  const response = await Promise.allSettled([getTrainingInfo(id), getNowcoderAccount(), getOfficialRecords(id), getPracticeStatus(), getPublicSamples(id)])
  if (id !== props.problemId) return
  if (response[0].status === 'fulfilled') info.value = response[0].value
  else error.value = '训练信息加载失败，请刷新页面重试'
  if (response[1].status === 'fulfilled') account.value = response[1].value
  if (response[2].status === 'fulfilled') records.value = response[2].value
  if (response[3].status === 'fulfilled') available.value = response[3].value.available
  if (response[4].status === 'fulfilled') { samples.value = response[4].value; cases.value = samples.value.length ? samples.value.map((sample,index) => newCase(sample.input,sample.output,true,index)) : [newCase()] }
  else { cases.value = [newCase()]; error.value = '公开样例自动读取失败，可点击重试' }
  loadingSamples.value = false
}, { immediate: true })
function errorText(e: unknown) { return (e as {response?:{data?:{message?:string}}}).response?.data?.message || '操作失败，请稍后重试' }
async function readSamples() {
  loadingSamples.value = true; error.value = ''
  try {
    samples.value = await getPublicSamples(props.problemId)
    if (samples.value.length) {
      const custom = cases.value.filter(item => item.sampleIndex === null && (item.input || item.expected || item.compare))
      cases.value = [...samples.value.map((sample,index) => newCase(sample.input,sample.output,true,index)), ...custom]
      activeCase.value = 0; caseResults.value = []
    } else message.value = '暂无公开样例，可以自行填写输入进行自测'
  } catch(e) { error.value = errorText(e) }
  finally { loadingSamples.value = false }
}
function selectCase(index: number) { activeCase.value = index }
function editCase() { if (currentCase.value) { currentCase.value.sampleIndex = null; caseResults.value = [] } }
function addCase() { cases.value.push(newCase()); activeCase.value = cases.value.length - 1; caseResults.value = [] }
function removeCase() { if (cases.value.length === 1) cases.value = [newCase()]; else cases.value.splice(activeCase.value,1); activeCase.value = Math.min(activeCase.value,cases.value.length-1); caseResults.value = [] }
async function run() {
  if (!available.value || running.value || !source.value.trim() || !cases.value.length) return
  const generation = ++runGeneration, problemId = props.problemId, code = source.value, selectedLanguage = language.value
  const pending = cases.value.map(item => ({...item}))
  running.value = true; error.value = ''; caseResults.value = []; testTab.value = 'result'
  for (const test of pending) {
    try {
      const result = await runPractice(problemId,test.sampleIndex ?? 0,selectedLanguage,code,test.sampleIndex === null ? test.input : undefined)
      if (generation !== runGeneration) return
      caseResults.value.push({test,result,error:'',matches:result.status === 'FINISHED' && test.compare ? normalize(result.stdout) === normalize(test.expected) : null})
    } catch(e) {
      if (generation !== runGeneration) return
      caseResults.value.push({test,result:null,error:errorText(e),matches:null})
    }
  }
  if (generation === runGeneration) running.value = false
}
function saveDraftNow() {
  try { localStorage.setItem(draftKey(), source.value); message.value = '代码已保存到当前浏览器'; return true }
  catch { error.value = '浏览器存储不可用，请复制代码保存'; return false }
}
function openOfficial(event: MouseEvent) {
  if (!saveDraftNow()) { event.preventDefault(); return }
  awaitingReturn.value = true; message.value = '代码已保存。请在牛客粘贴代码并选择对应语言提交。返回本页后，将尝试同步可公开读取的正式结果。'
  if (navigator.clipboard) void navigator.clipboard.writeText(source.value).catch(() => { message.value = '代码已保存，浏览器未允许复制，请手动复制代码，再到牛客提交。' })
  else message.value = '代码已保存，请手动复制代码，再到牛客提交。'
}
async function sync() {
  if (syncing.value || !info.value?.syncSupported) return
  syncing.value = true; error.value = ''
  try { const data = await syncOfficialRecords(props.problemId); records.value = await getOfficialRecords(props.problemId); message.value = `${data.message}，新增 ${data.inserted} 条`; awaitingReturn.value = false }
  catch(e) { error.value = errorText(e) }
  finally { syncing.value = false }
}
async function onReturn() {
  if (document.visibilityState === 'hidden' || !awaitingReturn.value || !info.value?.syncSupported) return
  try { account.value = await getNowcoderAccount(); if (account.value.verifyStatus === 1) await sync() }
  catch { /* Explicit sync still remains available. */ }
}
onMounted(() => { window.addEventListener('focus',onReturn); document.addEventListener('visibilitychange',onReturn) })
onUnmounted(() => { window.removeEventListener('focus',onReturn); document.removeEventListener('visibilitychange',onReturn) })
</script>
<template>
<section class="practice-panel" aria-label="牛客题目训练">
  <div class="practice-heading"><div><h2>编程训练</h2><p>在本站编写代码，前往牛客完成正式提交。</p></div><label>语言 <select :value="language" @change="setLanguage(($event.target as HTMLSelectElement).value)" aria-label="编程语言" :disabled="running"><option v-for="(name,key) in languages" :key="key" :value="key">{{ name }}</option></select></label></div>
  <CodeEditor v-model="source" :language="language" :disabled="running" :autocomplete="autocomplete" @update:autocomplete="setAutocomplete" @run="run" @save="saveDraftNow">
    <template #footer>
      <div class="self-test-panel">
        <div class="test-actionbar">
          <div class="test-tabs" role="tablist" aria-label="自测面板">
            <button id="practice-result-tab" type="button" role="tab" :aria-selected="testTab === 'result'" aria-controls="practice-result-panel" @click="testTab = 'result'">运行结果</button>
            <button id="practice-input-tab" type="button" role="tab" :aria-selected="testTab === 'input'" aria-controls="practice-input-panel" @click="testTab = 'input'">自测输入</button>
          </div>
          <button class="run-button" type="button" :disabled="!available || running || !source.trim()" @click="run"><span aria-hidden="true">▷</span> {{ running ? '运行中…' : '自测运行' }}<kbd>Ctrl + '</kbd></button>
          <a v-if="info" class="official-button" :href="info.officialUrl" target="_blank" rel="noopener noreferrer" @click="openOfficial">保存并跳转（牛客）<span aria-hidden="true"> ↗</span></a>
          <button v-else type="button" class="official-button" disabled>保存并跳转（牛客）</button>
        </div>
        <div v-show="testTab === 'input'" id="practice-input-panel" class="test-body" role="tabpanel" aria-labelledby="practice-input-tab">
          <div class="sample-toolbar"><span v-if="loadingSamples">正在自动读取公开样例…</span><template v-else><span class="sample-label">测试用例</span><button v-for="(item,index) in cases" :key="item.id" type="button" :class="{ 'sample-active': activeCase === index }" :aria-pressed="activeCase === index" :disabled="running" @click="selectCase(index)">用例 {{ index + 1 }}</button><button type="button" :disabled="running" @click="addCase">＋ 添加用例</button><button v-if="!samples.length" type="button" :disabled="running" @click="readSamples">重试读取样例</button></template></div>
          <template v-if="currentCase"><div class="input-mode"><span>{{ currentCase.sampleIndex !== null ? '公开样例，可直接修改为自定义用例' : '自定义用例' }}</span><button type="button" :disabled="running" @click="removeCase">删除用例</button></div>
          <div class="io-grid"><label>自测输入<textarea v-model="currentCase.input" aria-label="自测输入" placeholder="填写程序的标准输入，可留空" :disabled="running" maxlength="65536" @input="editCase" /></label><label>预期输出（选填）<textarea v-model="currentCase.expected" aria-label="预期输出" placeholder="选填，用于对比实际输出" :disabled="running" maxlength="65536" @input="currentCase.compare = true; editCase()" /></label></div>
          <label class="compare-output"><input v-model="currentCase.compare" type="checkbox" :disabled="running" @change="editCase"/>对比预期输出</label></template>
        </div>
        <div v-show="testTab === 'result'" id="practice-result-panel" class="test-body" role="tabpanel" aria-labelledby="practice-result-tab" aria-live="polite">
          <p v-if="running" class="muted">正在运行用例 {{ caseResults.length + 1 }} / {{ cases.length }}…</p>
          <div v-for="(item,index) in caseResults" :key="item.test.id" class="run-result case-result">
            <strong>用例 {{ index + 1 }} · <span :class="item.matches === true ? 'sample-pass' : item.matches === false || item.error || item.result?.status !== 'FINISHED' ? 'sample-fail' : ''">{{ item.error ? '运行失败' : item.result?.status !== 'FINISHED' ? item.result?.status : item.matches === true ? '通过' : item.matches === false ? '输出不一致' : '运行完成（未对比）' }}</span></strong>
            <span v-if="item.result"> {{ item.result.timeMs }} ms · {{ item.result.memoryKb }} KB</span>
            <p v-if="item.error" class="sample-fail">{{ item.error }}</p>
            <div v-if="item.result" class="result-grid"><div><h3>实际输出</h3><pre :class="{'sample-fail': item.matches === false}">{{ item.result.stdout || '（无输出）' }}</pre></div><div v-if="item.test.compare"><h3>预期输出</h3><pre :class="{'sample-pass': item.matches === false}">{{ item.test.expected || '（空输出）' }}</pre></div></div>
            <template v-if="item.result?.stderr"><h3>错误信息</h3><pre class="sample-fail">{{ item.result.stderr }}</pre></template>
          </div>
          <p v-if="!running && !caseResults.length" class="muted">点击“自测运行”，依次测试所有用例。</p>
        </div>
        <p class="test-hint">{{ !available ? '本站运行环境尚未启用，可先到牛客运行。' : '将依次运行全部公开样例和自定义用例；自测结果不作为正式 AC。' }}</p>
      </div>
    </template>
  </CodeEditor>
  <div class="editor-note">代码按账号、题目和语言自动保存在当前浏览器。<span v-if="settingsMessage" role="status"> {{ settingsMessage }} <button v-if="settingsMessage.includes('同步失败')" type="button" @click="retrySettings">重试同步设置</button></span></div>
  <div class="official-section"><div class="official-actions"><button type="button" :disabled="!info?.syncSupported || account?.verifyStatus !== 1 || syncing" @click="sync">{{ syncing ? '同步中…' : '同步官方结果' }}</button><RouterLink to="/settings/oj-account">{{ account?.hasAccount ? '管理牛客绑定' : '绑定牛客账号' }}</RouterLink></div><p>{{ info?.syncNotice }}</p><p v-if="account?.hasAccount && account.verifyStatus !== 1">牛客 UID {{ account.uid }} 尚未通过审核，审核后才能同步。</p></div>
  <p v-if="message" role="status">{{ message }}</p><p v-if="error" role="alert">{{ error }}</p>
  <div class="official-records"><h3>此题最近 10 次官方提交</h3><p v-if="!records.length" class="muted">暂无已同步的正式提交记录，本站样例运行不会计入此处。</p><div v-for="record in records" :key="record.remoteId" class="record-row"><a :href="`https://ac.nowcoder.com/acm/contest/view-submission?submissionId=${record.remoteId}`" target="_blank" rel="noopener noreferrer">#{{ record.remoteId }} ↗</a><strong :class="record.verdict === 'OK' ? 'sample-pass' : 'sample-fail'">{{ labels[record.verdict] || record.verdict }}</strong><span>{{ record.language }}</span><time>{{ new Date(record.submittedTime).toLocaleString('zh-CN') }}</time></div></div>
</section>
</template>
<style scoped>
.practice-panel{margin-top:24px;padding:24px;background:var(--color-surface-card);border:1px solid var(--color-border-subtle);border-radius:var(--radius-lg)}.practice-heading{display:flex;justify-content:space-between;align-items:center;gap:16px;margin-bottom:18px}.practice-heading h2{font-size:var(--text-headline-sm);margin:0 0 6px}.practice-heading p,.editor-note,.muted,.official-section p,.test-hint{color:var(--color-on-surface-variant);font-size:13px;line-height:1.6}.practice-heading select{margin-left:8px;padding:6px 10px;border-radius:6px;background:var(--color-surface-container-low);color:var(--color-on-surface);border:1px solid var(--color-border-subtle)}textarea{background:var(--color-surface-container-lowest);color:var(--color-on-surface);border:1px solid var(--color-border-subtle);border-radius:8px;resize:vertical;font-family:var(--font-mono),monospace;padding:14px;line-height:1.7;box-sizing:border-box}.editor-note{margin-top:6px}.self-test-panel .sample-toolbar .sample-active{border-color:var(--color-primary);color:var(--color-primary);background:var(--color-surface-container-low)}.sample-label{font-size:13px;color:var(--color-on-surface-variant)}.sample-toolbar,.official-actions{display:flex;gap:10px;align-items:center;flex-wrap:wrap;margin:18px 0 12px}button,.official-button{padding:9px 13px;border-radius:7px;background:var(--color-surface-container-low);color:var(--color-on-surface);border:1px solid var(--color-border-subtle);font:inherit;font-size:13px}.official-button{background:var(--color-primary);color:var(--color-on-primary);border-color:transparent}button:disabled{opacity:.5;cursor:not-allowed}.io-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}.io-grid label{display:grid;gap:8px;font-size:13px}.io-grid textarea{width:100%;min-height:120px}.official-section{margin-top:24px;padding-top:6px;border-top:1px solid var(--color-border-subtle)}.official-actions a:not(.official-button){color:var(--color-primary);font-size:13px}.run-result{font-size:13px}.run-result>span{color:var(--color-on-surface-variant)}.run-result h3,.official-records h3{font-size:14px;margin:14px 0 10px}.run-result pre{white-space:pre-wrap;overflow-wrap:anywhere;font:13px/1.7 var(--font-mono),monospace}.sample-pass{color:var(--color-status-success)}.sample-fail,[role=alert]{color:var(--color-status-error)}[role=status]{font-size:13px;line-height:1.7}.record-row{display:flex;gap:16px;align-items:center;padding:13px 0;border-bottom:1px solid var(--color-border-subtle);font-size:13px;flex-wrap:wrap}.record-row time{margin-left:auto;color:var(--color-on-surface-variant)}.record-row a{color:var(--color-primary)}@media(max-width:650px){.practice-panel{padding:16px}.practice-heading{align-items:flex-start;flex-direction:column}.io-grid{grid-template-columns:1fr}.record-row time{margin-left:0}}

.self-test-panel{color:var(--color-on-surface);background:var(--color-surface-card);border-top:1px solid var(--color-border-subtle)}
.test-actionbar{display:flex;align-items:center;gap:10px;flex-wrap:wrap;padding:10px 12px;border-bottom:1px solid var(--color-border-subtle)}
.test-tabs{display:flex;align-items:center;gap:4px}
.test-tabs button{background:transparent;border-color:transparent;color:var(--color-on-surface-variant)}
.test-tabs button[aria-selected=true]{color:var(--color-primary);background:var(--color-surface-container-low)}
.test-actionbar .run-button{display:inline-flex;align-items:center;gap:6px;color:var(--color-primary);background:transparent;border-color:var(--color-primary)}
.test-actionbar kbd{font:11px/1.4 var(--font-mono),monospace;opacity:.75}
.test-actionbar .official-button{margin-left:auto;text-decoration:none;white-space:nowrap}
.test-body{padding:14px;min-height:180px;max-height:360px;overflow:auto}
.test-body .sample-toolbar{margin:0 0 14px;font-size:13px}
.test-body .sample-toolbar button{padding:5px 10px}
.test-hint{margin:0;padding:0 14px 12px}
button:focus-visible,.official-button:focus-visible{outline:2px solid var(--color-primary);outline-offset:2px}
@media(max-width:650px){.test-actionbar{gap:8px}.test-actionbar .official-button{margin-left:0}.test-actionbar kbd{display:none}.test-body{max-height:300px}}
.case-result{padding:12px 0;border-bottom:1px solid var(--color-border-subtle)}.result-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}.result-grid pre{background:var(--color-surface-container-low);padding:10px;border-radius:6px}@media(max-width:650px){.result-grid{grid-template-columns:1fr}}
.input-mode{display:flex;justify-content:space-between;align-items:center;gap:10px;margin-bottom:12px;font-size:12px;color:var(--color-on-surface-variant)}.input-mode button{padding:4px 8px}.compare-output{display:flex;align-items:center;gap:6px;font-size:12px;margin-top:12px;color:var(--color-on-surface-variant)}
</style>
