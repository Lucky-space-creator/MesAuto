<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <el-form :inline="true" :model="query">
          <el-form-item label="用户名"><el-input v-model="query.username" placeholder="模糊查询" clearable /></el-form-item>
          <el-form-item label="姓名"><el-input v-model="query.realName" placeholder="模糊查询" clearable /></el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="success" :icon="Plus" @click="openDialog()">新增用户</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="page.pageNum" v-model:page-size="page.pageSize"
      :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadData" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="密码" v-if="!form.id"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="选择角色" style="width: 100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const query = reactive({ username: '', realName: '' })

const dialogVisible = ref(false)
const roles = ref([])
const form = reactive({ id: null, username: '', password: '', realName: '', phone: '', email: '', status: 1, roleIds: [] })

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.post('/system/user/page', {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      condition: { username: query.username || null, realName: query.realName || null }
    })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.username = ''
  query.realName = ''
  page.pageNum = 1
  loadData()
}

const loadRoles = async () => {
  roles.value = await request.get('/system/role/all')
}

const openDialog = async (row) => {
  if (row) {
    const detail = await request.get(`/system/user/${row.id}`)
    const roleIds = await request.get(`/system/user/${row.id}/roles`)
    Object.assign(form, { ...detail, password: '', roleIds: roleIds || [] })
  } else {
    Object.assign(form, { id: null, username: '', password: '', realName: '', phone: '', email: '', status: 1, roleIds: [] })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (form.id) {
    await request.put(`/system/user/${form.id}`, form)
  } else {
    await request.post('/system/user', form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '提示', { type: 'warning' })
  await request.delete(`/system/user/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => {
  loadData()
  loadRoles()
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
