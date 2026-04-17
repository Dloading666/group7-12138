import request from '../utils/request'

const DEFAULT_STREAM_MODEL = 'contract-gemma4:latest'

export function createAiAnalysisTask(data, config = {}) {
  return request({
    url: '/ai-analysis/tasks',
    method: 'post',
    data,
    ...config
  })
}

export function getAiAnalysisMessages(taskId, config = {}) {
  return request({
    url: `/ai-analysis/tasks/${taskId}/messages`,
    method: 'get',
    ...config
  })
}

export function sendAiAnalysisMessage(taskId, question, config = {}) {
  return request({
    url: `/ai-analysis/tasks/${taskId}/messages`,
    method: 'post',
    data: { question },
    timeout: 180000,
    ...config
  })
}

const readErrorMessage = async (response) => {
  const text = await response.text()

  if (!text) {
    return `请求失败 (${response.status})`
  }

  try {
    const data = JSON.parse(text)
    return data?.error?.message || data?.message || text
  } catch (error) {
    return text
  }
}

export async function streamAiAnalysisMessage(messages, options = {}) {
  const {
    signal,
    model = DEFAULT_STREAM_MODEL,
    temperature = 0.1,
    maxTokens = 128,
    reasoningEffort = 'none',
    onReasoning,
    onContent
  } = options

  const response = await fetch('/ollama-api/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      model,
      stream: true,
      temperature,
      max_tokens: maxTokens,
      reasoning_effort: reasoningEffort,
      reasoning: {
        effort: reasoningEffort
      },
      messages
    }),
    signal
  })

  if (!response.ok) {
    throw new Error(await readErrorMessage(response))
  }

  if (!response.body) {
    throw new Error('流式响应不可用')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')

  let buffer = ''
  let reasoning = ''
  let content = ''
  let finished = false

  const consumeEventBlock = (block) => {
    const lines = block
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line.startsWith('data:'))

    for (const line of lines) {
      const payload = line.slice(5).trim()
      if (!payload) {
        continue
      }

      if (payload === '[DONE]') {
        finished = true
        return
      }

      const parsed = JSON.parse(payload)
      const delta = parsed?.choices?.[0]?.delta || {}
      const reasoningDelta = delta.reasoning || delta.reasoning_content || ''
      const contentDelta = delta.content || ''

      if (reasoningDelta) {
        reasoning += reasoningDelta
        onReasoning?.(reasoning, reasoningDelta)
      }

      if (contentDelta) {
        content += contentDelta
        onContent?.(content, contentDelta)
      }
    }
  }

  while (!finished) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }

    buffer += decoder.decode(value, { stream: true })

    let separatorIndex = buffer.indexOf('\n\n')
    while (separatorIndex !== -1) {
      const block = buffer.slice(0, separatorIndex).trim()
      buffer = buffer.slice(separatorIndex + 2)

      if (block) {
        consumeEventBlock(block)
      }

      separatorIndex = buffer.indexOf('\n\n')
    }
  }

  if (!finished && buffer.trim()) {
    consumeEventBlock(buffer.trim())
  }

  return {
    content,
    reasoning
  }
}
