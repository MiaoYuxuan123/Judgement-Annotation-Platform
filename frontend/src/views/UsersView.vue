<template>
  <section class="panel">
    <div class="toolbar"><h3>用户列表</h3><div style="display:flex;gap:8px"><el-button @click="open()">新增用户</el-button><el-button type="primary" @click="openBatch">批量新增</el-button></div></div>
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
        <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="系统角色">
          <el-select v-model="form.role" style="width: 100%" :disabled="!roleEditable">
            <el-option v-for="opt in roleOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" placeholder="默认 123456" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
    <el-dialog v-model="batchVisible" title="批量新增用户" width="580px">
      <el-form label-position="top">
        <el-form-item>
          <template #label><span>每行一个用户，格式：<b>账号 姓名</b>（空格分隔）</span></template>
          <el-input v-model="batchText" type="textarea" :rows="10" placeholder="annotator3 张三&#10;annotator4 李四&#10;reviewer2 王五" />
        </el-form-item>
        <el-form-item label="统一角色">
          <el-select v-model="batchRole" style="width:100%">
            <el-option label="任务创建者" value="creator" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item label="统一密码"><el-input v-model="batchPassword" placeholder="默认 123456" /></el-form-item>
      </el-form>
      <div v-if="batchResults.length" style="margin-top:12px">
        <el-tag v-for="r in batchResults" :key="r.username" style="margin:2px" type="success">{{ r.username }} {{ r.realName }}</el-tag>
      </div>
      <template #footer>
        <el-button @click="batchVisible=false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="doBatchCreate">确认新增</el-button>
      </template>
    </el-dialog>
  </section>
</template>
<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import client from '../api/client'
import { useAuthStore } from '../stores/auth'
const users = ref([]), visible = ref(false), form = reactive({})
const auth = useAuthStore()
const batchVisible = ref(false), batchText = ref(''), batchRole = ref('user'), batchPassword = ref('123456')
const batchLoading = ref(false), batchResults = ref([])
const roleEditable = computed(() => !form.id || form.id !== auth.user?.id)
const roleOptions = computed(() => {
  if (form.id && form.id === auth.user?.id) {
    return [{ label: '超级管理员', value: 'admin' }]
  }
  return [
    { label: '任务创建者', value: 'creator' },
    { label: '普通用户', value: 'user' }
  ]
})
const roleText = (row) => row.role === 'admin' ? '超级管理员' : (row.role === 'creator' ? '任务创建者' : '普通用户')
function isOnline(row) { if (!row.lastSeen) return false; return Date.now() - new Date(row.lastSeen).getTime() < 120000 }
async function load() { users.value = await client.get('/users') }
function open(row) { Object.keys(form).forEach((key) => delete form[key]); Object.assign(form, row || { username: '', realName: '', role: 'user', password: '123456' }); visible.value = true }
async function save() { if (form.id) await client.put(`/users/${form.id}`, form); else await client.post('/users', form); visible.value = false; load() }
async function remove(row) { await ElMessageBox.confirm(`确认删除 ${row.realName}？`, '删除用户', { confirmButtonText: '确认', cancelButtonText: '取消' }); await client.delete(`/users/${row.id}`); load() }
function openBatch() { batchText.value = ''; batchRole.value = 'user'; batchPassword.value = '123456'; batchResults.value = []; batchVisible.value = true }
async function doBatchCreate() {
  const lines = batchText.value.split('\n').filter(l => l.trim())
  const users = lines.map(line => {
    const parts = line.split(/[\t,，\s]+/)
    return { username: parts[0] || '', realName: parts.slice(1).join(' ') || parts[0] }
  }).filter(u => u.username)
  if (users.length === 0) { ElMessage.warning('请按格式输入至少一个用户'); return }
  batchLoading.value = true
  try {
    const data = await client.post('/users/batch', { users, role: batchRole.value, password: batchPassword.value || '123456' })
    batchResults.value = data || []
    ElMessage.success(`成功创建 ${data.length} 个用户`)
    load()
  } finally { batchLoading.value = false }
}
let timer = null
onMounted(() => { load(); timer = setInterval(load, 30000) })
onUnmounted(() => { clearInterval(timer) })
</script>