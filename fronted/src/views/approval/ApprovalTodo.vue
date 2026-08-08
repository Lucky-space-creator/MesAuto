<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>待办审批（按当前登录用户的角色自动归并）</span>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </template>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="待办任务" name="todo">
        <el-table :data="todoList" v-loading="loading" border stripe>
          <el-table-column prop="bizType" label="业务类型" width="120" />
          <el-table-column label="业务单号" width="160">
            <template #default="{ row }">
              <span v-if="row.bizNo">{{ row.bizNo }}</span>
              <span v-else>{{ row.bizId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="nodeName" label="当前节点" />
          <el-table-column prop="assignee" label="处理人" width="120" />
          <el-table-column label="关联订单" width="160">
            <template #default="{ row }">
              <el-button v-if="row.bizType === 'ORDER'" link type="primary" @click="goOrder(row.bizId)">查看订单 {{ row.bizNo || row.bizId }}</el-button>
              <span v-else>{{ row.bizType }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }"><el-tag type="warning">待处理</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button link type="success" :icon="Select" @click="handleApprove(row)">通过</el-button>
              <el-button link type="danger" :icon="Close" @click="handleReject(row)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="已办记录" name="done">
        <el-table :data="doneList" v-loading="loadingDone" border stripe>
          <el-table-column prop="bizType" label="业务类型" width="120" />
          <el-table-column prop="action" label="动作" width="100">
            <template #default="{ row }">
              <el-tag :type="row.action === 'APPROVE' ? 'success' : 'danger'">{{ row.action === 'APPROVE' ? '通过' : '驳回' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="assignee" label="处理人" width="120" />
          <el-table-column prop="comment" label="意见" />
          <el-table-column prop="createTime" label="处理时间" width="180" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Select, Close } from '@element-plus/icons-vue'

const router = useRouter()
const activeTab = ref('todo')
const loading = ref(false)
const loadingDone = ref(false)
const todoList = ref([])
const doneList = ref([])

const loadData = async () => {
  loading.value = true
  try {
    todoList.value = await request.get('/approval/todo/mine')
  } finally {
    loading.value = false
  }
}

const loadDone = async () => {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (!userInfo.username) return
  loadingDone.value = true
  try {
    doneList.value = await request.get('/approval/done', { params: { assignee: userInfo.username } })
  } finally {
    loadingDone.value = false
  }
}

const handleApprove = async (row) => {
  const { value } = await ElMessageBox.prompt('审批意见（可选）', '通过审批', { inputRequired: false }).catch(() => ({ value: '' }))
  await request.post(`/approval/${row.id}/approve?assignee=${encodeURIComponent(row.assignee)}&comment=${encodeURIComponent(value || '')}`)
  ElMessage.success('已通过')
  loadData()
}

const handleReject = async (row) => {
  const { value } = await ElMessageBox.prompt('驳回意见', '驳回审批', { inputRequired: true }).catch(() => ({ value: '' }))
  if (!value) return
  await request.post(`/approval/${row.id}/reject?assignee=${encodeURIComponent(row.assignee)}&comment=${encodeURIComponent(value)}`)
  ElMessage.success('已驳回')
  loadData()
}

const goOrder = (id) => router.push(`/order?id=${id}`)

onMounted(() => {
  loadData()
  loadDone()
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
</style>
