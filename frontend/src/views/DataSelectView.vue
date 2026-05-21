<template>
  <div v-if="detail" class="data-select-layout">
    <aside class="directory">
      <strong>数据条目目录</strong>
      <el-input v-model="keyword" placeholder="搜索数据ID / 来源" clearable style="margin: 12px 0" />
      <div v-for="doc in filteredDocs" :key="doc.id" class="dir-item">
        <span>D{{ doc.id }}</span>
        <small>{{ doc.title }}</small>
      </div>
    </aside>
    <section class="panel">
      <div class="toolbar">
        <div>
          <el-button text @click="$router.push('/tasks')">← 返回</el-button>
          <h3>ID-{{ id }} {{ detail.summary.taskName }} — 数据选择</h3>
          <p class="muted">仅展示当前用户有权限操作的数据条目。</p>
        </div>
        <el-select v-model="status" clearable placeholder="全部状态" style="width: 150px">
          <el-option label="待标注" value="待标注" />
          <el-option label="已标注" value="已标注" />
          <el-option label="待裁定" value="待裁定" />
          <el-option label="已裁定" value="已裁定" />
        </el-select>
      </div>
      <el-table :data="filteredDocs">
        <el-table-column label="数据ID" width="100"><template #default="{ row }">D{{ row.id }}</template></el-table-column>
        <el-table-column prop="title" label="来源（文件名/文本）" min-width="220" />
        <el-table-column label="当前身份" width="120"><template #default><el-tag>{{ roleText }}</el-tag></template></el-table-column>
        <el-table-column label="状态" width="110"><template #default><el-tag>{{ rowStatus }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="260">
          <template #default="{ row }">
            <el-button v-if="canAnnotate" type="warning" size="small" @click="$router.push(`/annotate/${id}/${row.id}`)">{{ rowStatus === '已标注' ? '继续标注' : '开始标注' }}</el-button>
            <el-button v-if="canReview" type="primary" size="small" @click="$router.push(`/review/${id}?dataId=${row.id}`)">开始裁定</el-button>
            <el-button v-if="detail.summary.status === '可导出'" type="success" size="small" @click="$router.push(`/results/${id}`)">查看结果</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import client from '../api/client'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const auth = useAuthStore()
const id = route.params.id
const detail = ref(null)
const keyword = ref('')
const status = ref('')

const canAnnotate = computed(() => detail.value?.annotators.some((u) => u.id === auth.user?.id))
const canReview = computed(() => detail.value?.reviewer.id === auth.user?.id)
const roleText = computed(() => canReview.value ? '裁决者' : '标注者')
const rowStatus = computed(() => {
  if (detail.value?.summary.status === '可导出') return '已裁定'
  if (canReview.value) return detail.value?.summary.status === '待裁定' ? '待裁定' : '待裁定'
  return '待标注'
})
const filteredDocs = computed(() => (detail.value?.documents || []).filter((doc) => {
  const matchKeyword = !keyword.value || `D${doc.id} ${doc.title}`.includes(keyword.value)
  const matchStatus = !status.value || status.value === rowStatus.value
  return matchKeyword && matchStatus
}))

async function load() {
  detail.value = await client.get(`/tasks/${id}`)
}

onMounted(load)
</script>
