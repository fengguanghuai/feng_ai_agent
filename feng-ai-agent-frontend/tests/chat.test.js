import { test } from 'node:test'
import assert from 'node:assert/strict'
import { openChatStream } from '../src/services/api.js'
import { renderMarkdown } from '../src/services/markdown.js'

class FakeSource {
  static last
  constructor(url) { this.url = url; FakeSource.last = this; this.closed = false }
  close() { this.closed = true }
  emit(data) { this.onmessage({ data }) }
}
globalThis.EventSource = FakeSource

test('SSE preserves whitespace, finishes once, and ignores late events', () => {
  const chunks = []; let completed = 0
  openChatStream('/chat', { message: 'a&b 中文' }, x => chunks.push(x), () => assert.fail(), () => completed++)
  const source = FakeSource.last
  assert.equal(new URL(source.url, 'http://localhost').searchParams.get('message'), 'a&b 中文')
  source.emit(' hello\n  world'); source.emit('[DONE]'); source.emit('late'); source.onerror()
  assert.deepEqual(chunks, [' hello\n  world'])
  assert.equal(completed, 1); assert.equal(source.closed, true)
})
test('disconnect closes instead of retrying a paid request', () => {
  let errors = 0
  openChatStream('/chat', {}, () => assert.fail(), () => errors++, () => assert.fail())
  const source = FakeSource.last
  source.onerror(); source.onerror(); source.emit('late')
  assert.equal(errors, 1); assert.equal(source.closed, true)
})
test('manual stop suppresses late callbacks', () => {
  const stream = openChatStream('/chat', {}, () => assert.fail(), () => assert.fail(), () => assert.fail())
  stream.close(); FakeSource.last.emit('[DONE]'); FakeSource.last.onerror()
  assert.equal(FakeSource.last.closed, true)
})
test('Markdown renders lists but never raw HTML, script links, or remote images', () => {
  assert.match(renderMarkdown('- hello world'), /<li>hello world<\/li>/)
  assert.doesNotMatch(renderMarkdown('<img src=x onerror=alert(1)>'), /<img/)
  assert.doesNotMatch(renderMarkdown('[click](javascript:alert(1))'), /href="javascript:/)
  assert.doesNotMatch(renderMarkdown('![tracking](https://evil.example/pixel.png)'), /<img/)
  assert.match(renderMarkdown('[docs](https://example.com)'), /rel="noopener noreferrer nofollow"/)
})
