<template>
  <section class="panel">
    <div class="toolbar"><h3>用户管理</h3><el-button type="primary" @click="open()">新增用户</el-button></div>
    <el-table :data="users">
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="role" label="系统角色"><template #default="{ row }"><el-tag>{{ roleText(row) }}</el-tag></template></el-table-column>
      <el-table-column prop="canCreateTask" label="允许创建任务" width="130"><template #default="{ row }"><el-tag :type="row.canCreateTask ? 'success' : 'info'">{{ row.canCreateTask ? '是' : '否' }}</el-tag></template></el-table-column>
      <el-table-column label="状态"><template #default="{ row }"><el-tag :type="isOnline(row) ? 'success' : 'info'" size="small">{{ isOnline(row) ? '在线' : '离线' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="160"><template #default="{ row }"><el-button link type="primary" @click="open(row)">编辑</el-button><el-button link type="danger" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-dialog v-model="visible" title="用户信息" width="460px">
      <el-form :model="form" label-position="top">
        <el-form-item label="账号"><el-input v-model="form.username" :disabled="Boolean(form.id)" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="系统角色">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="超级管理员" value="admin" /><el-option label="任务创建者" value="creator" /><el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item label="允许创建任务"><el-switch v-model="form.canCreateTask" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" placeholder="默认 123456" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>
<script setup>
import { onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import client from '../api/client'
const users = ref([]), visible = ref(false), form = reactive({})
const roleText = (row) => row.role === 'admin' ? '超级管理员' : (row.canCreateTask ? '任务创建者' : '普通用户')
function isOnline(row) { if (!row.lastSeen) return false; return Date.now() - new Date(row.lastSeen).getTime() < 120000 }
async function load() { users.value = await client.get('/users') }
function open(row) { Object.keys(form).forEach((key) => delete form[key]); Object.assign(form, row || { username: '', realName: '', role: 'user', password: '123456', canCreateTask: false }); visible.value = true }
async function save() { if (form.id) await client.put(`/users/${form.id}`, form); else await client.post('/users', form); visible.value = false; load() }
async function remove(row) { await ElMessageBox.confirm(`确认删除 ${row.realName}？`, '删除用户', { confirmButtonText: '确认', cancelButtonText: '取消' }); await client.delete(`/users/${row.id}`); load() }
let timer = null
onMounted(() => { load(); timer = setInterval(load, 30000) })
onUnmounted(() => { clearInterval(timer) })
</script>