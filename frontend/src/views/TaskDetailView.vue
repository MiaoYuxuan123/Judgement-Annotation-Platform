<template>
  <section class="panel" v-if="detail">
    <div class="toolbar">
      <div>
        <h3>{{ detail.summary.taskName }}</h3>
        <p class="muted">{{ detail.summary.description }}</p>
      </div>
      <el-button type="primary" @click="advance">推进阶段</el-button>
    </div>
    <el-steps :active="active" finish-status="success" style="margin-bottom: 18px">
      <el-step title="标注中" />
      <el-step title="待裁定" />
      <el-step title="可导出" />
    </el-steps>
    <div class="tag-row" style="margin-bottom: 16px">
      <el-tag>指南：{{ detail.configSnapshot.versionName }}</el-tag>
      <el-tag type="success">裁定者：{{ detail.reviewer.realName }}</el-tag>
      <el-tag type="warning">标注员：{{ detail.annotators.map(a => a.realName).join('、') }}</el-tag>
    </div>
    <el-table :data="detail.documents">
      <el-table-column prop="documentId" label="文书ID" width="110" />
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column prop="type" label="类型" width="120" />
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/annotate/${id}/${row.id}`)">进入标注</el-button>
          <el-button link type="warning" @click="$router.push(`/review/${id}`)">裁定</el-button>
          <el-button link type="success" @click="$router.push(`/results/${id}`)">结果</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import client from '../api/client'

const route = useRoute()
const id = route.params.id
const detail = ref(null)
const active = computed(() => ['标注中', '待裁定', '可导出'].indexOf(detail.value?.summary.status || '标注中'))

async function load() {
  detail.value = await client.get(`/tasks/${id}`)
}

async function advance() {
  detail.value = await client.put(`/tasks/${id}/stage`, {})
}

onMounted(load)
</script>
