<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getPendingReviews, getProblemReviews, getProblemReviewDetail, decideProblemReview, type ProblemReview } from '@/api/problems'
const props = defineProps<{ problemId?: number; pending?: boolean }>()
const auth=useAuthStore(), reviews=ref<ProblemReview[]>([]), error=ref(''), busy=ref<number|null>(null)
const notes=ref<Record<number,string>>({}), details=ref<Record<number,{original_content:string;proposed_content:string}>>({}), page=ref(1), pages=ref(0)
async function load(){error.value='';try{const result=props.pending?await getPendingReviews(page.value):await getProblemReviews(props.problemId!,page.value);reviews.value=result.records;pages.value=result.pages}catch{error.value='修改建议加载失败，请稍后重试'}}
async function loadDetail(id:number){if(details.value[id])return;try{details.value[id]=await getProblemReviewDetail(id)}catch{error.value='修改建议详情加载失败，请稍后重试'}}
function changePage(n:number){details.value={};page.value=n;void load()}
async function decide(id:number,action:'approve'|'reject'){
  busy.value=id;error.value=''
  try{await decideProblemReview(id,action,notes.value[id]||'');await load()}
  catch(e:unknown){error.value=(e as {response?:{data?:{message?:string}}}).response?.data?.message || '审核失败，请稍后重试'}
  finally{busy.value=null}
}
function formatted(content:string){try{return JSON.stringify(JSON.parse(content),null,2)}catch{return content}}
onMounted(load)
</script>
<template>
  <section class="reviews" aria-label="题目修改审核">
    <h2>{{ pending ? '待审核的题目修改' : '修改建议历史' }}</h2>
    <p v-if="error" role="alert">{{ error }} <button type="button" @click="load">重试</button></p>
    <p v-else-if="!reviews.length">暂无修改建议</p>
    <article v-for="review in reviews" :key="review.id" class="review">
      <p>建议 #{{ review.id }} · 题目 #{{ review.problem_id }} · {{ review.status === 'PENDING' ? '待审核' : review.status === 'APPROVED' ? '已通过' : '已驳回' }}</p>
      <p v-if="review.reviewer_user_id">审核人 #{{ review.reviewer_user_id }} · {{ review.review_time }} <span v-if="review.review_note">· {{ review.review_note }}</span></p>
      <details @toggle="($event.target as HTMLDetailsElement).open && loadDetail(review.id)"><summary>查看修改前后内容</summary><template v-if="details[review.id]"><h3>修改前</h3><pre>{{ formatted(details[review.id]!.original_content) }}</pre><h3>修改建议</h3><pre>{{ formatted(details[review.id]!.proposed_content) }}</pre></template><p v-else>正在加载详情…</p></details>
      <div v-if="auth.isAdmin && review.status === 'PENDING'" class="actions"><input v-model="notes[review.id]" maxlength="500" placeholder="审核意见（选填）" aria-label="审核意见"/><button :disabled="busy !== null" @click="decide(review.id,'approve')">审核通过</button><button :disabled="busy !== null" @click="decide(review.id,'reject')">驳回</button></div>
    </article>
    <div v-if="pages > 1" class="actions"><button :disabled="page <= 1" @click="changePage(page-1)">上一页</button><span>第 {{ page }} / {{ pages }} 页</span><button :disabled="page >= pages" @click="changePage(page+1)">下一页</button></div>
  </section>
</template>
<style scoped>
.reviews{padding:18px;margin:20px 0;border:1px solid var(--color-border-subtle);border-radius:var(--radius-lg);background:var(--color-surface-card);font-size:14px}.reviews h2{font-size:17px}.review{padding:12px 0;border-top:1px solid var(--color-border-subtle)}.review pre{white-space:pre-wrap;overflow-wrap:anywhere;max-height:320px;overflow:auto}.actions{display:flex;flex-wrap:wrap;gap:8px;margin-top:10px}.actions input{flex:1;min-width:180px}.actions button,.actions input{padding:8px;border:1px solid var(--color-border-subtle);border-radius:6px;background:var(--color-surface-container-low);color:var(--color-on-surface)}[role=alert]{color:var(--color-status-error)}
</style>
