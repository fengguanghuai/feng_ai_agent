<script setup>
import { computed, h, nextTick, ref, watch } from 'vue'
import { Bubble, Sender } from 'ant-design-x-vue'
import AppIcon from './AppIcon.vue'
import { conversations, useConversation } from '../composables/useConversation'
import { renderMarkdown } from '../services/markdown'

const props = defineProps({ mode: { type: String, default: 'love' } })
const isLove = computed(() => props.mode === 'love')
const { current, draft, pending, error, send, stop, newConversation, selectConversation } = useConversation(props.mode)
const title = computed(() => isLove.value ? 'AI 恋爱大师' : 'Manus 通用助手')
const history = computed(() => conversations.value.filter(item => item.mode === props.mode && item.messages.length))
const sender = ref(null)
const scrollArea = ref(null)
const followLatest = ref(true)
const copied = ref('')
const notice = ref('')
const showInfo = ref(false)
const prompts = computed(() => isLove.value ? [
  { icon: 'chat', tag: '打破沉默', title: '第一句话，怎么说？', text: '想和喜欢的人自然地开启聊天，有哪些不尴尬的开场方式？' },
  { icon: 'compass', tag: '理清心意', title: '这段关系，怎么看？', text: '我们聊天很开心，但对方很少主动。我想理清自己的感受，该从哪里开始？' },
  { icon: 'leaf', tag: '好好相处', title: '不同意见，怎么聊？', text: '我和伴侣最近有一些分歧，怎样表达自己的需求，又不让对方感到被指责？' },
] : [
  { icon: 'book', tag: '梳理思路', title: '把想法变成计划', text: '帮我制定一份每天半小时、持续一周的学习计划，开始前先问我学习目标。' },
  { icon: 'chat', tag: '清晰表达', title: '让文字更有条理', text: '我想写一段清晰又自然的工作汇报，请先告诉我需要提供哪些信息。' },
  { icon: 'compass', tag: '拆解问题', title: '找到下一步行动', text: '我有一个复杂的任务不知从何下手，请用几个问题帮我拆解它。' },
])
function fillPrompt(text) {
  draft.value = text
  nextTick(() => sender.value?.focus({ cursor: 'end' }))
}
function submit(text) { followLatest.value = true; send(text) }
function reset() { newConversation(); followLatest.value = true; nextTick(() => sender.value?.focus()) }
function scrollLatest() {
  const el = scrollArea.value
  if (el) el.scrollTop = el.scrollHeight
  followLatest.value = true
}
function onScroll() {
  const el = scrollArea.value
  followLatest.value = el.scrollHeight - el.scrollTop - el.clientHeight < 90
}
watch(() => current.value?.messages.map(item => `${item.content}:${item.status}`).join(''), async () => {
  await nextTick()
  if (followLatest.value) scrollLatest()
})
function renderContent(content, role) {
  return role === 'assistant'
    ? h('div', { class: 'message-markdown', innerHTML: renderMarkdown(content) })
    : h('div', { class: 'user-text' }, content)
}
async function copyMessage(item) {
  try { await navigator.clipboard.writeText(item.content); copied.value = item.id; notice.value = '回复已复制' }
  catch { notice.value = '无法访问剪贴板，请选中文字复制。' }
}
</script>

<template>
  <div class="workspace" :class="{ 'manus-mode': !isLove }">
    <aside class="sidebar" aria-label="助手导航">
      <RouterLink to="/" class="brand" aria-label="恋爱大师首页">
        <span class="brand-mark"><AppIcon name="heart" :size="23" /></span>
        <span><strong>恋爱大师</strong><small>留一点时间，听听自己</small></span>
      </RouterLink>
      <button class="new-conversation" :disabled="pending" @click="reset"><AppIcon name="plus" :size="18" />开启新的对话<span class="key-hint">＋</span></button>
      <div class="nav-label">你的 AI 助手</div>
      <nav class="assistant-nav" aria-label="选择助手">
        <RouterLink to="/love-app" :class="{ selected: isLove }" :aria-current="isLove ? 'page' : undefined"><AppIcon name="heart" /><span>恋爱与关系</span><span class="nav-dot" v-if="isLove"></span></RouterLink>
        <RouterLink to="/manus-app" :class="{ selected: !isLove }" :aria-current="!isLove ? 'page' : undefined"><AppIcon name="spark" /><span>通用助手</span><span class="nav-dot" v-if="!isLove"></span></RouterLink>
      </nav>
      <section class="history-section" aria-label="本次访问的对话">
        <div class="nav-label">本次访问 <span>{{ history.length }}</span></div>
        <p v-if="!history.length" class="history-empty">从一个小问题开始，<br />你的对话会出现在这里。</p>
        <div v-else class="history-list">
          <button v-for="thread in history" :key="thread.id" :class="{ active: thread.id === current.id }" :disabled="pending" @click="selectConversation(thread.id); followLatest = true" :title="thread.title"><AppIcon name="chat" :size="16" /><span>{{ thread.title }}</span></button>
        </div>
      </section>
      <div class="sidebar-note"><span class="note-symbol">“</span><p>好的关系，<br />从理解自己开始。</p><span class="note-line"></span><small>不急着找到标准答案</small></div>
      <div class="sidebar-footer"><span class="local-dot"></span><span>本机体验版</span><button class="icon-button" @click="showInfo = !showInfo" aria-label="查看使用说明" :aria-expanded="showInfo"><AppIcon name="shield" :size="17" /></button></div>
    </aside>

    <main class="main-panel">
      <header class="workspace-header">
        <div class="page-label"><AppIcon :name="isLove ? 'heart' : 'spark'" :size="18" /><span>{{ title }}</span><span class="header-divider"></span><span class="header-description">{{ isLove ? '认真倾听，也一起想办法' : '把复杂的想法，慢慢理清' }}</span></div>
        <div class="model-label"><span class="model-dot"></span>DeepSeek Flash</div>
      </header>
      <div v-if="showInfo" class="info-banner" role="status"><AppIcon name="shield" /><p>聊天及命中的知识片段会发送至 DeepSeek。本地知识库模式在后端检索文件，不调用百炼；若配置为百炼模式，恋爱问题也会发送至阿里云检索。恋爱对话保存在 Supabase。本次访问列表刷新后清空，不代表删除数据库记录。当前未启用登录，请仅在本机使用，避免输入敏感个人信息。</p><button class="icon-button" aria-label="关闭使用说明" @click="showInfo = false"><AppIcon name="close" /></button></div>

      <div class="conversation-scroll" ref="scrollArea" @scroll.passive="onScroll">
        <section v-if="!current.messages.length" class="welcome" aria-labelledby="welcome-heading">
          <div class="welcome-eyebrow"><span></span>{{ isLove ? '给心事一个可以停靠的地方' : '给想法一个可以展开的地方' }}</div>
          <div class="welcome-art" aria-hidden="true"><span class="orbit orbit-one"></span><span class="orbit orbit-two"></span><span class="orbit-dot"></span><AppIcon :name="isLove ? 'heart' : 'spark'" :size="39" /></div>
          <h1 id="welcome-heading">{{ isLove ? '有些心事，' : '有些想法，' }}<br /><span>{{ isLove ? '可以慢慢说。' : '值得好好聊。' }}</span></h1>
          <p class="welcome-description">{{ isLove ? '心动时的犹豫，相处中的困惑，或只是想找个地方说说。' : '梳理思路、学习新知，或把一个模糊的目标变得清晰。' }}<br />{{ isLove ? '不必组织好语言，我们一起从这里开始。' : '从你正在思考的那个问题开始。' }}</p>
          <div class="welcome-topics"><span><AppIcon name="chat" :size="15" />{{ isLove ? '真诚表达' : '清晰表达' }}</span><i></i><span><AppIcon name="compass" :size="15" />{{ isLove ? '理解彼此' : '拆解问题' }}</span><i></i><span><AppIcon name="leaf" :size="15" />{{ isLove ? '尊重边界' : '找到方向' }}</span></div>
          <div class="prompt-heading"><span>不知道从哪里开始？</span><span>试着聊聊这些</span></div>
          <div class="prompt-grid">
            <button v-for="prompt in prompts" :key="prompt.title" class="prompt-card" @click="fillPrompt(prompt.text)"><span class="prompt-top"><span class="prompt-icon"><AppIcon :name="prompt.icon" /></span><small>{{ prompt.tag }}</small></span><strong>{{ prompt.title }}</strong><span class="prompt-example">{{ prompt.text }}</span><AppIcon class="prompt-arrow" name="arrow" :size="17" /></button>
          </div>
          <p v-if="!isLove" class="capability-note">当前仅启用基础时间工具；邮件、联网搜索与图片服务尚未开通。每次发送由 Manus 独立处理，请在消息中补充必要背景。</p>
        </section>

        <section v-else class="message-list" aria-label="对话消息" :aria-busy="pending">
          <div class="conversation-start"><span></span><span>从理解开始，不急着下结论</span><span></span></div>
          <article v-for="item in current.messages" :key="item.id" class="message-row" :class="item.role">
            <Bubble :content="item.content" :placement="item.role === 'user' ? 'end' : 'start'" :variant="item.role === 'user' ? 'filled' : 'borderless'" :loading="item.status === 'streaming' && !item.content" :streaming="item.status === 'streaming'" :content-render="content => renderContent(content, item.role)" :styles="{ content: { maxWidth: '100%' } }">
              <template #avatar><span class="message-avatar" :class="item.role"><AppIcon v-if="item.role === 'assistant'" :name="isLove ? 'heart' : 'spark'" :size="18" /><span v-else>我</span></span></template>
              <template #header><span class="message-author">{{ item.role === 'assistant' ? title : '你' }}<span v-if="item.status === 'streaming'" class="streaming-label">{{ item.content ? '正在回复' : '正在思考' }}</span></span></template>
              <template #footer v-if="item.role === 'assistant'">
                <div class="message-actions"><button v-if="item.content" class="copy-button" @click="copyMessage(item)" :aria-label="copied === item.id ? '已复制回复' : '复制回复'"><AppIcon :name="copied === item.id ? 'check' : 'copy'" :size="15" />{{ copied === item.id ? '已复制' : '复制' }}</button><span v-if="item.status === 'stopped'">已停止接收 · 服务端可能仍在生成</span><span v-if="item.status === 'error'">回复未完成</span></div>
              </template>
            </Bubble>
          </article>
        </section>
      </div>

      <div class="composer-region">
        <button v-if="current.messages.length && !followLatest" class="latest-button" @click="scrollLatest">回到最新消息<AppIcon name="down" :size="16" /></button>
        <div v-if="error" class="error-banner" role="alert">{{ error }}<button class="icon-button" aria-label="关闭错误提示" @click="error = ''"><AppIcon name="close" :size="16" /></button></div>
        <div class="composer-card">
          <Sender ref="sender" :value="draft" :loading="pending" :read-only="pending" :auto-size="{ minRows: 2, maxRows: 6 }" :placeholder="isLove ? '今天，有什么想说的吗？' : '告诉我，你想解决什么问题？'" submit-type="enter" aria-label="聊天消息" @change="value => draft = value" @submit="submit" @cancel="stop">
            <template #suffix><button v-if="pending" class="send-button stop-button" @click="stop" aria-label="停止接收回复" title="停止接收回复"><span class="stop-square"></span></button><button v-else class="send-button" :disabled="!draft.trim() || draft.length > 800" @click="submit(draft)" aria-label="发送消息"><AppIcon name="send" :size="21" /></button></template>
            <template #footer><div class="composer-meta"><span><AppIcon name="spark" :size="14" />{{ pending ? '正在为你组织回答…' : 'DeepSeek Flash' }}</span><span :class="{ 'over-limit': draft.length > 800 }">{{ draft.length ? `${draft.length} / 800` : 'Enter 发送 · Shift + Enter 换行' }}</span></div></template>
          </Sender>
        </div>
        <p class="disclaimer">AI 建议仅供参考，不替代专业心理咨询。<button @click="showInfo = !showInfo">了解数据使用方式</button></p>
        <span class="sr-only" role="status" aria-live="polite">{{ notice || (pending ? '正在生成回复' : '') }}</span>
      </div>
    </main>
  </div>
</template>
