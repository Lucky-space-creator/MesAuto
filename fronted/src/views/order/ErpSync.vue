<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <div>
          <el-tag type="info" effect="plain">模拟上游 ERP 将销售订单同步进入 MES，作为生产订单来源</el-tag>
        </div>
        <div>
          <el-input-number v-model="batchSize" :min="1" :max="10" controls-position="right" />
          <el-button type="primary" :icon="Refresh" :loading="syncing" @click="doSync">立即同步</el-button>
          <el-button :icon="View" @click="doPreview">预览上游报文</el-button>
        </div>
      </div>
    </template>

    <el-alert v-if="lastResult" :type="lastResult.skipped > 0 ? 'warning' : 'success'" :closable="false" show-icon style="margin-bottom: 16px">
      <template #title>
        本次同步：新增 {{ lastResult.created }} 条，跳过（已存在） {{ lastResult.skipped }} 条，共处理 {{ lastResult.total }} 条
      </template>
    </el-alert>

    <el-divider content-position="left">上游 ERP 待下发报文预览</el-divider>
    <el-table :data="previewList" border v-loading="previewing" stripe>
      <el-table-column prop="erpOrderNo" label="ERP单号" width="150" />
      <el-table-column prop="materialCode" label="物料编码" width="140" />
      <el-table-column prop="materialName" label="物料名称" min-width="140" />
      <el-table-column prop="quantity" label="数量" width="90" />
      <el-table-column prop="unitName" label="单位" width="80" />
      <el-table-column prop="planStartDate" label="计划开始" width="115" />
      <el-table-column prop="planEndDate" label="计划结束" width="115" />
      <el-table-column prop="customerName" label="客户" min-width="120" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Refresh, View } from '@element-plus/icons-vue'

const batchSize = ref(3)
const syncing = ref(false)
const previewing = ref(false)
const lastResult = ref(null)
const previewList = ref([])

const doSync = async () => {
  syncing.value = true
  try {
    const r = await request.post(`/erp/sync/order?batchSize=${batchSize.value}`)
    lastResult.value = r
    ElMessage.success('同步完成')
    previewList.value = []
  } finally {
    syncing.value = false
  }
}

const doPreview = async () => {
  previewing.value = true
  try {
    previewList.value = await request.get(`/erp/preview/order?batchSize=${batchSize.value}`)
  } finally {
    previewing.value = false
  }
}

onMounted(doPreview)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
</style>
