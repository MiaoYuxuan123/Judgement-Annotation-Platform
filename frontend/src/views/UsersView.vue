<template>
  <section class="panel">
    <div class="toolbar">
      <h3>用户管理</h3>
      <el-button type="primary" @click="open()">新增用户</el-button>
    </div>
    <el-table :data="users">
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="role" label="角色">
        <template #default="{ row }"><el-tag>{{ roleText(row.role) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="visible" title="用户信息" width="460px">
      <el-form :model="form" label-position="top">
        <el-form-item label="账号"><el-input v-model="form.username" :disabled="Boolean(form.id)" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="超级管理员" value="admin" />
            <el-option label="任务创建者" value="creator" />
            <el-option label="标注员" value="annotator" />
            <el-option label="裁定者" value="reviewer" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" placeholder="默认 123456" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import client from '../api/client'

const users = ref([])
const visible = ref(false)
const form = reactive({})

const roleText = (role) => ({ admin: '超级管理员', creator: '任务创建者', annotator: '标注员', reviewer: '裁定者' }[role] || role)

async function load() {
  users.value = await client.get('/users')
}

function open(row) {
  Object.keys(form).forEach((key) => delete form[key])
  Object.assign(form, row || { username: '', realName: '', role: 'annotator', password: '123456', canCreateTask: false })
  visible.value = true
}

async function save() {
  if (form.id) await client.put(`/users/${form.id}`, form)
  else await client.post('/users', form)
  visible.value = false
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确认删除 ${row.realName}？`, '删除用户')
  await client.delete(`/users/${row.id}`)
  load()
}

onMounted(load)
</script>
