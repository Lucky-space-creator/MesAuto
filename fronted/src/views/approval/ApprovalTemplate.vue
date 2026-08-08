<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>审批模板</span>
        <el-button type="success" :icon="Plus" @click="openDialog()">新增模板</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="templateName" label="模板名称" />
      <el-table-column prop="bizType" label="业务类型" width="140" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="isDefault" label="默认" width="80">
        <template #default="{ row }"><el-tag v-if="row.isDefault === 1" type="warning">默认</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click="viewNodes(row)">节点</el-button>
          <el-button link type="success" :icon="Promotion" v-if="row.status !== 'PUBLISHED'" @click="publish(row)">发布</el-button>
          <el-button link type="warning" :icon="Switch" v-if="row.status === 'PUBLISHED'" @click="toggle(row)">启停</el-button>
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

    <el-dialog v-model="dialogVisible" title="新增审批模板" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="模板名称"><el-input v-model="form.templateName" /></el-form-item>
        <el-form-item label="业务类型"><el-input v-model="form.bizType" placeholder="如 PURCHASE / ORDER" /></el-form-item>
        <el-form-item label="设为默认"><el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="nodesVisible" title="审批节点" width="640px">
      <el-table :data="nodes" border>
        <el-table-column prop="nodeSeq" label="顺序" width="70" />
        <el-table-column prop="nodeName" label="节点名称" />
        <el-table-column prop="assigneeType" label="审批人类型" width="110" />
        <el-table-column prop="assigneeExpr" label="审批人/表达式" min-width="160" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, View, Promotion, Switch, Delete } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })

const dialogVisible = ref(false)
const form = reactive({ id: null, templateName: '', bizType: '', isDefault: 0 })

const nodesVisible = ref(false)
const nodes = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.post('/approval-template/page', { pageNum: page.pageNum, pageSize: page.pageSize })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const openDialog = () => {
  Object.assign(form, { id: null, templateName: '', bizType: '', isDefault: 0 })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await request.post('/approval-template', form)
  ElMessage.success('创建成功')
  dialogVisible.value = false
  loadData()
}

const viewNodes = async (row) => {
  nodes.value = await request.get(`/approval-template/${row.id}/nodes`)
  nodesVisible.value = true
}

const publish = async (row) => {
  await request.post(`/approval-template/${row.id}/publish`)
  ElMessage.success('已发布')
  loadData()
}

const toggle = async (row) => {
  await request.post(`/approval-template/${row.id}/toggle`)
  ElMessage.success('状态已切换')
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除模板「${row.templateName}」？`, '提示', { type: 'warning' })
  await request.delete(`/approval-template/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
