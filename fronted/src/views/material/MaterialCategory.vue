<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>物料分类</span>
        <el-button type="success" :icon="Plus" v-if="can('material:category:add')" @click="openDialog()">新增分类</el-button>
      </div>
    </template>
    <el-table :data="tableData" v-loading="loading" border stripe row-key="id" default-expand-all>
      <el-table-column prop="categoryName" label="分类名称" />
      <el-table-column prop="categoryCode" label="分类编码" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" v-if="can('material:category:edit')" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" :icon="Delete" v-if="can('material:category:del')" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="分类名称"><el-input v-model="form.categoryName" /></el-form-item>
        <el-form-item label="分类编码"><el-input v-model="form.categoryCode" /></el-form-item>
        <el-form-item label="父级ID"><el-input v-model.number="form.parentId" type="number" placeholder="0 表示顶级" /></el-form-item>
        <el-form-item label="排序"><el-input v-model.number="form.sort" type="number" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { usePerm } from '@/composables/usePerm'

const { can } = usePerm()

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, categoryName: '', categoryCode: '', parentId: 0, sort: 0, status: 1 })

const loadData = async () => {
  loading.value = true
  try {
    tableData.value = await request.get('/material/category/tree')
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  Object.assign(form, row ? { ...row } : { id: null, categoryName: '', categoryCode: '', parentId: 0, sort: 0, status: 1 })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (form.id) {
    await request.put(`/material/category/${form.id}`, form)
  } else {
    await request.post('/material/category', form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除分类「${row.categoryName}」？`, '提示', { type: 'warning' })
  await request.delete(`/material/category/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
</style>
