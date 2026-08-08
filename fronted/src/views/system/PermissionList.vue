<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>权限管理</span>
        <el-button type="success" :icon="Plus" @click="openDialog()">新增权限</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border stripe row-key="id" default-expand-all>
      <el-table-column prop="permissionName" label="权限名称" />
      <el-table-column prop="permissionCode" label="权限编码" />
      <el-table-column prop="permissionType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag>{{ row.permissionType === 'MENU' ? '菜单' : row.permissionType === 'BUTTON' ? '按钮' : '接口' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="permissionUrl" label="URL" />
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑权限' : '新增权限'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="权限名称"><el-input v-model="form.permissionName" /></el-form-item>
        <el-form-item label="权限编码"><el-input v-model="form.permissionCode" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.permissionType" style="width: 100%">
            <el-option label="菜单" value="MENU" />
            <el-option label="按钮" value="BUTTON" />
            <el-option label="接口" value="API" />
          </el-select>
        </el-form-item>
        <el-form-item label="URL"><el-input v-model="form.permissionUrl" /></el-form-item>
        <el-form-item label="父级ID"><el-input v-model.number="form.parentId" type="number" placeholder="0 表示顶级" /></el-form-item>
        <el-form-item label="排序"><el-input v-model.number="form.sort" type="number" /></el-form-item>
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
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, permissionName: '', permissionCode: '', permissionType: 'MENU', permissionUrl: '', parentId: 0, sort: 0 })

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await request.get('/system/permission/tree')
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  Object.assign(form, row ? { ...row } : { id: null, permissionName: '', permissionCode: '', permissionType: 'MENU', permissionUrl: '', parentId: 0, sort: 0 })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (form.id) {
    await request.put(`/system/permission/${form.id}`, form)
  } else {
    await request.post('/system/permission', form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除权限「${row.permissionName}」？`, '提示', { type: 'warning' })
  await request.delete(`/system/permission/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
</style>
