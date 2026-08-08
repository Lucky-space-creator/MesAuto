<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <el-radio-group v-model="activeTab">
          <el-radio-button value="plan">生产计划</el-radio-button>
          <el-radio-button value="task">生产任务</el-radio-button>
          <el-radio-button value="station">工位</el-radio-button>
          <el-radio-button value="center">工作中心</el-radio-button>
        </el-radio-group>
        <el-button v-if="activeTab === 'station' && can('schedule:station:add')" type="success" :icon="Plus" @click="openStation()">新增工位</el-button>
        <el-button v-if="activeTab === 'center' && can('schedule:center:add')" type="success" :icon="Plus" @click="openCenter()">新增工作中心</el-button>
      </div>
    </template>

    <template v-if="activeTab === 'plan'">
      <el-table :data="planData" v-loading="loadingPlan" border stripe>
        <el-table-column prop="planNo" label="计划单号" width="150" />
        <el-table-column prop="orderNo" label="关联订单" min-width="150" show-overflow-tooltip />
        <el-table-column prop="orderId" label="订单ID" width="100" />
        <el-table-column prop="totalQty" label="总数量" width="100" />
        <el-table-column prop="completedQty" label="已完成" width="100" />
        <el-table-column prop="planDate" label="计划日期" width="120" />
        <el-table-column prop="planStatus" label="状态" width="120">
          <template #default="{ row }"><el-tag>{{ row.planStatus }}</el-tag></template>
        </el-table-column>
      </el-table>
      <el-pagination class="pager" v-model:current-page="planPage.pageNum" v-model:page-size="planPage.pageSize"
        :total="planTotal" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadPlan" />
    </template>

    <template v-else-if="activeTab === 'task'">
      <el-table :data="taskData" v-loading="loadingTask" border stripe>
        <el-table-column prop="taskNo" label="任务单号" width="150" />
        <el-table-column prop="orderNo" label="关联订单" width="130" show-overflow-tooltip />
        <el-table-column prop="materialName" label="物料" min-width="140" show-overflow-tooltip />
        <el-table-column prop="workstationName" label="工位" width="130" />
        <el-table-column prop="taskStatus" label="状态" width="110">
          <template #default="{ row }"><el-tag :type="taskType(row.taskStatus)">{{ row.taskStatus }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="plannedQty" label="计划数量" width="100" />
        <el-table-column prop="actualQty" label="实际数量" width="100" />
        <el-table-column prop="defectiveQty" label="不良数量" width="100" />
        <el-table-column prop="priority" label="优先级" width="90" />
        <el-table-column prop="planStartTime" label="计划开始" width="150" />
        <el-table-column prop="planEndTime" label="计划结束" width="150" />
        <el-table-column prop="actualStartTime" label="实际开始" width="150" />
        <el-table-column prop="actualEndTime" label="实际结束" width="150" />
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" :icon="VideoPlay" @click="startTask(row)">开工</el-button>
            <el-button link type="warning" :icon="EditPen" @click="reportTask(row)">报工</el-button>
            <el-button link type="primary" :icon="Select" @click="completeTask(row)">完成</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pager" v-model:current-page="taskPage.pageNum" v-model:page-size="taskPage.pageSize"
        :total="taskTotal" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadTask" />
    </template>

    <template v-else-if="activeTab === 'station'">
      <el-table :data="stations" v-loading="loadingStation" border stripe>
        <el-table-column prop="stationCode" label="工位编码" />
        <el-table-column prop="stationName" label="工位名称" />
        <el-table-column prop="workCenterId" label="工作中心ID" width="120" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
      </el-table>
    </template>

    <template v-else>
      <el-table :data="centers" v-loading="loadingCenter" border stripe>
        <el-table-column prop="centerCode" label="编码" />
        <el-table-column prop="centerName" label="名称" />
        <el-table-column prop="capacity" label="产能" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
      </el-table>
    </template>

    <el-dialog v-model="stationVisible" title="新增工位" width="480px">
      <el-form :model="stationForm" label-width="90px">
        <el-form-item label="工位编码"><el-input v-model="stationForm.stationCode" /></el-form-item>
        <el-form-item label="工位名称"><el-input v-model="stationForm.stationName" /></el-form-item>
        <el-form-item label="工作中心ID"><el-input v-model.number="stationForm.workCenterId" type="number" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="stationForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stationVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStation">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="centerVisible" title="新增工作中心" width="480px">
      <el-form :model="centerForm" label-width="90px">
        <el-form-item label="编码"><el-input v-model="centerForm.centerCode" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="centerForm.centerName" /></el-form-item>
        <el-form-item label="产能"><el-input v-model.number="centerForm.capacity" type="number" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="centerForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="centerVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCenter">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, VideoPlay, EditPen, Select } from '@element-plus/icons-vue'
import { usePerm } from '@/composables/usePerm'

const { can } = usePerm()

const activeTab = ref('plan')
const loadingPlan = ref(false)
const planData = ref([])
const planTotal = ref(0)
const planPage = reactive({ pageNum: 1, pageSize: 10 })

const loadingTask = ref(false)
const taskData = ref([])
const taskTotal = ref(0)
const taskPage = reactive({ pageNum: 1, pageSize: 10 })

const loadingStation = ref(false)
const stations = ref([])
const stationVisible = ref(false)
const stationForm = reactive({ id: null, stationCode: '', stationName: '', workCenterId: null, status: 1 })

const loadingCenter = ref(false)
const centers = ref([])
const centerVisible = ref(false)
const centerForm = reactive({ id: null, centerCode: '', centerName: '', capacity: 0, status: 1 })

const taskType = (s) => ({ PENDING: 'info', RUNNING: 'success', PAUSED: 'warning', COMPLETED: 'primary', CLOSED: 'danger' }[s] || 'info')

const operator = () => (JSON.parse(localStorage.getItem('userInfo') || '{}').username || 'admin')

const loadPlan = async () => {
  loadingPlan.value = true
  try {
    const res = await request.post('/schedule/plan/page', { pageNum: planPage.pageNum, pageSize: planPage.pageSize })
    planData.value = res.records
    planTotal.value = res.total
  } finally {
    loadingPlan.value = false
  }
}

const loadTask = async () => {
  loadingTask.value = true
  try {
    const res = await request.post('/schedule/task/page', { pageNum: taskPage.pageNum, pageSize: taskPage.pageSize })
    taskData.value = res.records
    taskTotal.value = res.total
  } finally {
    loadingTask.value = false
  }
}

const loadStations = async () => {
  loadingStation.value = true
  try {
    stations.value = await request.get('/schedule/workstation/all')
  } finally {
    loadingStation.value = false
  }
}

const loadCenters = async () => {
  loadingCenter.value = true
  try {
    centers.value = await request.get('/schedule/workcenter/all')
  } finally {
    loadingCenter.value = false
  }
}

const startTask = async (row) => {
  await request.post(`/schedule/task/${row.id}/start?operator=${encodeURIComponent(operator())}`)
  ElMessage.success('已开工')
  loadTask()
}

const reportTask = async (row) => {
  const { value: qty } = await ElMessageBox.prompt('报工数量', '报工', { inputValue: row.planQty || 0 }).catch(() => ({ value: null }))
  if (qty === null) return
  await request.post(`/schedule/task/${row.id}/report?qty=${encodeURIComponent(qty)}&operator=${encodeURIComponent(operator())}`)
  ElMessage.success('报工成功')
  loadTask()
}

const completeTask = async (row) => {
  await request.post(`/schedule/task/${row.id}/complete?operator=${encodeURIComponent(operator())}`)
  ElMessage.success('已完成')
  loadTask()
}

const openStation = () => {
  Object.assign(stationForm, { id: null, stationCode: '', stationName: '', workCenterId: null, status: 1 })
  stationVisible.value = true
}
const submitStation = async () => {
  await request.post('/schedule/workstation', stationForm)
  ElMessage.success('创建成功')
  stationVisible.value = false
  loadStations()
}

const openCenter = () => {
  Object.assign(centerForm, { id: null, centerCode: '', centerName: '', capacity: 0, status: 1 })
  centerVisible.value = true
}
const submitCenter = async () => {
  await request.post('/schedule/workcenter', centerForm)
  ElMessage.success('创建成功')
  centerVisible.value = false
  loadCenters()
}

onMounted(() => {
  loadPlan()
  loadTask()
  loadStations()
  loadCenters()
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
