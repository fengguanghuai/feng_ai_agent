import test from 'node:test'
import assert from 'node:assert/strict'
import { parseAssistantMessage } from '../src/services/thoughtChain.js'

test('parses Manus tool lines into ThoughtChain steps and keeps markdown body', () => {
  const input = [
    '工具 getCurrentTime 完成了它的任务！结果：{"date":"2026-11-16","time":"17:45:42"}',
    '',
    '已确认当前时间：2026年11月16日。',
    '',
    '- 方案A：市区经典线',
  ].join('\n')

  const { steps, body } = parseAssistantMessage(input)
  assert.equal(steps.length, 1)
  assert.equal(steps[0].name, 'getCurrentTime')
  assert.equal(steps[0].title, '获取当前时间（getCurrentTime）')
  assert.equal(steps[0].status, 'success')
  assert.match(steps[0].result, /"date": "2026-11-16"/)
  assert.equal(body, '已确认当前时间：2026年11月16日。\n\n- 方案A：市区经典线')
})

test('leaves plain assistant answers untouched when no tool call line exists', () => {
  const input = '你好，请问有什么可以帮您？'
  const { steps, body } = parseAssistantMessage(input)
  assert.equal(steps.length, 0)
  assert.equal(body, input)
})
