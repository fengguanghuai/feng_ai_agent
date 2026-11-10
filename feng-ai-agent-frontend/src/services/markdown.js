import MarkdownIt from 'markdown-it'
const markdown = new MarkdownIt({ html: false, breaks: true, linkify: false })
const defaultLink = markdown.renderer.rules.link_open || ((tokens, idx, options, env, self) => self.renderToken(tokens, idx, options))
markdown.renderer.rules.link_open = (tokens, idx, options, env, self) => {
  tokens[idx].attrSet('target', '_blank')
  tokens[idx].attrSet('rel', 'noopener noreferrer nofollow')
  return defaultLink(tokens, idx, options, env, self)
}
// No remote image loads from model output: don't leak browser network data through Markdown.
markdown.renderer.rules.image = (tokens, idx) => markdown.utils.escapeHtml(tokens[idx].content || '图片')
export const renderMarkdown = (text) => markdown.render(String(text || ''))
