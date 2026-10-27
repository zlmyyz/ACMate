import { describe, expect, it } from 'vitest'
import { extractDataConstraints, extractDataConstraintDetails } from '@/utils/problem-constraints'

describe('题目数据范围提取',()=>{
  it('只提取变量和边界，不重复题面文字',()=>{
    const text='给定一个数组，请求它的最大值。数据范围：1 ≤ n ≤ 100000，数组元素满足 -10^9 ≤ a_i ≤ 10^9。时间限制：1 秒。'
    expect(extractDataConstraints(text)).toEqual(['1 ≤ n ≤ 100000','-10^9 ≤ a_i ≤ 10^9'])
  })
  it('支持公式图片、LaTeX、多个变量和区间',()=>{
    const text='![1 \\leq n \\leq 10^{5}](<https://www.nowcoder.com/equation?tex=n>)，另有 m,k <= 2×10^5 和 x ∈ [1, 100]。'
    expect(extractDataConstraints(text)).toEqual(['1 ≤ n ≤ 10^5','m,k ≤ 2×10^5','x ∈ [1, 100]'])
  })
  it('提取牛客抢票题的 n、p、q 和数组元素范围',()=>{
    // 牛客原始 alt 包含双反斜杠；导入器将它们再转义后保存为 Markdown。
    const first=String.raw`n,p,q\\left(1\\leq n\\leq 100;\\ 1\\leq p\\leq q < 998\\,244\\,353\\right)`.replace(/\\/g,'\\\\')
    const second=String.raw`a_1,a_2,\\dots,a_n\\left(1\\leq a_i\\leq 100\\right)`.replace(/\\/g,'\\\\')
    const content=`## 输入描述\n\n第一行输入三个整数 ![${first}](<https://hr.nowcoder.com/equation?tex=first>)，表示出票口的数量、概率的分子和分母。\n\n第二行输入 n 个整数 ![${second}](<https://hr.nowcoder.com/equation?tex=second>)，表示余票数量。`
    expect(extractDataConstraints(content)).toEqual(['1 ≤ n ≤ 100','1 ≤ p ≤ q < 998244353','1 ≤ a_i ≤ 100'])
    expect(extractDataConstraintDetails(content)).toEqual([
      {text:'1 ≤ n ≤ 100',latex:String.raw`1\leq n\leq 100`},
      {text:'1 ≤ p ≤ q < 998244353',latex:String.raw`1\leq p\leq q < 998\,244\,353`},
      {text:'1 ≤ a_i ≤ 100',latex:String.raw`1\leq a_i\leq 100`},
    ])
  })
  it('兼容未额外转义的 LaTeX 空格和千位分隔符',()=>{
    expect(extractDataConstraints(String.raw`输入（1\leq p\leq q < 998\,244\,353）`)).toEqual(['1 ≤ p ≤ q < 998244353'])
  })
  it('排除示例中的值和代码块，之后恢复识别新的数据范围段落',()=>{
    const text='## 示例 1\n\n## 输入\n\n1 ≤ x ≤ 20\n\n```\n1 ≤ y ≤ 30\n```\n\n## 数据范围\n\n1 ≤ n ≤ 50'
    expect(extractDataConstraints(text)).toEqual(['1 ≤ n ≤ 50'])
  })
  it('去重且不把纯数字比较或无边界说明当作变量范围',()=>{
    expect(extractDataConstraints('2 < 3，n 的值取决于输入。\n\n1≤n≤100。\n\n1 ≤ n ≤ 100。')).toEqual(['1 ≤ n ≤ 100'])
    expect(extractDataConstraints(null)).toEqual([])
  })
  it('不把解题过程中的条件误当成全局数据范围',()=>{
    expect(extractDataConstraints('如果 n < 5，则输出 0；当 m ≥ 3 时，输出 1。\n\n数据范围：1 ≤ n ≤ 100，0 ≤ m ≤ 50。')).toEqual(['1 ≤ n ≤ 100','0 ≤ m ≤ 50'])
  })
  it('保留原公式的上下标和比较符，并兼容普通文本',()=>{
    const content=String.raw`![0\leqslant a_{i}\leqslant 10^{9}](<https://example.com/equation>)。普通范围：1 ≤ n ≤ 100。`
    expect(extractDataConstraintDetails(content)).toEqual([
      {text:'0 ≤ a_i ≤ 10^9',latex:String.raw`0\leqslant a_{i}\leqslant 10^{9}`},
      {text:'1 ≤ n ≤ 100',latex:null},
    ])
  })
})
