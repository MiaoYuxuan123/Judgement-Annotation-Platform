<template>
  <section class="panel guide-config-view">
    <div class="guide-config-header">
      <el-button text @click="goBack">← 返回</el-button>
      <h2>指南版本详情（只读）</h2>
    </div>

    <div v-if="loading" class="muted">加载中...</div>
    <template v-else-if="config">
      <div class="guide-meta">
        <h3>{{ config.versionName }}</h3>
        <p class="muted">{{ config.description || '暂无描述' }}</p>
        <p class="muted">创建日期：{{ config.createdAt || '—' }}</p>
      </div>

      <div class="config-table-section">
        <h4>一级标签</h4>
        <el-table :data="config.primaryTags" size="small">
          <el-table-column prop="shortName" label="简称" width="90" />
          <el-table-column prop="name" label="标签名称" min-width="160" />
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        </el-table>
      </div>

      <div class="config-table-section">
        <h4>二级标签</h4>
        <el-table :data="config.secondaryTags" size="small">
          <el-table-column prop="shortName" label="简称" width="90" />
          <el-table-column prop="name" label="标签名称" min-width="160" />
          <el-table-column prop="parentTag" label="所属一级" width="120" />
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        </el-table>
      </div>

      <div class="config-table-section">
        <h4>关系类型</h4>
        <el-table :data="config.relationTypes" size="small">
          <el-table-column prop="shortName" label="简称" width="90" />
          <el-table-column prop="name" label="关系名称" min-width="160" />
          <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        </el-table>
      </div>
    </template>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { navigateBackFromGuide } from '../utils/navigationReturn'
import client from '../api/client'

const route = useRoute()
const router = useRouter()
const config = ref(null)
const loading = ref(true)

function goBack() {
  navigateBackFromGuide(router, route)
}

onMounted(async () => {
  try {
    config.value = await client.get(`/configs/versions/${route.params.id}`)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.guide-config-view {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}

.guide-config-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.guide-config-header h2 {
  margin: 0;
  font-size: 20px;
}

.guide-meta h3 {
  margin: 0 0 8px;
}

.config-table-section {
  margin-top: 24px;
}

.config-table-section h4 {
  margin: 0 0 12px;
}

.muted {
  color: #6b7280;
}
</style>
