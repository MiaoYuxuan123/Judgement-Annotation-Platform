<template>
  <div>
    <div class="grid-4">
      <div class="metric"><span>任务总数</span><strong>{{ app.tasks.length }}</strong></div>
      <div class="metric"><span>文书总数</span><strong>{{ app.documents.length }}</strong></div>
      <div class="metric"><span>系统用户</span><strong>{{ app.users.length }}</strong></div>
      <div class="metric"><span>指南版本</span><strong>{{ app.configs.length }}</strong></div>
    </div>
    <div class="split">
      <section class="panel">
        <div class="toolbar">
          <h3>最近任务</h3>
          <el-button type="primary" @click="$router.push('/tasks')">创建 / 查看任务</el-button>
        </div>
        <el-table :data="app.tasks" empty-text="暂无任务">
          <el-table-column prop="taskName" label="任务名称" min-width="180" />
          <el-table-column prop="status" label="阶段" width="110">
            <template #default="{ row }"><el-tag>{{ row.status }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="documentCount" label="文书" width="80" />
          <el-table-column label="操作" width="110">
            <template #default="{ row }"><el-button link type="primary" @click="$router.push(`/tasks/${row.taskId}`)">进入</el-button></template>
          </el-table-column>
        </el-table>
      </section>
      <section class="panel">
        <h3>流程概览</h3>
        <el-steps direction="vertical" :active="2">
          <el-step title="创建任务" description="选择指南、文书、标注员和裁定者" />
          <el-step title="独立标注" description="命题、标签、关系与论证图同步生成" />
          <el-step title="冲突裁定" description="双结果并排展示，人工采纳或编辑" />
          <el-step title="结果导出" description="JSON/XLSX/PNG/SVG/ZIP 演示导出" />
        </el-steps>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAppStore } from '../stores/app'

const app = useAppStore()
onMounted(app.refreshBase)
</script>
