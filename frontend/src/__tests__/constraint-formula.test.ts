import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ConstraintFormula from '@/components/common/ConstraintFormula.vue'

describe('范围公式',()=>{
  it('用数学公式排版原题的下标和指数',()=>{
    const wrapper=mount(ConstraintFormula,{props:{range:{text:'1 ≤ a_i ≤ 10^9',latex:String.raw`1\leq a_i\leq 10^{9}`}}})
    expect(wrapper.find('.katex').exists()).toBe(true)
    expect(wrapper.find('msub').exists()).toBe(true)
    expect(wrapper.find('msup').exists()).toBe(true)
  })
  it('无法渲染或没有 LaTeX 时安全地显示原范围',()=>{
    const invalid=mount(ConstraintFormula,{props:{range:{text:'1 ≤ n ≤ 5',latex:String.raw`\unknown{<script>alert(1)</script>}`}}})
    expect(invalid.text()).toBe('1 ≤ n ≤ 5');expect(invalid.find('script').exists()).toBe(false)
    const plain=mount(ConstraintFormula,{props:{range:{text:'1 ≤ n ≤ 5',latex:null}}})
    expect(plain.text()).toBe('1 ≤ n ≤ 5');expect(plain.find('.katex').exists()).toBe(false)
  })
})
