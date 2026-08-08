<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <el-radio-group v-model="activeTab">
          <el-radio-button value="warehouse">仓库</el-radio-button>
          <el-radio-button value="inventory">库存</el-radio-button>
          <el-radio-button value="inbound">入库单</el-radio-button>
          <el-radio-button value="outbound">出库单</el-radio-button>
        </el-radio-group>
        <el-button v-if="activeTab === 'warehouse'" type="success" :icon="Plus" @click="openWhDialog()">新增仓库</el-button>
      </div>
    </template>

    <!-- 仓库 -->
    <template v-if="activeTab === 'warehouse'">
      <el-table :data="warehouses" v-loading="loading" border stripe>
        <el-table-column prop="warehouseCode" label="仓库编码" />
        <el-table-column prop="warehouseName" label="仓库名称" />
        <el-table-column prop="warehouseType" label="类型" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="viewLocations(row)">库位</el-button>
            <el-button link type="danger" :icon="Delete" @click="deleteWh(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 库存 -->
    <template v-else-if="activeTab === 'inventory'">
      <el-table :data="inventoryData" v-loading="loadingInv" border stripe>
        <el-table-column prop="materialId" label="物料ID" width="100" />
        <el-table-column prop="warehouseId" label="仓库ID" width="100" />
        <el-table-column prop="quantity" label="数量" />
        <el-table-column prop="availableQty" label="可用数量" />
        <el-table-column prop="lockedQty" label="锁定数量" />
        <el-table-column prop="batchNo" label="批次" />
      </el-table>
      <el-pagination class="pager" v-model:current-page="invPage.pageNum" v-model:page-size="invPage.pageSize"
        :total="invTotal" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadInventory" />
    </template>

    <!-- 入库单 -->
    <template v-else-if="activeTab === 'inbound'">
      <el-table :data="inboundData" v-loading="loadingIn" border stripe>
        <el-table-column prop="orderNo" label="单号" />
        <el-table-column prop="warehouseId" label="仓库ID" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="createTime" label="创建时间" />
      </el-table>
      <el-pagination class="pager" v-model:current-page="inPage.pageNum" v-model:page-size="inPage.pageSize"
        :total="inTotal" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadInbound" />
    </template>

    <!-- 出库单 -->
    <template v-else>
      <el-table :data="outboundData" v-loading="loadingOut" border stripe>
        <el-table-column prop="orderNo" label="单号" />
        <el-table-column prop="warehouseId" label="仓库ID" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="createTime" label="创建时间" />
      </el-table>
      <el-pagination class="pager" v-model:current-page="outPage.pageNum" v-model:page-size="outPage.pageSize"
        :total="outTotal" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadOutbound" />
    </template>

    <el-dialog v-model="whVisible" title="新增仓库" width="480px">
      <el-form :model="whForm" label-width="90px">
        <el-form-item label="仓库编码"><el-input v-model="whForm.warehouseCode" /></el-form-item>
        <el-form-item label="仓库名称"><el-input v-model="whForm.warehouseName" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="whForm.warehouseType" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="whForm.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="whVisible = false">取消</el-button>
        <el-button type="primary" @click="submitWh">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="locVisible" title="库位列表" width="560px">
      <el-table :data="locations" border>
        <el-table-column prop="locationCode" label="库位编码" />
        <el-table-column prop="locationName" label="库位名称" />
        <el-table-column prop="capacity" label="容量" />
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, View, Delete } from '@element-plus/icons-vue'

const activeTab = ref('warehouse')
const loading = ref(false)
const warehouses = ref([])

const whVisible = ref(false)
const whForm = reactive({ id: null, warehouseCode: '', warehouseName: '', warehouseType: '', status: 1 })

const locVisible = ref(false)
const locations = ref([])

const inventoryData = ref([])
const loadingInv = ref(false)
const invTotal = ref(0)
const invPage = reactive({ pageNum: 1, pageSize: 10 })

const inboundData = ref([])
const loadingIn = ref(false)
const inTotal = ref(0)
const inPage = reactive({ pageNum: 1, pageSize: 10 })

const outboundData = ref([])
const loadingOut = ref(false)
const outTotal = ref(0)
const outPage = reactive({ pageNum: 1, pageSize: 10 })

const loadWarehouses = async () => {
  loading.value = true
  try {
    warehouses.value = await request.get('/warehouse/all')
  } finally {
    loading.value = false
  }
}

const openWhDialog = () => {
  Object.assign(whForm, { id: null, warehouseCode: '', warehouseName: '', warehouseType: '', status: 1 })
  whVisible.value = true
}

const submitWh = async () => {
  await request.post('/warehouse', whForm)
  ElMessage.success('创建成功')
  whVisible.value = false
  loadWarehouses()
}

const deleteWh = async (row) => {
  await ElMessageBox.confirm(`确认删除仓库「${row.warehouseName}」？`, '提示', { type: 'warning' })
  await request.delete(`/warehouse/${row.id}`)
  ElMessage.success('删除成功')
  loadWarehouses()
}

const viewLocations = async (row) => {
  locations.value = await request.get(`/warehouse/${row.id}/locations`)
  locVisible.value = true
}

const loadInventory = async () => {
  loadingInv.value = true
  try {
    const res = await request.post('/inventory/page', { pageNum: invPage.pageNum, pageSize: invPage.pageSize })
    inventoryData.value = res.records
    invTotal.value = res.total
  } finally {
    loadingInv.value = false
  }
}

const loadInbound = async () => {
  loadingIn.value = true
  try {
    const res = await request.post('/inbound/page', { pageNum: inPage.pageNum, pageSize: inPage.pageSize })
    inboundData.value = res.records
    inTotal.value = res.total
  } finally {
    loadingIn.value = false
  }
}

const loadOutbound = async () => {
  loadingOut.value = true
  try {
    const res = await request.post('/outbound/page', { pageNum: outPage.pageNum, pageSize: outPage.pageSize })
    outboundData.value = res.records
    outTotal.value = res.total
  } finally {
    loadingOut.value = false
  }
}

onMounted(loadWarehouses)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
