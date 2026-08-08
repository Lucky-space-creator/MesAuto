<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <el-form :inline="true" :model="query">
          <el-form-item label="物料编码">
            <el-input v-model="query.materialCode" placeholder="模糊查询" clearable />
          </el-form-item>
          <el-form-item label="物料名称">
            <el-input v-model="query.materialName" placeholder="模糊查询" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="success" :icon="Plus" @click="openDialog()">新增物料</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="materialCode" label="物料编码" />
      <el-table-column prop="materialName" label="物料名称" />
      <el-table-column prop="materialSpec" label="规格" />
      <el-table-column prop="unitName" label="单位" width="80" />
      <el-table-column prop="categoryName" label="分类" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pager"
      v-model:current-page="page.pageNum"
      v-model:page-size="page.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @change="loadData"
    />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑物料' : '新增物料'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="物料编码"><el-input v-model="form.materialCode" /></el-form-item>
        <el-form-item label="物料名称"><el-input v-model="form.materialName" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="form.materialSpec" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unitName" /></el-form-item>
        <el-form-item label="分类ID"><el-input v-model.number="form.categoryId" type="number" /></el-form-item>
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
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const query = reactive({ materialCode: '', materialName: '' })

const dialogVisible = ref(false)
const form = reactive({ id: null, materialCode: '', materialName: '', materialSpec: '', unitName: '', categoryId: null, status: 1 })

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.post('/material/page', {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      condition: { materialCode: query.materialCode || null, materialName: query.materialName || null }
    })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.materialCode = ''
  query.materialName = ''
  page.pageNum = 1
  loadData()
}

const openDialog = (row) => {
  Object.assign(form, row ? { ...row } : { id: null, materialCode: '', materialName: '', materialSpec: '', unitName: '', categoryId: null, status: 1 })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (form.id) {
    await request.put(`/material/${form.id}`, form)
  } else {
    await request.post('/material', form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除物料「${row.materialName}」？`, '提示', { type: 'warning' })
  await request.delete(`/material/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
