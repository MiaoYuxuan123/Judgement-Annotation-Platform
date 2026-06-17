<template>
  <el-popover
    :visible="visible"
    placement="bottom-end"
    :width="400"
    trigger="click"
    @show="loadMessages"
    @hide="visible = false"
  >
    <template #reference>
      <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99" class="message-badge">
        <el-button text @click="visible = !visible" class="message-bell-btn">
          <el-icon :size="20"><Bell /></el-icon>
        </el-button>
      </el-badge>
    </template>

    <div class="message-panel">
      <div class="message-panel-header">
        <span>消息中心</span>
        <div>
          <el-button v-if="messages.length && unreadCount" text size="small" type="primary" @click="markAllRead">
            全部已读
          </el-button>
          <el-button v-if="messages.some(m => m.read)" text size="small" type="danger" @click="deleteRead">
            清除已读
          </el-button>
        </div>
      </div>

      <div class="message-list" v-if="messages.length">
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="{ unread: !msg.read }"
          @click="openMessage(msg)"
        >
          <span class="message-dot" v-if="!msg.read"></span>
          <el-icon class="message-type-icon" :size="18">
            <component :is="typeIcon(msg.type)" />
          </el-icon>
          <div class="message-body">
            <div class="message-title">{{ msg.title }}</div>
            <div class="message-content">{{ msg.content }}</div>
            <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
          </div>
        </div>
      </div>

      <div v-else class="message-empty">暂无消息</div>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, List, Warning, Document } from '@element-plus/icons-vue'
import client from '../api/client'

const router = useRouter()
const visible = ref(false)
const messages = ref([])
const unreadCount = ref(0)

function typeIcon(type) {
  if (type === 'TASK') return List
  if (type === 'ARBITRATION') return Warning
  return Document
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function fetchUnreadCount() {
  try {
    const data = await client.get('/messages/unread-count')
    unreadCount.value = data.count || 0
  } catch (_) { /* ignore */ }
}

async function loadMessages() {
  const [listData, countData] = await Promise.all([
    client.get('/messages'),
    client.get('/messages/unread-count')
  ])
  messages.value = (listData.list || []).map(m => ({ ...m, read: !!m.isRead }))
  unreadCount.value = countData.count || 0
}

onMounted(() => {
  fetchUnreadCount()
})

let timer = null
timer = setInterval(fetchUnreadCount, 30000)
onUnmounted(() => clearInterval(timer))

async function openMessage(msg) {
  if (!msg.read) {
    await client.put(`/messages/${msg.id}/read`)
    msg.read = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  if (msg.taskId) {
    visible.value = false
    if (msg.dataId) {
      router.push(`/tasks/${msg.taskId}/data`)
    } else {
      router.push({ path: '/tasks', query: { taskId: msg.taskId, expand: '1' } })
    }
  }
}

async function markAllRead() {
  await client.put('/messages/read-all')
  messages.value.forEach(m => { m.read = true })
  unreadCount.value = 0
}

async function deleteRead() {
  await client.delete('/messages/read')
  messages.value = messages.value.filter(m => !m.read)
}
</script>

<style scoped>
.message-bell-btn { color: #fff; font-size: 20px; }
.message-panel { max-height: 480px; display: flex; flex-direction: column; }
.message-panel-header {
  display: flex; justify-content: space-between; align-items: center;
  padding-bottom: 12px; border-bottom: 1px solid #eee; margin-bottom: 8px;
  font-weight: 600;
}
.message-list { flex: 1; overflow-y: auto; }
.message-item {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 10px 8px; border-radius: 6px; cursor: pointer;
}
.message-item:hover { background: #f5f3f0; }
.message-item.unread { background: #fef8e7; }
.message-dot {
  width: 8px; height: 8px; border-radius: 50%; background: #409eff;
  flex-shrink: 0; margin-top: 6px;
}
.message-type-icon { flex-shrink: 0; margin-top: 4px; color: #909399; }
.message-body { flex: 1; min-width: 0; }
.message-title { font-weight: 600; font-size: 14px; }
.message-content { font-size: 13px; color: #555; margin-top: 2px; word-break: break-all; }
.message-time { font-size: 12px; color: #999; margin-top: 4px; }
.message-empty { text-align: center; color: #999; padding: 40px 0; }
</style>
