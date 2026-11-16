import { computed, onUnmounted, ref } from 'vue'
import { chatWithLoveAppSSE, chatWithManusSSE, generateChatId } from '../services/api'

// Deliberately memory-only: private conversation text is not cached in localStorage.
export const conversations = ref([])
const selected = ref('')

export function useConversation(mode) {
  const draft = ref('')
  const pending = ref(false)
  const error = ref('')
  let stream = null
  let currentReply = null
  let smoothBuffer = ''
  let smoothTimer = null
  let streamEnded = false

  const current = computed(() => conversations.value.find(item => item.id === selected.value))

  function clearSmoothTimer() {
    if (smoothTimer) {
      clearInterval(smoothTimer)
      smoothTimer = null
    }
  }

  function finishReply() {
    clearSmoothTimer()
    smoothBuffer = ''
    streamEnded = false
    if (currentReply) currentReply.status = 'done'
    pending.value = false
    currentReply = null
    stream = null
  }

  function flushSmoothly() {
    if (!currentReply) {
      clearSmoothTimer()
      return
    }
    if (!smoothBuffer) {
      if (streamEnded) finishReply()
      else clearSmoothTimer()
      return
    }
    const step = smoothBuffer.length > 160 ? 10 : smoothBuffer.length > 40 ? 5 : 2
    currentReply.content += smoothBuffer.slice(0, step)
    smoothBuffer = smoothBuffer.slice(step)
    if (!smoothBuffer && streamEnded) {
      finishReply()
    }
  }

  function newConversation() {
    stop()
    const thread = { id: generateChatId(), mode, title: '新的对话', messages: [], createdAt: Date.now() }
    conversations.value.unshift(thread)
    selected.value = thread.id
    draft.value = ''
    error.value = ''
  }

  if (!current.value || current.value.mode !== mode) {
    const existing = conversations.value.find(item => item.mode === mode)
    if (existing) selected.value = existing.id
    else newConversation()
  }

  function stop() {
    clearSmoothTimer()
    smoothBuffer = ''
    streamEnded = false
    stream?.close()
    stream = null
    if (currentReply && pending.value) currentReply.status = 'stopped'
    currentReply = null
    pending.value = false
  }

  function selectConversation(id) {
    if (pending.value) return
    const thread = conversations.value.find(item => item.id === id && item.mode === mode)
    if (thread) {
      selected.value = id
      error.value = ''
      draft.value = ''
    }
  }

  function send(text = draft.value) {
    const message = String(text).trim()
    if (!message || pending.value) return
    if (message.length > 800) {
      error.value = '单条消息请控制在 800 字以内。'
      return
    }
    error.value = ''
    const thread = current.value
    if (!thread.messages.length) thread.title = message.slice(0, 20)
    thread.messages.push({ id: crypto.randomUUID(), role: 'user', content: message, status: 'done' })
    thread.messages.push({ id: crypto.randomUUID(), role: 'assistant', content: '', status: 'streaming' })

    // Use Vue's reactive proxy, not the raw object passed into push.
    const reply = thread.messages.at(-1)
    currentReply = reply
    smoothBuffer = ''
    streamEnded = false
    draft.value = ''
    pending.value = true

    const onMessage = chunk => {
      if (mode === 'manus' && chunk.length > 16) {
        // Keep tool-result lines immediate so ThoughtChain renders right away, smooth the rest.
        if (chunk.includes('完成了它的任务') && !chunk.includes('\n\n')) {
          reply.content += chunk
          return
        }
        smoothBuffer += chunk
        if (!smoothTimer) {
          smoothTimer = setInterval(flushSmoothly, 22)
        }
        return
      }
      reply.content += chunk
    }

    const finish = () => {
      if (smoothBuffer) {
        streamEnded = true
        if (!smoothTimer) smoothTimer = setInterval(flushSmoothly, 22)
        return
      }
      finishReply()
    }

    const fail = problem => {
      clearSmoothTimer()
      smoothBuffer = ''
      streamEnded = false
      reply.status = 'error'
      error.value = problem.message || '无法连接服务，请稍后重试。'
      pending.value = false
      currentReply = null
      stream = null
      draft.value = message
    }

    try {
      stream = mode === 'love'
        ? chatWithLoveAppSSE(message, thread.id, onMessage, fail, finish)
        : chatWithManusSSE(message, onMessage, fail, finish)
    } catch (problem) {
      fail(problem)
    }
  }

  onUnmounted(stop)
  return { current, draft, pending, error, send, stop, newConversation, selectConversation }
}
