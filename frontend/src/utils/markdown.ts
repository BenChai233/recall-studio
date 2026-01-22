import { marked } from 'marked'

marked.use({
  renderer: {
    html() {
      return ''
    },
  },
  mangle: false,
  headerIds: false,
})

export function renderMarkdown(source: string): string {
  if (!source) return ''
  return marked.parse(source) as string
}

export function stripMarkdown(source: string, maxLen = 120): string {
  if (!source) return ''
  let text = source
  text = text.replace(/```[\s\S]*?```/g, ' ')
  text = text.replace(/`[^`]*`/g, ' ')
  text = text.replace(/!\[[^\]]*]\([^)]+\)/g, ' ')
  text = text.replace(/\[([^\]]+)]\([^)]+\)/g, '$1')
  text = text.replace(/^>\s?/gm, ' ')
  text = text.replace(/^#{1,6}\s+/gm, ' ')
  text = text.replace(/(\*{1,3}|_{1,3})(\S.*?)(\1)/g, '$2')
  text = text.replace(/^\s*[-*+]\s+/gm, ' ')
  text = text.replace(/^\s*\d+\.\s+/gm, ' ')
  text = text.replace(/<[^>]+>/g, ' ')
  text = text.replace(/\s+/g, ' ').trim()
  if (maxLen > 0 && text.length > maxLen) {
    return text.slice(0, maxLen) + '…'
  }
  return text
}
