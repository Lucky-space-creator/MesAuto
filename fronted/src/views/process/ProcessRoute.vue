<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <el-form :inline="true" :model="query">
          <el-form-item label="路线编码"><el-input v-model="query.routeCode" placeholder="模糊查询" clearable /></el-form-item>
          <el-form-item label="路线名称"><el-input v-model="query.routeName" placeholder="模糊查询" clearable /></el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="success" :icon="Plus" @click="openDialog()" v-if="can('process:route:add')">新增工艺路线</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="routeCode" label="路线编码" />
      <el-table-column prop="routeName" label="路线名称" />
      <el-table-column prop="materialName" label="适用产品" />
      <el-table-column prop="version" label="版本" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }"><el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click="viewSteps(row)">工序</el-button>
          <el-button link type="success" :icon="Promotion" v-if="row.status !== 'PUBLISHED' && can('process:route:publish')" @click="publish(row)">发布</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(row)" v-if="can('process:route:del')">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="page.pageNum" v-model:page-size="page.pageSize"
      :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadData" />

    <el-dialog v-model="dialogVisible" title="新增工艺路线" width="860px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="路线编码"><el-input v-model="form.routeCode" /></el-form-item>
        <el-form-item label="路线名称"><el-input v-model="form.routeName" /></el-form-item>
        <el-form-item label="适用产品">
          <el-select v-model="form.materialId" filterable placeholder="选择物料" style="width: 100%">
            <el-option v-for="m in materials" :key="m.id" :label="`${m.materialCode} ${m.materialName}`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="版本"><el-input v-model="form.version" placeholder="如 V1.0" /></el-form-item>
      </el-form>

      <el-divider content-position="left">工序步骤</el-divider>
      <div class="step-toolbar">
        <el-button type="primary" :icon="Plus" size="small" @click="addStep">添加工序</el-button>
      </div>
      <el-table :data="steps" border>
        <el-table-column label="顺序" width="70">
          <template #default="{ row, $index }"><el-input v-model.number="row.stepSeq" :placeholder="$index + 1" /></template>
        </el-table-column>
        <el-table-column label="工序名称" min-width="120">
          <template #default="{ row }"><el-input v-model="row.stepName" /></template>
        </el-table-column>
        <el-table-column label="工作中心ID" width="110">
          <template #default="{ row }"><el-input v-model.number="row.workCenterId" /></template>
        </el-table-column>
        <el-table-column label="工序类型" width="120">
          <template #default="{ row }"><el-input v-model="row.operationType" placeholder="如 加工" /></template>
        </el-table-column>
        <el-table-column label="标准工时" width="100">
          <template #default="{ row }"><el-input v-model.number="row.standardHours" /></template>
        </el-table-column>
        <el-table-column label="质检点" width="80">
          <template #default="{ row }">
            <el-switch v-model="row.qualityCheck" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{ $index }">
            <el-button link type="danger" :icon="Delete" @click="removeStep($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stepsVisible" title="工序步骤" width="640px">
      <el-table :data="steps" border>
        <el-table-column prop="stepSeq" label="顺序" width="70" />
        <el-table-column prop="stepName" label="工序名称" />
        <el-table-column prop="workCenterId" label="工作中心ID" width="110" />
        <el-table-column prop="operationType" label="工序类型" width="110" />
        <el-table-column prop="standardHours" label="标准工时" width="100" />
        <el-table-column prop="qualityCheck" label="质检点" width="80">
          <template #default="{ row }">{{ row.qualityCheck === 1 ? '是' : '否' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, View, Promotion, Delete } from '@element-plus/icons-vue'
import { usePerm } from '@/composables/usePerm'

const { can } = usePerm()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const query = reactive({ routeCode: '', routeName: '' })

const dialogVisible = ref(false)
const form = reactive({ id: null, routeCode: '', routeName: '', materialId: null, version: 'V1.0' })

const stepsVisible = ref(false)
const steps = ref([])

const blankStep = () => ({ stepSeq: steps.value.length + 1, stepName: '', workCenterId: null,
  operationType: '', standardHours: null, qualityCheck: 0 })

const addStep = () => steps.value.push(blankStep())
const removeStep = (i) => steps.value.splice(i, 1)

const materials = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.post('/process-route/page', {
      pageNum: page.pageNum,
      pageSize: page.pageSize,
      condition: { routeCode: query.routeCode || null, routeName: query.routeName || null }
    })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const loadMaterials = async () => {
  materials.value = await request.post('/material/page', { pageNum: 1, pageSize: 999, condition: {} }).then(r => r.records)
}

const resetQuery = () => {
  query.routeCode = ''
  query.routeName = ''
  page.pageNum = 1
  loadData()
}

const openDialog = () => {
  Object.assign(form, { id: null, routeCode: '', routeName: '', materialId: null, version: 'V1.0' })
  steps.value = []
  dialogVisible.value = true
}

const handleSubmit = async () => {
  // 仅保留已填写工序名称的步骤
  const payload = { ...form, steps: steps.value.filter(s => s.stepName) }
  const data = await request.post('/process-route', payload)
  ElMessage.success('创建成功，路线ID：' + (data?.id || ''))
  dialogVisible.value = false
  loadData()
}

const viewSteps = async (row) => {
  steps.value = await request.get(`/process-route/${row.id}/steps`)
  stepsVisible.value = true
}

const publish = async (row) => {
  await request.post(`/process-route/${row.id}/publish`)
  ElMessage.success('已发布')
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除工艺路线「${row.routeName}」？`, '提示', { type: 'warning' })
  await request.delete(`/process-route/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => { loadData(); loadMaterials() })
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
