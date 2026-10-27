import { it, expect, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import CreatePostView from '@/views/CreatePostView.vue'
const mocks=vi.hoisted(()=>({create:vi.fn(),current:vi.fn()}))
vi.mock('@/api/discussion',()=>({createPost:mocks.create}))
vi.mock('@/api/auth',()=>({getCurrentUser:mocks.current}))
it('刷新会话中的旧角色后允许管理员选择并发布公告',async()=>{
 const pinia=createPinia();const auth=useAuthStore(pinia)
 const user={id:2,username:'plji',nickname:'成员',email:null,avatarUrl:null,bio:null,admin:false}
 auth.user=user
 mocks.current.mockResolvedValue({...user,admin:true});mocks.create.mockResolvedValue({id:9})
 const router=createRouter({history:createMemoryHistory(),routes:[{path:'/',component:{template:'<div />'}},{path:'/posts/:id',name:'post-detail',component:{template:'<div />'}}]})
 await router.push('/')
 const wrapper=mount(CreatePostView,{global:{plugins:[pinia,router]}})
 await flushPromises()
 const option=wrapper.get('option[value="ANNOUNCEMENT"]')
 expect(option.attributes('disabled')).toBeUndefined()
 await wrapper.get('select').setValue('ANNOUNCEMENT')
 await wrapper.get('input[placeholder="帖子标题"]').setValue('社团公告')
 await wrapper.get('textarea').setValue('公告内容')
 await wrapper.get('.save-btn').trigger('click');await flushPromises()
 expect(mocks.create).toHaveBeenCalledWith(expect.objectContaining({postType:'ANNOUNCEMENT',pinned:true}))
 expect(router.currentRoute.value.path).toBe('/posts/9')
 wrapper.unmount()
})
