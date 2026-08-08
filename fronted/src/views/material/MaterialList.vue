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
        <el-button type="success" :icon="Plus" @click="openDialog()" v-if="can('material:add')">新增物料</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="materialCode" label="物料编码" />
      <el-table-column prop="materialName" label="物料名称" />
      <el-table-column prop="materialSpec" label="规格" />
      <el-table-column prop="materialType" label="类型" width="90">
        <template #default="{ row }">
          <el-tag effect="plain">{{ {PRODUCT:'成品',SEMI:'半成品',RAW:'原材料',AUX:'辅材'}[row.materialType] || row.materialType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="drawingNo" label="图号" width="110" />
      <el-table-column prop="unitName" label="单位" width="80" />
      <el-table-column prop="categoryName" label="分类" />
      <el-table-column prop="warehouseName" label="默认仓库" width="110" />
      <el-table-column prop="locationCode" label="默认库位" width="110" />
      <el-table-column prop="minStock" label="最小库存" width="90" />
      <el-table-column prop="maxStock" label="最大库存" width="90" />
      <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="openDialog(row)" v-if="can('material:edit')">编辑</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(row)" v-if="can('material:del')">删除</el-button>
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
        <el-form-item label="图号"><el-input v-model="form.drawingNo" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.materialType" style="width:100%">
            <el-option label="成品" value="PRODUCT" />
            <el-option label="半成品" value="SEMI" />
            <el-option label="原材料" value="RAW" />
            <el-option label="辅材" value="AUX" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位">
          <el-select v-model="form.primaryUnitId" filterable placeholder="选择单位" style="width:100%">
            <el-option v-for="u in units" :key="u.id" :label="`${u.unitName}(${u.unitCode})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" filterable placeholder="选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认仓库">
          <el-select v-model="form.defaultWarehouseId" filterable placeholder="选择仓库" style="width:100%"
            @change="onWarehouseChange">
            <el-option v-for="w in warehouses" :key="w.id" :label="`${w.warehouseCode} ${w.warehouseName}`" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认库位">
          <el-select v-model="form.defaultLocationId" filterable placeholder="选择库位" style="width:100%" :disabled="!form.defaultWarehouseId">
            <el-option v-for="l in locations" :key="l.id" :label="`${l.locationCode}`" :value="l.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="最小库存"><el-input v-model.number="form.minStock" type="number" /></el-form-item>
        <el-form-item label="最大库存"><el-input v-model.number="form.maxStock" type="number" /></el-form-item>
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
import { usePerm } from '@/composables/usePerm'

const { can } = usePerm()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const query = reactive({ materialCode: '', materialName: '' })

const dialogVisible = ref(false)
const form = reactive({ id: null, materialCode: '', materialName: '', materialSpec: '', drawingNo: '',
  materialType: 'RAW', primaryUnitId: null, categoryId: null, minStock: 0, maxStock: 0,
  defaultWarehouseId: null, defaultLocationId: null, status: 1 })

const units = ref([])
const categories = ref([])
const warehouses = ref([])
const locations = ref([])

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

const loadUnits = async () => {
  units.value = await request.get('/unit/all')
}

const loadCategories = async () => {
  categories.value = await request.get('/material/category/all')
}

const loadWarehouses = async () => {
  warehouses.value = await request.get('/warehouse/all')
}

const onWarehouseChange = async (whId) => {
  form.defaultLocationId = null
  locations.value = whId ? await request.get(`/warehouse/${whId}/locations`) : []
}

const resetQuery = () => {
  query.materialCode = ''
  query.materialName = ''
  page.pageNum = 1
  loadData()
}

const openDialog = (row) => {
  const base = { id: null, materialCode: '', materialName: '', materialSpec: '', drawingNo: '',
    materialType: 'RAW', primaryUnitId: null, categoryId: null, minStock: 0, maxStock: 0,
    defaultWarehouseId: null, defaultLocationId: null, status: 1 }
  Object.assign(form, row ? { ...row } : base)
  if (row && row.defaultWarehouseId) {
    onWarehouseChange(row.defaultWarehouseId)
  } else {
    locations.value = []
  }
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

onMounted(() => { loadData(); loadUnits(); loadCategories(); loadWarehouses() })
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
