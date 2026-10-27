import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent } from 'vue'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { useEditorSettings } from '@/composables/useEditorSettings'
const api=vi.hoisted(()=>({get:vi.fn(),save:vi.fn()}))
vi.mock('@/api/editor-settings',()=>({getEditorSettings:api.get,saveEditorSettings:api.save}))
const member=(id:number)=>({id,username:'member'+id,nickname:'Member',email:null,avatarUrl:null,bio:null,admin:false})
const Harness=defineComponent({setup:useEditorSettings,template:'<div>{{ settingsMessage }}</div>'})
beforeEach(()=>{vi.clearAllMocks();localStorage.clear();api.get.mockResolvedValue({autocomplete:true});api.save.mockResolvedValue({autocomplete:false})})
describe('account editor settings',()=>{
  it('loads the server preference and saves changes with the owning account ID',async()=>{
    const pinia=createPinia();useAuthStore(pinia).user=member(1);api.get.mockResolvedValue({autocomplete:false})
    const w=mount(Harness,{global:{plugins:[pinia]}});await flushPromises();expect(w.vm.autocomplete).toBe(false)
    w.vm.setAutocomplete(true);await flushPromises();expect(api.save).toHaveBeenCalledWith(true,1,'CPP17')
    expect(JSON.parse(localStorage.getItem('acmate:editor-settings:1')!)).toEqual({autocomplete:true,language:'CPP17'});expect(w.text()).toContain('已同步到账号');w.unmount()
  })
  it('restores and synchronizes the preferred language together with autocomplete',async()=>{
    const pinia=createPinia();useAuthStore(pinia).user=member(1)
    api.get.mockResolvedValue({autocomplete:false,language:'GO'})
    const w=mount(Harness,{global:{plugins:[pinia]}});await flushPromises();expect(w.vm.language).toBe('GO')
    w.vm.setLanguage('PYTHON3');await flushPromises()
    expect(api.save).toHaveBeenCalledWith(false,1,'PYTHON3')
    expect(JSON.parse(localStorage.getItem('acmate:editor-settings:1')!)).toEqual({autocomplete:false,language:'PYTHON3'})
    w.unmount()
  })
  it('never applies a prior account save to the next account',async()=>{
    const pinia=createPinia(),auth=useAuthStore(pinia);auth.user=member(1)
    let complete!:(value:{autocomplete:boolean})=>void;api.save.mockImplementation(()=>new Promise(resolve=>{complete=resolve}))
    const w=mount(Harness,{global:{plugins:[pinia]}});await flushPromises();w.vm.setAutocomplete(false)
    api.get.mockResolvedValue({autocomplete:true});auth.user=member(2);await flushPromises()
    complete({autocomplete:false});await flushPromises();expect(w.vm.autocomplete).toBe(true);expect(api.save).toHaveBeenCalledTimes(1)
    expect(JSON.parse(localStorage.getItem('acmate:editor-settings:2')!)).toEqual({autocomplete:true,language:'CPP17'});w.unmount()
  })
  it('serializes quick toggles so the last selection reaches the server',async()=>{
    const pinia=createPinia();useAuthStore(pinia).user=member(1)
    let complete!:(value:{autocomplete:boolean})=>void;api.save.mockImplementationOnce(()=>new Promise(resolve=>{complete=resolve})).mockResolvedValue({autocomplete:true})
    const w=mount(Harness,{global:{plugins:[pinia]}});await flushPromises();w.vm.setAutocomplete(false);w.vm.setAutocomplete(true)
    complete({autocomplete:false});await flushPromises();expect(api.save.mock.calls).toEqual([[false,1,'CPP17'],[true,1,'CPP17']]);w.unmount()
  })
  it('finishes the latest queued preference after leaving the editor',async()=>{
    const pinia=createPinia();useAuthStore(pinia).user=member(1)
    let complete!:(value:{autocomplete:boolean})=>void;api.save.mockImplementationOnce(()=>new Promise(resolve=>{complete=resolve})).mockResolvedValue({autocomplete:true})
    const w=mount(Harness,{global:{plugins:[pinia]}});await flushPromises();w.vm.setAutocomplete(false);w.vm.setAutocomplete(true);w.unmount()
    complete({autocomplete:false});await flushPromises();expect(api.save.mock.calls).toEqual([[false,1,'CPP17'],[true,1,'CPP17']])
  })
})
