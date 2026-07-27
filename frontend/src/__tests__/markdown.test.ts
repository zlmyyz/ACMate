import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import MarkdownContent from '@/components/common/MarkdownContent.vue'

describe('MarkdownContent security', () => {
  function render(content: string): string {
    const wrapper = mount(MarkdownContent, { props: { content } })
    return wrapper.html()
  }

  it('renders normal markdown', () => {
    const html = render('# Hello\n\nThis is **bold** text.')
    expect(html).toContain('<h1>Hello</h1>')
    expect(html).toContain('<strong>bold</strong>')
  })

  it('escapes script tags in content', () => {
    const html = render('<script>alert(1)</script>')
    expect(html).not.toContain('<script>')
    expect(html).not.toContain('</script>')
    expect(html).toContain('&lt;script&gt;')
  })

  it('escapes img onerror in content', () => {
    const html = render('<img src=x onerror=alert(1)>')
    // html: false means the tag is escaped, not rendered as a real <img>
    expect(html).toContain('&lt;img')
    expect(html).not.toContain('<img ')
  })

  it('escapes HTML inside markdown code blocks', () => {
    const html = render('```html\n<script>alert(1)</script>\n```')
    expect(html).toContain('<pre>')
    expect(html).toContain('language-html')
    // Inside code block, the script tag becomes escaped text
    expect(html).toContain('&lt;script&gt;')
  })

  it('blocks javascript: protocol in markdown links', () => {
    const html = render('[click me](javascript:alert(1))')
    // validateLink returns false, so the link is not rendered as <a href="...">
    expect(html).not.toContain('href="javascript:')
  })

  it('blocks data: protocol in markdown links', () => {
    const html = render('[click me](data:text/html,<script>alert(1)</script>)')
    expect(html).not.toContain('href="data:')
  })

  it('blocks vbscript: protocol in markdown links', () => {
    const html = render('[click me](vbscript:msgbox(1))')
    expect(html).not.toContain('href="vbscript:')
  })

  it('escapes uppercase SCRIPT tags', () => {
    const html = render('<SCRIPT>alert(1)</SCRIPT>')
    expect(html).not.toContain('<SCRIPT>')
    expect(html).toContain('&lt;SCRIPT&gt;')
  })

  it('escapes mixed case HTML tags', () => {
    const html = render('<ScRiPt>alert(1)</ScRiPt>')
    expect(html).not.toContain('<ScRiPt>')
    expect(html).toContain('&lt;ScRiPt&gt;')
  })

  it('renders legitimate markdown links normally', () => {
    const html = render('[safe link](https://example.com)')
    expect(html).toContain('href="https://example.com"')
    expect(html).toContain('safe link')
  })

  it('escapes iframe tags', () => {
    const html = render('<iframe src="https://evil.com"></iframe>')
    expect(html).not.toContain('<iframe')
    expect(html).toContain('&lt;iframe')
  })
})
