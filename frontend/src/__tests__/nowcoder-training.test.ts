import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import Panel from '@/components/problem/NowcoderPracticePanel.vue'
import Detail from '@/views/ProblemDetailView.vue'
import { extractDataConstraints } from '@/utils/problem-constraints'
vi.mock('@/components/editor/CodeEditor.vue',()=>({default:{props:['modelValue','language','disabled'],emits:['update:modelValue','run','save'],template:`<div><textarea aria-label="代码编辑器" :value="modelValue" @input="$emit('update:modelValue', $event.target.value)" /><slot name="footer" /></div>`}}))
const m=vi.hoisted(()=>({account:vi.fn(),info:vi.fn(),records:vi.fn(),samples:vi.fn(),status:vi.fn(),run:vi.fn(),sync:vi.fn(),detail:vi.fn()}))
vi.mock('@/api/nowcoder-training',()=>({getNowcoderAccount:m.account,getTrainingInfo:m.info,getOfficialRecords:m.records,getPublicSamples:m.samples,getPracticeStatus:m.status,runPractice:m.run,syncOfficialRecords:m.sync}))
vi.mock('@/api/problems',()=>({getProblemDetail:m.detail,deactivateProblem:vi.fn(),restoreProblem:vi.fn(),adminDeactivateProblem:vi.fn(),adminRestoreProblem:vi.fn()}))
vi.mock('@/api/editor-settings',()=>({getEditorSettings:vi.fn().mockResolvedValue({autocomplete:true}),saveEditorSettings:vi.fn().mockResolvedValue({autocomplete:true})}))
const url='https://ac.nowcoder.com/acm/contest/19304/A'
beforeEach(()=>{vi.clearAllMocks();localStorage.clear();m.account.mockResolvedValue({hasAccount:true,uid:'123',verifyStatus:1});m.info.mockResolvedValue({officialUrl:url,syncSupported:true,syncNotice:'公开记录'});m.records.mockResolvedValue([]);m.samples.mockResolvedValue([{input:'1 2',output:'3'}]);m.status.mockResolvedValue({available:false});m.sync.mockResolvedValue({inserted:1,message:'同步成功'})})
async function render(detail=false){
 const pinia=createPinia();useAuthStore(pinia).user={id:1,username:'member',nickname:'成员',email:null,avatarUrl:null,bio:null,admin:false}
 const router=createRouter({history:createMemoryHistory(),routes:['/problems/:id','/settings/oj-account','/users/:id'].map(path=>({path,component:{template:'<div />'}}))});await router.push('/problems/1')
 const options={global:{plugins:[pinia,router]}}
 const wrapper=detail?mount(Detail,options):mount(Panel,{...options,props:{problemId:1}});await flushPromises();return wrapper
}
describe('牛客训练',()=>{
 it('保留草稿，读取公开样例，未部署时不运行代码',async()=>{
  const w=await render();expect(w.get('.run-button').attributes('disabled')).toBeDefined();expect(w.get('.official-button').attributes('href')).toBe(url)
  await w.get('[aria-label="代码编辑器"]').setValue('print(1)');expect(localStorage.getItem('acmate:practice:1:1:CPP17')).toBe('print(1)')
  expect(m.samples).toHaveBeenCalledWith(1);expect((w.get('[aria-label="自测输入"]').element as HTMLTextAreaElement).value).toBe('1 2');expect(w.get('[aria-label="自测输入"]').attributes('readonly')).toBeUndefined();expect(m.run).not.toHaveBeenCalled();expect(m.sync).not.toHaveBeenCalled();w.unmount()
 })
 it('样例通过不变成正式 AC，官方结果需单独同步',async()=>{
  m.status.mockResolvedValue({available:true});m.run.mockResolvedValue({status:'FINISHED',stdout:'3\n',stderr:'',timeMs:1,memoryKb:100})
  const w=await render();await w.get('.run-button').trigger('click');await flushPromises();expect(w.get('.run-result').text()).toContain('用例 1 · 通过');expect(m.sync).not.toHaveBeenCalled();expect(m.run).toHaveBeenCalledWith(1,0,'CPP17',expect.any(String),undefined)
  m.records.mockResolvedValue([{remoteId:'99',verdict:'OK',language:'C++',submittedTime:'2026-10-26T12:00:00'}]);await w.get('.official-actions button').trigger('click');await flushPromises();expect(m.sync).toHaveBeenCalledWith(1);expect(w.get('.record-row').text()).toContain('官方通过');w.unmount()
 })
 it('底部切换样例与结果，保存代码后跳转牛客',async()=>{
  m.status.mockResolvedValue({available:true});m.samples.mockResolvedValue([{input:'1 2',output:'3'},{input:'3 4',output:'7'}])
  m.run.mockResolvedValue({status:'FINISHED',stdout:'7',stderr:'',timeMs:1,memoryKb:100})
  const writeText=vi.fn().mockResolvedValue(undefined);Object.defineProperty(navigator,'clipboard',{value:{writeText},configurable:true})
  const w=await render();expect(w.get('.self-test-panel .official-button').text()).toContain('保存并跳转（牛客）')
  await w.get('.sample-toolbar button:nth-of-type(2)').trigger('click');expect((w.get('[aria-label="自测输入"]').element as HTMLTextAreaElement).value).toBe('3 4')
  await w.get('[aria-label="代码编辑器"]').setValue('test source')
  await w.get('.run-button').trigger('click');await flushPromises()
  expect(w.get('#practice-result-tab').attributes('aria-selected')).toBe('true');expect(m.run).toHaveBeenCalledWith(1,1,'CPP17','test source',undefined)
  await w.get('#practice-input-tab').trigger('click');expect(w.get('#practice-input-tab').attributes('aria-selected')).toBe('true')
  localStorage.removeItem('acmate:practice:1:1:CPP17')
  await w.get('.official-button').trigger('click');await flushPromises()
  expect(localStorage.getItem('acmate:practice:1:1:CPP17')).toBe('test source');expect(writeText).toHaveBeenCalledWith('test source')
  expect(w.get('.official-button').attributes('target')).toBe('_blank');w.unmount()
 })
 it('允许编辑自测输入与预期输出，没有公开样例也能运行',async()=>{
  m.status.mockResolvedValue({available:true});m.samples.mockResolvedValue([]);m.run.mockResolvedValue({status:'FINISHED',stdout:'12',stderr:'',timeMs:1,memoryKb:100})
  const w=await render();expect(w.get('.run-button').attributes('disabled')).toBeUndefined()
  await w.get('[aria-label="编程语言"]').setValue('GO');await w.get('[aria-label="自测输入"]').setValue('5 7\n');await w.get('[aria-label="预期输出"]').setValue('12')
  await w.get('.run-button').trigger('click');await flushPromises();expect(m.run).toHaveBeenCalledWith(1,0,'GO',expect.stringContaining('package main'),'5 7\n');expect(w.get('.run-result').text()).toContain('用例 1 · 通过');expect(m.sync).not.toHaveBeenCalled();w.unmount()
 })
 it('依次运行全部用例，并显示错误用例与期望结果，允许修改和添加用例',async()=>{
  m.status.mockResolvedValue({available:true});m.samples.mockResolvedValue([{input:'1',output:'2'},{input:'3',output:'4'}])
  m.run.mockResolvedValueOnce({status:'FINISHED',stdout:'2',stderr:'',timeMs:1,memoryKb:10}).mockResolvedValueOnce({status:'FINISHED',stdout:'5',stderr:'',timeMs:1,memoryKb:10}).mockResolvedValueOnce({status:'FINISHED',stdout:'8',stderr:'',timeMs:1,memoryKb:10})
  const w=await render();await w.get('.sample-toolbar button:nth-of-type(2)').trigger('click');await w.get('[aria-label="自测输入"]').setValue('9');await w.get('[aria-label="预期输出"]').setValue('4')
  await w.get('.sample-toolbar button:nth-of-type(3)').trigger('click');await w.get('[aria-label="自测输入"]').setValue('7');await w.get('[aria-label="预期输出"]').setValue('8')
  await w.get('.run-button').trigger('click');await flushPromises()
  expect(m.run).toHaveBeenCalledTimes(3);expect(m.run).toHaveBeenNthCalledWith(1,1,0,'CPP17',expect.any(String),undefined);expect(m.run).toHaveBeenNthCalledWith(2,1,0,'CPP17',expect.any(String),'9')
  const results=w.findAll('.case-result');expect(results[0]!.text()).toContain('通过');expect(results[1]!.text()).toContain('输出不一致');expect(results[1]!.get('pre.sample-fail').text()).toBe('5');expect(results[1]!.get('pre.sample-pass').text()).toBe('4');expect(results[2]!.text()).toContain('通过');w.unmount()
 })
 it('左侧题面重复提示范围，右侧为代码编辑器',async()=>{
  m.detail.mockResolvedValue({id:1,title:'题目',platform:'NOWCODER',sourceUrl:url,active:true,creatorUserId:2,contentMd:'## 题目描述\n\n给定一个数组，求最大值。数据范围：1 ≤ n ≤ 100000。',createTime:'2026-10-26',updateTime:'2026-10-26'})
  const w=await render(true);expect(w.find('.problem-workspace.with-editor').exists()).toBe(true);expect(w.get('.statement-pane .data-constraints').text()).toContain('1 ≤ n ≤ 100000');expect(w.get('.data-constraints').text()).not.toContain('给定一个数组');expect(w.get('.problem-statement').text()).toContain('给定一个数组，求最大值');expect(w.find('.editor-pane [aria-label="代码编辑器"]').exists()).toBe(true);w.unmount()
 })
 it('排除样例和图片地址，保留原题公式范围',()=>{
  expect(extractDataConstraints('正文\n\n![图](<https://example.com/123.png>)\n\n## 示例 1\n\n1 < 2')).toEqual([])
  const text='输入 n（![1 \\le n \\le 10^5](<https://www.nowcoder.com/equation?tex=n>)）。';expect(extractDataConstraints(text)).toEqual(['1 ≤ n ≤ 10^5'])
 })
})
