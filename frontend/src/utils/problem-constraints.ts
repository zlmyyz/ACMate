import MarkdownIt from 'markdown-it'

const markdown = new MarkdownIt({ html: false })
const variable = String.raw`(?:\|[A-Za-z][A-Za-z0-9_]{0,39}\||[A-Za-z][A-Za-z0-9_]{0,39}(?:\[[A-Za-z0-9_+\-]{1,20}\])?)`
const variableGroup = String.raw`${variable}(?:\s*[,，、]\s*${variable})*`
const number = String.raw`[+\-]?\d{1,30}(?:\.\d+)?(?:[eE][+\-]?\d+)?(?:\s*[×·*]\s*\d+(?:\^\d+)?)?(?:\^\d+)?[⁰¹²³⁴⁵⁶⁷⁸⁹]*`
const atom = String.raw`(?:${number}|${variableGroup})`
const operator = String.raw`(?:≤|≥|<|>)`
const comparison = new RegExp(String.raw`(?<![A-Za-z0-9_.])(${atom}(?:\s*${operator}\s*${atom}){1,4})(?![A-Za-z0-9_.])`, 'g')
const interval = new RegExp(String.raw`(?<![A-Za-z0-9_.])(${variableGroup}\s*∈\s*[\[(]\s*${atom}\s*[,，]\s*${atom}\s*[\])])`, 'g')
const variableOnly = new RegExp(String.raw`^${variableGroup}$`)
const hasNumber = /\d/

interface MappedText { value: string; starts: number[]; ends: number[] }
export interface DataConstraint { text: string; latex: string | null }

/** Keep source offsets while normalizing, so the displayed math can use the original LaTeX. */
function replaceMapped(input: MappedText, pattern: RegExp, replacement: (match: string) => string): MappedText {
  let value = '', cursor = 0
  const starts: number[] = [], ends: number[] = []
  const appendOriginal = (from: number, to: number) => {
    value += input.value.slice(from, to)
    for (let i = from; i < to; i++) { starts.push(input.starts[i]!); ends.push(input.ends[i]!) }
  }
  for (const match of input.value.matchAll(pattern)) {
    const index = match.index
    appendOriginal(cursor, index)
    const result = replacement(match[0])
    value += result
    for (let i = 0; i < result.length; i++) { starts.push(input.starts[index]!); ends.push(input.ends[index + match[0].length - 1]!) }
    cursor = index + match[0].length
  }
  appendOriginal(cursor, input.value.length)
  return { value, starts, ends }
}

function plainMath(source: string): MappedText {
  let result: MappedText = { value: source, starts: [], ends: [] }
  for (let i = 0; i < source.length; i++) { result.starts.push(i); result.ends.push(i + 1) }
  const apply = (pattern: RegExp, replacement: (match: string) => string) => { result = replaceMapped(result, pattern, replacement) }
  apply(/(\d)\\,(?=\d)/g, value => value[0]!)
  // Markdown unescapes a single LaTeX \, into a comma in ordinary text.
  apply(/\b\d{1,3}(?:,\d{3})+\b/g, value => value.replace(/,/g, ''))
  apply(/\\[,;:!]/g, () => ' ')
  apply(/\\[ \t]+/g, () => ' ')
  apply(/\\(?:quad|qquad|enspace|thinspace)\b/g, () => ' ')
  apply(/\\(?:left|right)\b/g, () => '')
  apply(/\\(?:leqslant|leqq|leq|le)\b/g, () => '≤')
  apply(/\\(?:geqslant|geqq|geq|ge)\b/g, () => '≥')
  apply(/\\lt\b/g, () => '<'); apply(/\\gt\b/g, () => '>')
  apply(/\\(?:times|cdot)\b/g, () => '×'); apply(/\\in\b/g, () => '∈')
  apply(/\\(?:lvert|rvert|vert)\b/g, () => '|')
  apply(/_\{([A-Za-z0-9_+\-]+)\}/g, value => '_' + value.slice(2, -1))
  apply(/\^\{([+\-]?\d+)\}/g, value => '^' + value.slice(2, -1))
  apply(/<=/g, () => '≤'); apply(/>=/g, () => '≥')
  return result
}

/** Extract only explicit numeric bounds. The full original statement stays untouched. */
export function extractDataConstraintDetails(content: string | null): DataConstraint[] {
  if (!content) return []
  const tokens = markdown.parse(content, {})
  const ranges: DataConstraint[] = []
  let sampleDepth: number | null = null
  let rangeSection = false
  const add = (value: string, start: number, source: string, mapped: MappedText, images: { start: number; end: number }[]) => {
    const normalized = value.replace(/\s+/g, ' ').replace(/\s*(≤|≥|<|>|∈)\s*/g, ' $1 ').trim()
    const from = mapped.starts[start]!, to = mapped.ends[start + value.length - 1]!
    const raw = source.slice(from, to)
    const imageMath = images.some(image => image.start < to && image.end > from)
    const inlineMath = /\$[^$]*$/.test(source.slice(0, from)) && /^[^$]*\$/.test(source.slice(to))
    const isLatex = imageMath || inlineMath || /\\(?:leqslant|leqq|leq|le|geqslant|geqq|geq|ge|in|times|cdot)\b/.test(raw)
    const latex = isLatex ? raw.replace(/\\(?:left|right)\b/g, '').replace(/<=/g, '\\leq ').replace(/>=/g, '\\geq ').replace(/≤/g, '\\leq ').replace(/≥/g, '\\geq ').replace(/∈/g, '\\in ').replace(/×/g, '\\times ') : null
    const existing = ranges.find(range => range.text === normalized)
    if (existing) { if (!existing.latex && latex) existing.latex = latex; return }
    if (ranges.length < 40) ranges.push({ text: normalized, latex })
  }
  for (let index = 0; index < tokens.length; index++) {
    const token = tokens[index]!
    if (token.type !== 'inline') continue
    const heading = tokens[index - 1]
    if (heading?.type === 'heading_open') {
      const level = Number(heading.tag.slice(1))
      const title = token.content.trim().replace(/[:：]$/, '')
      rangeSection = /数据范围|取值范围|约束条件|范围限制/.test(title)
      if (/示例|样例/.test(title)) sampleDepth = level
      else if (sampleDepth !== null && level <= sampleDepth && !/^(输入|输出|输入描述|输出描述|说明)$/.test(title)) sampleDepth = null
      continue
    }
    if (sampleDepth !== null) continue
    // Formula image alt text contains the original LaTeX; never read image URLs.
    let source = ''
    const images: { start: number; end: number }[] = []
    for (const child of token.children ?? []) {
      const value = child.content.replace(/\\{2,}(?=[A-Za-z,;:!\s])/g, '\\')
      if (child.type === 'image') images.push({ start: source.length, end: source.length + value.length })
      source += value
    }
    if (source.length > 10000) continue
    const mapped = plainMath(source), text = mapped.value
    const conditional = (start: number, length: number) => {
      const before = text.slice(Math.max(0, start - 80), start).split(/[。；;\n]/).pop() ?? ''
      const explicit = rangeSection || /数据范围|取值范围|约束条件|范围限制|保证|满足/.test(before)
      return !explicit && (/(?:如果|若|当|仅当|否则|例如|比如)[^。；;\n]{0,60}$/.test(before) || /^\s*(?:时|则|的话)/.test(text.slice(start + length)))
    }
    for (const match of text.matchAll(comparison)) {
      const value = match[1]!
      const parts = value.split(/\s*(?:≤|≥|<|>)\s*/)
      if (!conditional(match.index, value.length) && hasNumber.test(value) && parts.some(part => variableOnly.test(part.trim()))) add(value, match.index, source, mapped, images)
    }
    for (const match of text.matchAll(interval)) if (!conditional(match.index, match[1]!.length) && hasNumber.test(match[1]!)) add(match[1]!, match.index, source, mapped, images)
    if (ranges.length >= 40) break
  }
  return ranges
}

/** Compatibility helper for callers that only need plain text. */
export function extractDataConstraints(content: string | null): string[] {
  return extractDataConstraintDetails(content).map(range => range.text)
}
