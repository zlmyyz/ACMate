import { beforeEach, describe, it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ProblemForm from '@/components/problem/ProblemForm.vue'
const mocks=vi.hoisted(()=>({read:vi.fn()}))
vi.mock('@/api/problems',()=>({importNowcoderProblem:mocks.read}))
const props={allowImport:true,draftKey:'test-nowcoder-draft',submitting:false,errorMsg:''}
const data={platform:'NOWCODER',externalProblemKey:'NC123',title:'测试题',sourceUrl:'https://ac.nowcoder.com/acm/problem/123',difficulty:'',tags:'',contentMd:'## 题目描述\n\n题面\n',warnings:['牛客未公开标签，可手动填写']}
beforeEach(()=>{localStorage.clear();vi.clearAllMocks()})
describe('牛客链接导入表单',()=>{
 it('读取后先预览，确认才填入，保存时提交完整字段',async()=>{
  mocks.read.mockResolvedValue(data)
  const wrapper=mount(ProblemForm,{props})
  await wrapper.get('input[placeholder="输入题目标题"]').setValue('原有草稿')
  await wrapper.get('#nowcoder-import-url').setValue(data.sourceUrl)
  await wrapper.get('.import-input-row button').trigger('click');await flushPromises()
  expect(mocks.read).toHaveBeenCalledWith(data.sourceUrl)
  expect((wrapper.get('input[placeholder="输入题目标题"]').element as HTMLInputElement).value).toBe('原有草稿')
  expect(wrapper.get('.import-preview').text()).toContain('可手动填写')
  await wrapper.get('.import-actions .btn-submit').trigger('click')
  expect((wrapper.get('input[placeholder="输入题目标题"]').element as HTMLInputElement).value).toBe('测试题')
  await wrapper.get('form').trigger('submit')
  expect(wrapper.emitted('submit')?.[0]?.[0]).toEqual(expect.objectContaining({platform:'NOWCODER',externalProblemKey:'NC123',title:'测试题',sourceUrl:data.sourceUrl,contentMd:data.contentMd}))
  wrapper.unmount()
 })
 it('读取失败不清空已输入内容，并显示后端原因',async()=>{
  mocks.read.mockRejectedValue({response:{data:{message:'题目需要登录'}}})
  const wrapper=mount(ProblemForm,{props})
  await wrapper.get('input[placeholder="输入题目标题"]').setValue('保留标题')
  await wrapper.get('#nowcoder-import-url').setValue(data.sourceUrl)
  await wrapper.get('.import-input-row button').trigger('click');await flushPromises()
  expect(wrapper.get('[role="alert"]').text()).toContain('题目需要登录')
  expect((wrapper.get('input[placeholder="输入题目标题"]').element as HTMLInputElement).value).toBe('保留标题')
  expect(wrapper.find('.import-preview').exists()).toBe(false)
  wrapper.unmount()
 })
 it('取消预览不修改表单；编辑模式默认不提供导入入口',async()=>{
  mocks.read.mockResolvedValue(data)
  const wrapper=mount(ProblemForm,{props})
  await wrapper.get('#nowcoder-import-url').setValue(data.sourceUrl)
  await wrapper.get('.import-input-row button').trigger('click');await flushPromises()
  await wrapper.get('.import-actions .btn-cancel').trigger('click')
  expect((wrapper.get('input[placeholder="输入题目标题"]').element as HTMLInputElement).value).toBe('')
  wrapper.unmount()
  const edit=mount(ProblemForm,{props:{...props,allowImport:false}})
  expect(edit.find('.link-import').exists()).toBe(false);edit.unmount()
 })
})
