<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <el-form :inline="true" :model="query">
          <el-form-item label="订单号">
            <el-input v-model="query.orderNo" placeholder="模糊查询" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.orderStatus" placeholder="全部" clearable style="width: 140px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="待审批" value="PENDING" />
              <el-option label="已下达" value="RELEASED" />
              <el-option label="生产中" value="PRODUCING" />
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="success" :icon="Plus" @click="openDialog()">新建订单</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="orderNo" label="订单号" />
      <el-table-column prop="materialName" label="产品" />
      <el-table-column prop="quantity" label="数量" width="90" />
      <el-table-column prop="orderStatus" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.orderStatus)">{{ statusLabel(row.orderStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="planStartDate" label="计划开始" />
      <el-table-column prop="planEndDate" label="计划结束" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click="viewItems(row)">明细</el-button>
          <el-button link type="warning" :icon="Stamp" v-if="row.orderStatus === 'PENDING'" @click="submitApproval(row)">提交审批</el-button>
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

    <el-dialog v-model="dialogVisible" title="新建订单" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="订单号"><el-input v-model="form.orderNo" placeholder="留空自动生成" /></el-form-item>
        <el-form-item label="产品名称"><el-input v-model="form.materialName" /></el-form-item>
        <el-form-item label="数量"><el-input v-model.number="form.quantity" type="number" /></el-form-item>
        <el-form-item label="计划开始"><el-date-picker v-model="form.planStartDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="计划结束"><el-date-picker v-model="form.planEndDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="itemsVisible" title="订单明细" width="640px">
      <el-table :data="items" border>
        <el-table-column prop="materialName" label="物料" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, View, Stamp, Delete } from '@element-plus/icons-vue'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const query = reactive({ orderNo: '', orderStatus: '' })

const dialogVisible = ref(false)
const form = reactive({ id: null, orderNo: '', materialName: '', quantity: 1, planStartDate: '', planEndDate: '' })

const itemsVisible = ref(false)
const items = ref([])

const statusLabel = (s) => ({ DRAFT: '草稿', PENDING: '待审批', RELEASED: '已下达', PRODUCING: '生产中', COMPLETED: '已完成', CLOSED: '已关闭' }[s] || s)
const statusType = (s) => ({ DRAFT: 'info', PENDING: 'warning', RELEASED: 'primary', PRODUCING: 'success', COMPLETED: '', CLOSED: 'danger' }[s] || 'info')

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.post('/order/page', {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      condition: { orderNo: query.orderNo || null, orderStatus: query.orderStatus || null }
    })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.orderNo = ''
  query.orderStatus = ''
  page.pageNum = 1
  loadData()
}

const openDialog = () => {
  Object.assign(form, { id: null, orderNo: '', materialName: '', quantity: 1, planStartDate: '', planEndDate: '' })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const data = await request.post('/order', form)
  ElMessage.success('创建成功，订单号：' + (data?.orderNo || ''))
  dialogVisible.value = false
  loadData()
}

const submitApproval = async (row) => {
  await ElMessageBox.prompt('请输入申请人', '提交审批', { inputValue: '' }).then(async ({ value }) => {
    await request.post(`/order/${row.id}/submit?applicant=${encodeURIComponent(value)}`)
    ElMessage.success('已提交审批')
    loadData()
  }).catch(() => {})
}

const viewItems = async (row) => {
  items.value = await request.get(`/order/${row.id}/items`)
  itemsVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除订单「${row.orderNo}」？`, '提示', { type: 'warning' })
  await request.delete(`/order/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
