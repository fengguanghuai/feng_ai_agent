const BASE_URL = import.meta.env?.VITE_API_BASE_URL || '/api'

export const generateChatId = () => `chat_${crypto.randomUUID()}`

/** Keep the existing Spring SSE contract; never send a model key from the browser. */
export function openChatStream(path, params, onMessage, onError, onClose) {
  const source = new EventSource(`${BASE_URL}${path}?${new URLSearchParams(params)}`)
  let finished = false
  const timer = setTimeout(() => fail('回复等待超时，请稍后重试。'), 180000)
  function close() {
    finished = true
    clearTimeout(timer)
    source.close()
  }
  function fail(message) {
    if (finished) return
    close()
    onError(new Error(message))
  }
  source.onmessage = ({ data }) => {
    if (finished) return
    if (data === '[DONE]') {
      close()
      onClose()
    } else {
      onMessage(data)
    }
  }
  // Do not auto-reconnect: reconnecting a generation endpoint repeats paid requests.
  source.onerror = () => fail('连接中断，已保留收到的内容。请检查后端服务后重新发送。')
  return { close }
}

export const chatWithLoveAppSSE = (message, chatId, onMessage, onError, onClose) =>
  openChatStream('/ai/love_app_chat/sse_emitter', { message, chatId }, onMessage, onError, onClose)

export const chatWithManusSSE = (message, onMessage, onError, onClose) =>
  openChatStream('/ai/manus/chat', { message }, onMessage, onError, onClose)
