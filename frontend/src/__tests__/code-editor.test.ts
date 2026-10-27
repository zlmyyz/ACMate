import { describe, it, expect, vi, beforeAll } from 'vitest'
import { mount } from '@vue/test-utils'
import CodeEditor from '@/components/editor/CodeEditor.vue'
import { startCompletion, completionStatus, currentCompletions } from '@codemirror/autocomplete'
beforeAll(()=>{
  vi.stubGlobal('ResizeObserver', class { observe(){} unobserve(){} disconnect(){} })
  Range.prototype.getClientRects = () => [] as unknown as DOMRectList
  Range.prototype.getBoundingClientRect = () => new DOMRect()
})
describe('代码编辑器',()=>{
 it('支持 Tab 缩进、快捷保存、快捷运行以及放大还原',async()=>{
  const w=mount(CodeEditor,{props:{modelValue:'int main() {}',language:'CPP17'},attachTo:document.body})
  const content=w.get('[aria-label="代码编辑器"]')
  ;(content.element as HTMLElement).focus()
  await content.trigger('keydown',{key:'Tab',code:'Tab'})
  const edits=w.emitted('update:modelValue') ?? []; expect(edits[edits.length-1]?.[0]).toBe('    int main() {}')
  await content.trigger('keydown',{key:'s',code:'KeyS',ctrlKey:true})
  await content.trigger('keydown',{key:'Enter',code:'Enter',ctrlKey:true})
  expect(w.emitted('save')).toHaveLength(1);expect(w.emitted('run')).toHaveLength(1)
  await content.trigger('keydown',{key:"'",code:'Quote',ctrlKey:true})
  expect(w.emitted('run')).toHaveLength(2)
  await w.get('button[aria-label="放大编辑器"]').trigger('click');expect(w.classes()).toContain('expanded')
  await content.trigger('keydown',{key:'Escape',code:'Escape'});expect(w.classes()).not.toContain('expanded')
  w.unmount()
 })
 it('运行时只读，语言切换保持父组件代码并显示对应文件名',async()=>{
  const w=mount(CodeEditor,{props:{modelValue:'print(1)',language:'PYTHON3',disabled:true},attachTo:document.body})
  expect(w.get('[aria-label="代码编辑器"]').attributes('contenteditable')).toBe('false')
  expect(w.get('.file-tab').text()).toContain('Main.py')
  await w.setProps({language:'JAVA17',modelValue:'class Main {}',disabled:false})
  expect(w.get('.file-tab').text()).toContain('Main.java')
  expect(w.get('.cm-content').text()).toContain('class Main {}');w.unmount()
 })
 it('放大时保留底部自测操作',async()=>{
  const w=mount(CodeEditor,{props:{modelValue:'',language:'CPP17'},slots:{footer:'<button>自测运行</button>'}})
  await w.get('button[aria-label="放大编辑器"]').trigger('click')
  expect(w.get('.expanded .editor-footer').text()).toContain('自测运行');w.unmount()
 })
 it('关闭补全会关闭提示并停止再次提供建议，重新开启可恢复',async()=>{
  const w=mount(CodeEditor,{props:{modelValue:'cons',language:'JAVASCRIPT',autocomplete:true},attachTo:document.body})
  const view=w.vm.getView()!;view.dispatch({selection:{anchor:4}});startCompletion(view)
  await new Promise(resolve=>setTimeout(resolve,100))
  expect(currentCompletions(view.state).some(value=>value.label==='const')).toBe(true)
  await w.setProps({autocomplete:false});expect(completionStatus(view.state)).toBeNull()
  startCompletion(view);await new Promise(resolve=>setTimeout(resolve,100));expect(currentCompletions(view.state)).toHaveLength(0)
  await w.setProps({autocomplete:true});startCompletion(view);await new Promise(resolve=>setTimeout(resolve,100));expect(currentCompletions(view.state).length).toBeGreaterThan(0)
  await w.get('[aria-label="代码补全"]').setValue(false);expect(w.emitted('update:autocomplete')?.[0]).toEqual([false]);w.unmount()
 })
 it.each([['CSHARP','Main.cs'],['TYPESCRIPT','Main.ts'],['GO','Main.go'],['RUST','Main.rs'],['C17','Main.c']])('支持 %s 的语言模式和文件名',async(language,filename)=>{
  const w=mount(CodeEditor,{props:{modelValue:'',language}});expect(w.get('.file-tab').text()).toContain(filename);w.unmount()
 })
})

