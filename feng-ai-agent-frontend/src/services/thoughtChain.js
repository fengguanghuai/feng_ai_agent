const TOOL_LINE_RE = /^工具\s+([^\s]+)\s+完成了它的任务[！!]\s*结果[:：]\s*(.*)$/

const TOOL_LABELS = {
  getCurrentTime: '获取当前时间',
  getCurrentDate: '获取当前日期',
  getCurrentDateTime: '获取当前日期时间',
  getTimestamp: '获取时间戳',
  formatTime: '格式化时间',
  doTerminate: '结束任务',
}

function formatToolResult(raw) {
  const trimmed = (raw || '').trim()
  if (!trimmed) return ''
  let candidate = trimmed
  if (candidate.startsWith('"') && candidate.endsWith('"')) {
    try {
      const unwrapped = JSON.parse(candidate)
      if (typeof unwrapped === 'string') candidate = unwrapped.trim()
    } catch {
      // Keep original string
    }
  }
  if (candidate.startsWith('{') || candidate.startsWith('[')) {
    try {
      return JSON.stringify(JSON.parse(candidate), null, 2)
    } catch {
      return candidate
    }
  }
  return candidate
}

export function parseAssistantMessage(content) {
  const text = String(content || '')
  if (!text) return { steps: [], body: '' }

  const lines = text.split(/\r?\n/)
  const steps = []
  const bodyLines = []

  for (const line of lines) {
    const match = line.match(TOOL_LINE_RE)
    if (match) {
      const name = match[1]
      const label = TOOL_LABELS[name]
      steps.push({
        key: `tool-${steps.length}-${name}`,
        name,
        title: label ? `${label}（${name}）` : `调用工具 ${name}`,
        description: '工具执行完成',
        result: formatToolResult(match[2]),
        status: 'success',
      })
    } else {
      bodyLines.push(line)
    }
  }

  return {
    steps,
    body: bodyLines.join('\n').trim(),
  }
}
