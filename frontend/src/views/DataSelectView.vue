<template>
  <div v-if="detail" class="task-catalog-page data-select-page">
    <DataDirectorySidebar
      :documents="allDocs"
      :keyword="sidebarKeyword"
      :active-doc-id="activeDocId"
      @update:keyword="sidebarKeyword = $event"
      @select="selectSidebarDoc"
    />

    <main class="task-catalog-main">
      <div class="task-filter-bar">
        <div class="task-filter-row">
          <el-button text @click="$router.push('/tasks')">← 返回任务目录</el-button>
          <span class="task-filter-label">ID-{{ id }} {{ detail.summary.taskName }}</span>
          <span class="task-filter-spacer" />
          <span class="task-filter-label">筛选：</span>
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 150px">
            <el-option label="全部状态" value="" />
            <el-option label="标注中" value="标注中" />
            <el-option label="待裁定" value="待裁定" />
            <el-option label="可导出" value="可导出" />
          </el-select>
          <el-button type="primary" @click="applyFilters">筛选</el-button>
          <el-button @click="resetFilters">重置</el-button>
          <el-button
            v-if="canBatchExport"
            type="success"
            :disabled="!selectedExportCount || exporting"
            :loading="exporting"
            @click="batchExport"
          >
            批量导出 ZIP{{ selectedExportCount ? `（${selectedExportCount}）` : '' }}
          </el-button>
        </div>
        <div class="task-filter-row">
          <span class="task-filter-label">排序：</span>
          <el-select v-model="sortBy" style="width: 180px">
            <el-option label="数据ID（降序）" value="idDesc" />
            <el-option label="数据ID（升序）" value="idAsc" />
            <el-option label="标题（A→Z）" value="titleAsc" />
          </el-select>
          <span v-if="canBatchExport" class="task-export-hint">
            勾选「可导出」文书后点击「批量导出 ZIP」；操作列「查看结果/导出」可进入结果页查看并导出单篇。
          </span>
        </div>
      </div>

      <div class="task-table-wrap">
        <table class="task-table">
          <thead>
            <tr>
              <th v-if="canBatchExport" class="task-table-check-col">
                <el-checkbox
                  :model-value="allExportableSelected"
                  :indeterminate="someExportableSelected"
                  :disabled="!exportableRowsInView.length"
                  @change="toggleSelectAllExportable"
                />
              </th>
              <th>数据ID</th>
              <th>来源（文件名/文本）</th>
              <th>当前身份</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in displayRows"
              :key="row.id"
              class="task-row"
              :class="{ highlight: activeDocId === row.id }"
            >
              <td v-if="canBatchExport" class="task-table-check-col">
                <el-tooltip
                  v-if="!isRowExportable(row)"
                  :content="`当前为「${docStage(row)}」，仅「可导出」状态可批量导出`"
                  placement="top"
                >
                  <span class="task-check-disabled">
                    <el-checkbox :model-value="false" disabled />
                  </span>
                </el-tooltip>
                <el-checkbox
                  v-else
                  :model-value="selectedIds.has(row.id)"
                  @change="(checked) => toggleSelect(row.id, checked)"
                />
              </td>
              <td>D{{ row.id }}</td>
              <td>{{ row.title }}</td>
              <td>
                <span class="task-role-tags">
                  <span
                    v-for="r in viewerRoleDisplay"
                    :key="r.role"
                    class="task-role-tag"
                    :class="r.roleClass"
                  >{{ r.roleLabel }}</span>
                </span>
              </td>
              <td>
                <span class="task-status-tag" :class="statusClass(row)">{{ docStage(row) }}</span>
              </td>
              <td>
                <button
                  v-if="canAnnotate && docStage(row) === '标注中'"
                  class="task-action-btn orange"
                  @click="$router.push(`/annotate/${id}/${row.id}`)"
                >
                  {{ hasMyAnnotation(row) ? '继续标注' : '开始标注' }}
                </button>
                <button
                  v-if="canReview && docStage(row) === '待裁定'"
                  class="task-action-btn orange"
                  @click="$router.push(`/review/${id}?docId=${row.id}`)"
                >
                  开始裁定
                </button>
                <button
                  v-if="canViewResult(row)"
                  class="task-action-btn green"
                  @click="$router.push(`/results/${id}?dataId=${row.id}`)"
                >
                  查看结果/导出
                </button>
              </td>
            </tr>
            <tr v-if="!displayRows.length">
              <td :colspan="canBatchExport ? 6 : 5" class="task-empty">暂无数据条目</td>
            </tr>
          </tbody>
        </table>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElLoading, ElMessage } from 'element-plus'
import client from '../api/client'
import { useAuthStore } from '../stores/auth'
import DataDirectorySidebar from '../components/task/DataDirectorySidebar.vue'
import { exportTaskZipBatch } from '../utils/taskZipExport'
import { resolveDocStage, resolveTaskViewerRoles } from '../utils/taskRows'

const route = useRoute()
const auth = useAuthStore()
const id = route.params.id
const detail = ref(null)
const review = ref(null)
const sidebarKeyword = ref('')
const activeDocId = ref(null)
const sortBy = ref('idDesc')
const filters = reactive({ status: '' })
const selectedIds = ref(new Set())
const exporting = ref(false)

const canAnnotate = computed(() => detail.value?.annotators.some((u) => u.id === auth.user?.id))
const canReview = computed(() => detail.value?.reviewer.id === auth.user?.id)
const canBatchExport = computed(() => {
  return (
    auth.user?.canCreateTask ||
    canAnnotate.value ||
    canReview.value
  )
})
const viewerRoleDisplay = computed(() => resolveTaskViewerRoles(detail.value, auth.user))

const allDocs = computed(() => detail.value?.documents || [])

function getReviewEntry(row) {
  return review.value?.documents?.find((d) => d.document.id === row.id)
}

const viewerRole = computed(() => {
  if (canReview.value || auth.user?.canCreateTask) return 'reviewer'
  return 'annotator'
})

function docStage(row) {
  const annotatorCount = detail.value?.annotators?.length || 0
  return resolveDocStage(getReviewEntry(row), {
    annotatorCount,
    documentStatus: row.status,
    viewerRole: viewerRole.value,
    userId: auth.user?.id
  })
}

function statusClass(row) {
  const stage = docStage(row)
  if (stage === '可导出') return 'status-done'
  if (stage === '待裁定') return 'status-progress'
  return 'status-wait'
}

function canViewResult(row) {
  if (docStage(row) !== '可导出') return false
  return canAnnotate.value || canReview.value || auth.user?.canCreateTask
}

function hasMyAnnotation(row) {
  const entry = getReviewEntry(row)
  if (!entry || auth.user?.id == null) return false
  const mine = entry.annotatorResults?.find((r) => r.userId === auth.user.id)
  return Boolean(mine?.propositions?.length || mine?.relations?.length)
}

function isRowExportable(row) {
  return docStage(row) === '可导出'
}

const displayRows = computed(() => {
  let rows = [...allDocs.value]
  if (activeDocId.value != null) {
    rows = rows.filter((r) => r.id === activeDocId.value)
  }
  if (filters.status) {
    rows = rows.filter((r) => docStage(r) === filters.status)
  }
  rows.sort((a, b) => {
    if (sortBy.value === 'titleAsc') return (a.title || '').localeCompare(b.title || '')
    if (sortBy.value === 'idAsc') return a.id - b.id
    return b.id - a.id
  })
  return rows
})

const exportableRowsInView = computed(() => displayRows.value.filter((row) => isRowExportable(row)))

const allExportableSelected = computed(() => {
  const exportable = exportableRowsInView.value
  return exportable.length > 0 && exportable.every((row) => selectedIds.value.has(row.id))
})

const someExportableSelected = computed(() => {
  const exportable = exportableRowsInView.value
  const selectedInView = exportable.filter((row) => selectedIds.value.has(row.id))
  return selectedInView.length > 0 && selectedInView.length < exportable.length
})

const selectedExportCount = computed(() => {
  let count = 0
  for (const docId of selectedIds.value) {
    const row = allDocs.value.find((d) => d.id === docId)
    if (row && isRowExportable(row)) count += 1
  }
  return count
})

function toggleSelect(docId, checked) {
  const next = new Set(selectedIds.value)
  if (checked) next.add(docId)
  else next.delete(docId)
  selectedIds.value = next
}

function toggleSelectAllExportable(checked) {
  const next = new Set(selectedIds.value)
  for (const row of exportableRowsInView.value) {
    if (checked) next.add(row.id)
    else next.delete(row.id)
  }
  selectedIds.value = next
}

function selectSidebarDoc(docId) {
  activeDocId.value = docId
}

function applyFilters() {
  // client-side only
}

function resetFilters() {
  filters.status = ''
  sidebarKeyword.value = ''
  sortBy.value = 'idDesc'
  activeDocId.value = null
}

async function runExport(docIds, loadingText = '正在准备导出…') {
  if (!canBatchExport.value || exporting.value || !docIds.length) return

  exporting.value = true
  const loading = ElLoading.service({
    lock: true,
    text: loadingText,
    background: 'rgba(255,255,255,0.7)'
  })

  try {
    const { savedName, exportedCount, skippedCount } = await exportTaskZipBatch({
      review: review.value,
      taskDetail: detail.value,
      docIds,
      onProgress: (text) => {
        if (text) loading.setText(text)
      }
    })
    const suffix = skippedCount ? `（已跳过 ${skippedCount} 篇非「可导出」文书）` : ''
    ElMessage.success(`已导出 ${exportedCount} 篇：${savedName}${suffix}`)
  } catch (err) {
    if (err?.name === 'AbortError') return
    ElMessage.error(err?.message || '导出失败')
  } finally {
    loading.close()
    exporting.value = false
  }
}

async function batchExport() {
  if (!selectedExportCount.value) {
    ElMessage.warning('请先勾选「可导出」状态的文书')
    return
  }
  const docIds = [...selectedIds.value].filter((docId) => {
    const row = allDocs.value.find((d) => d.id === docId)
    return row && isRowExportable(row)
  })
  await runExport(docIds, '正在准备批量导出…')
}

async function load() {
  const [detailData, reviewData] = await Promise.all([
    client.get(`/tasks/${id}`),
    client.get(`/reviews/${id}`)
  ])
  detail.value = detailData
  review.value = reviewData
}

onMounted(load)
</script>

<style scoped>
.task-table-check-col {
  width: 44px;
  text-align: center;
  padding-left: 10px !important;
  padding-right: 10px !important;
}

.task-check-disabled {
  display: inline-flex;
  cursor: not-allowed;
}

.task-export-hint {
  margin-left: 8px;
  font-size: 13px;
  color: #6b7280;
}
</style>
