<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <el-form :inline="true" :model="query">
          <el-form-item label="订单号">
            <el-input v-model="query.orderNo" placeholder="模糊查询" clearable style="width: 160px" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.orderStatus" placeholder="全部" clearable style="width: 140px">
              <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="来源">
            <el-select v-model="query.sourceType" placeholder="全部" clearable style="width: 130px">
              <el-option label="手工创建" value="MANUAL" />
              <el-option label="ERP同步" value="ERP" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <div>
          <el-button type="success" :icon="Plus" @click="openDialog()">新建订单</el-button>
          <el-button type="warning" :icon="Link" @click="goErpSync">ERP同步</el-button>
        </div>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="orderNo" label="订单号" width="150" />
      <el-table-column prop="orderType" label="类型" width="90">
        <template #default="{ row }">
          <el-tag effect="plain">{{ orderTypeLabel(row.orderType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="materialName" label="产品" min-width="140" />
      <el-table-column prop="plannedQty" label="计划数量" width="90" />
      <el-table-column prop="completedQty" label="完工数量" width="90" />
      <el-table-column prop="orderStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.orderStatus)">{{ statusLabel(row.orderStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.sourceType === 'ERP'" type="warning" effect="plain">ERP</el-tag>
          <el-tag v-else effect="plain">手工</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="bomName" label="BOM" min-width="130" show-overflow-tooltip />
      <el-table-column prop="routeName" label="工艺路线" min-width="130" show-overflow-tooltip />
      <el-table-column prop="planStartDate" label="计划开始" width="115" />
      <el-table-column prop="planEndDate" label="计划结束" width="115" />
      <el-table-column prop="actualStartDate" label="实际开工" width="115" />
      <el-table-column prop="actualEndDate" label="实际完工" width="115" />
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
          <el-button link type="warning" :icon="Stamp"
            v-if="row.orderStatus === 'DRAFT'" @click="submitApproval(row)">提交审批</el-button>
          <el-button link type="success" :icon="Promotion"
            v-if="row.orderStatus === 'APPROVING'" @click="release(row)">审批通过下达</el-button>
          <el-button link type="success" :icon="VideoPlay"
            v-if="row.orderStatus === 'RELEASED'" @click="start(row)">开工</el-button>
          <el-button link type="success" :icon="Finished"
            v-if="row.orderStatus === 'IN_PRODUCTION'" @click="finish(row)">完工</el-button>
          <el-button link type="danger" :icon="CircleClose"
            v-if="['APPROVING','RELEASED','IN_PRODUCTION','PENDING_STORAGE'].includes(row.orderStatus)" @click="close(row)">关闭</el-button>
          <el-button link type="danger" :icon="Delete"
            v-if="row.orderStatus === 'DRAFT'" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新建 / 编辑 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑订单' : '新建订单'" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="订单号">
          <el-input v-model="form.orderNo" placeholder="留空自动生成" :disabled="!!form.id || form.sourceType === 'ERP'" />
        </el-form-item>
        <el-form-item label="订单类型">
          <el-select v-model="form.orderType" style="width: 100%" :disabled="!editable('orderType')">
            <el-option label="生产订单" value="PRODUCTION" />
            <el-option label="委外订单" value="OUTSOURCE" />
            <el-option label="返工订单" value="REWORK" />
          </el-select>
        </el-form-item>
        <el-form-item label="产品">
          <el-select v-model="form.materialId" filterable placeholder="选择物料" style="width: 100%"
            :disabled="!editable('materialId')">
            <el-option v-for="m in materials" :key="m.id" :label="`${m.materialCode} ${m.materialName}`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="BOM">
          <el-select v-model="form.bomId" filterable placeholder="选择BOM" style="width: 100%"
            :disabled="!editable('bomId')">
            <el-option v-for="b in boms" :key="b.id" :label="`${b.bomCode} ${b.bomName}`" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="工艺路线">
          <el-select v-model="form.routeId" filterable placeholder="选择工艺路线" style="width: 100%"
            :disabled="!editable('routeId')">
            <el-option v-for="r in routes" :key="r.id" :label="`${r.routeCode} ${r.routeName}`" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input v-model.number="form.plannedQty" type="number" :disabled="!editable('plannedQty')" />
        </el-form-item>
        <el-form-item label="单位">
          <el-select v-model="form.unitId" filterable placeholder="选择单位" style="width: 100%"
            :disabled="!editable('unitId')">
            <el-option v-for="u in units" :key="u.id" :label="u.unitName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" style="width: 100%" :disabled="!editable('priority')">
            <el-option label="普通" value="NORMAL" />
            <el-option label="加急" value="URGENT" />
            <el-option label="特急" value="RUSH" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划开始">
          <el-date-picker v-model="form.planStartDate" type="date" value-format="YYYY-MM-DD"
            :disabled="!editable('planStartDate')" style="width: 100%" />
        </el-form-item>
        <el-form-item label="计划结束">
          <el-date-picker v-model="form.planEndDate" type="date" value-format="YYYY-MM-DD"
            :disabled="!editable('planEndDate')" style="width: 100%" />
        </el-form-item>
        <el-form-item label="客户">
          <el-input v-model="form.customerName" :disabled="!editable('customerName')" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" :disabled="!editable('remark')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 详情（只读 + 操作记录） -->
    <el-dialog v-model="detailVisible" title="订单详情" width="680px">
      <el-descriptions :column="2" border v-if="detail">
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detail.orderStatus)">{{ statusLabel(detail.orderStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="订单类型">{{ orderTypeLabel(detail.orderType) }}</el-descriptions-item>
        <el-descriptions-item label="产品">{{ detail.materialName || detail.materialId }}</el-descriptions-item>
        <el-descriptions-item label="计划数量">{{ detail.plannedQty }}</el-descriptions-item>
        <el-descriptions-item label="完工数量">{{ detail.completedQty }}</el-descriptions-item>
        <el-descriptions-item label="优先级">{{ priorityLabel(detail.priority) }}</el-descriptions-item>
        <el-descriptions-item label="订单来源">
          <el-tag v-if="detail.sourceType === 'ERP'" type="warning">ERP同步</el-tag>
          <el-tag v-else>手工创建</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="BOM">{{ detail.bomName || detail.bomId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="工艺路线">{{ detail.routeName || detail.routeId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="计划开始">{{ detail.planStartDate }}</el-descriptions-item>
        <el-descriptions-item label="计划结束">{{ detail.planEndDate }}</el-descriptions-item>
        <el-descriptions-item label="实际开工">{{ detail.actualStartDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="实际完工">{{ detail.actualEndDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="上游ERP单号" :span="1">{{ detail.sourceNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="同步时间" :span="1">{{ detail.syncTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户" :span="2">{{ detail.customerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="1">{{ detail.createTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间" :span="1">{{ detail.updateTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">明细</el-divider>
      <el-table :data="items" border size="small">
        <el-table-column prop="materialName" label="物料" />
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, View, Stamp, Delete, Link,
         Promotion, VideoPlay, Finished, CircleClose } from '@element-plus/icons-vue'

const router = useRouter()

const statusOptions = [
  { value: 'DRAFT', label: '草稿' },
  { value: 'APPROVING', label: '审批中' },
  { value: 'RELEASED', label: '已下达' },
  { value: 'IN_PRODUCTION', label: '生产中' },
  { value: 'PENDING_STORAGE', label: '待入库' },
  { value: 'COMPLETED', label: '已完成' },
  { value: 'CLOSED', label: '已关闭' }
]
const statusLabel = (s) => (statusOptions.find(x => x.value === s)?.label || s)
const statusType = (s) => ({
  DRAFT: 'info', APPROVING: 'warning', RELEASED: 'primary',
  IN_PRODUCTION: 'success', PENDING_STORAGE: 'success', COMPLETED: '', CLOSED: 'danger'
}[s] || 'info')

const orderTypeOptions = [
  { value: 'PRODUCTION', label: '生产订单' },
  { value: 'OUTSOURCE', label: '委外订单' },
  { value: 'REWORK', label: '返工订单' }
]
const orderTypeLabel = (s) => (orderTypeOptions.find(x => x.value === s)?.label || s || '-')
const priorityLabel = (s) => ({ NORMAL: '普通', URGENT: '加急', RUSH: '特急' }[s] || s || '-')

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const query = reactive({ orderNo: '', orderStatus: '', sourceType: '' })

const materials = ref([])
const units = ref([])
const boms = ref([])
const routes = ref([])
const editableFields = ref([])

const dialogVisible = ref(false)
const form = reactive({ id: null, orderNo: '', orderType: 'PRODUCTION', materialId: null, plannedQty: 1,
  unitId: null, priority: 'NORMAL', bomId: null, routeId: null,
  planStartDate: '', planEndDate: '', customerName: '', remark: '', sourceType: 'MANUAL' })

const detailVisible = ref(false)
const detail = ref(null)
const items = ref([])

const editable = (field) => {
  // 新建时不限制
  if (!form.id) return true
  return editableFields.value.includes(field)
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.post('/order/page', {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      condition: {
        orderNo: query.orderNo || null,
        orderStatus: query.orderStatus || null,
        sourceType: query.sourceType || null
      }
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
  query.sourceType = ''
  page.pageNum = 1
  loadData()
}

const loadMaterials = async () => {
  materials.value = await request.post('/material/page', { pageNum: 1, pageSize: 999, condition: {} }).then(r => r.records)
}
const loadUnits = async () => {
  units.value = await request.get('/unit/all')
}
const loadBoms = async () => {
  boms.value = await request.post('/bom/page', { pageNum: 1, pageSize: 999, condition: {} }).then(r => r.records)
}
const loadRoutes = async () => {
  routes.value = await request.post('/process-route/page', { pageNum: 1, pageSize: 999, condition: {} }).then(r => r.records)
}

const openDialog = async () => {
  await Promise.all([loadMaterials(), loadUnits(), loadBoms(), loadRoutes()])
  Object.assign(form, { id: null, orderNo: '', orderType: 'PRODUCTION', materialId: null, plannedQty: 1, unitId: null,
    priority: 'NORMAL', bomId: null, routeId: null,
    planStartDate: '', planEndDate: '', customerName: '', remark: '', sourceType: 'MANUAL' })
  editableFields.value = []
  dialogVisible.value = true
}

const editOrder = async (row) => {
  await Promise.all([loadMaterials(), loadUnits(), loadBoms(), loadRoutes()])
  const detail = await request.get(`/order/${row.id}`)
  Object.assign(form, {
    id: detail.id, orderNo: detail.orderNo, orderType: detail.orderType, materialId: detail.materialId,
    plannedQty: detail.plannedQty, unitId: detail.unitId, priority: detail.priority,
    bomId: detail.bomId, routeId: detail.routeId,
    planStartDate: detail.planStartDate, planEndDate: detail.planEndDate,
    customerName: detail.customerName, remark: detail.remark, sourceType: detail.sourceType
  })
  editableFields.value = await request.get(`/order/${row.id}/editable-fields`)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (form.id) {
    await request.put(`/order/${form.id}`, form)
    ElMessage.success('保存成功')
  } else {
    const data = await request.post('/order', form)
    ElMessage.success('创建成功，订单号：' + (data?.orderNo || ''))
  }
  dialogVisible.value = false
  loadData()
}

const submitApproval = async (row) => {
  const { value } = await ElMessageBox.prompt('请输入申请人', '提交审批', { inputValue: 'admin' }).catch(() => ({ value: null }))
  if (value === null) return
  await request.post(`/order/${row.id}/submit?applicant=${encodeURIComponent(value)}`)
  ElMessage.success('已提交审批')
  loadData()
}
const release = async (row) => { await request.post(`/order/${row.id}/release`); ElMessage.success('已下达'); loadData() }
const start = async (row) => { await request.post(`/order/${row.id}/start`); ElMessage.success('已开工'); loadData() }
const finish = async (row) => { await request.post(`/order/${row.id}/finish-production`); ElMessage.success('已完工'); loadData() }
const close = async (row) => {
  await ElMessageBox.confirm(`确认关闭订单「${row.orderNo}」？`, '提示', { type: 'warning' })
  await request.post(`/order/${row.id}/close`); ElMessage.success('已关闭'); loadData()
}
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除订单「${row.orderNo}」？`, '提示', { type: 'warning' })
  await request.delete(`/order/${row.id}`); ElMessage.success('删除成功'); loadData()
}

const openDetail = async (row) => {
  detail.value = await request.get(`/order/${row.id}`)
  items.value = await request.get(`/order/${row.id}/items`)
  detailVisible.value = true
}

const goErpSync = () => router.push('/order/erp-sync')

onMounted(async () => {
  await loadData()
  const id = router.currentRoute.value.query.id
  if (id) {
    const row = tableData.value.find(r => String(r.id) === String(id))
    if (row) openDetail(row)
  }
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
