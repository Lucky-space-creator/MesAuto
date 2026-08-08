<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <el-form :inline="true" :model="query">
          <el-form-item label="单号"><el-input v-model="query.reqNo" placeholder="模糊查询" clearable /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.reqStatus" placeholder="全部" clearable style="width:120px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="审批中" value="APPROVING" />
              <el-option label="已通过" value="APPROVED" />
              <el-option label="已驳回" value="REJECTED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="success" :icon="Plus" @click="openDialog()" v-if="can('purchase:add')">新增采购申请</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="reqNo" label="申请单号" width="160" />
      <el-table-column prop="title" label="主题" min-width="140" show-overflow-tooltip />
      <el-table-column prop="materialName" label="采购物料" />
      <el-table-column prop="planQty" label="数量" width="100" />
      <el-table-column prop="unitName" label="单位" width="80" />
      <el-table-column prop="expectDate" label="期望到货" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.reqStatus)">{{ statusText(row.reqStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-if="['DRAFT','REJECTED'].includes(row.reqStatus) && can('purchase:submit')" link type="success" :icon="Promotion" @click="submitApproval(row)">提交审批</el-button>
          <el-button link type="primary" :icon="Edit" @click="openDialog(row)" v-if="can('purchase:add')">编辑</el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(row)" v-if="can('purchase:del')">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="page.pageNum" v-model:page-size="page.pageSize"
      :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="loadData" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑采购申请' : '新增采购申请'" width="540px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="主题"><el-input v-model="form.title" placeholder="如：8月钢板采购" /></el-form-item>
        <el-form-item label="采购物料">
          <el-select v-model="form.materialId" filterable placeholder="选择物料" style="width:100%">
            <el-option v-for="m in materials" :key="m.id" :label="`${m.materialName}(${m.materialCode})`" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input v-model.number="form.planQty" type="number" /></el-form-item>
        <el-form-item label="单位">
          <el-select v-model="form.unitId" filterable placeholder="选择单位" style="width:100%">
            <el-option v-for="u in units" :key="u.id" :label="`${u.unitName}(${u.unitCode})`" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="期望到货"><el-date-picker v-model="form.expectDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
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
import { Search, Refresh, Plus, Promotion, Edit, Delete } from '@element-plus/icons-vue'
import { usePerm } from '@/composables/usePerm'

const { can } = usePerm()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })
const query = reactive({ reqNo: '', reqStatus: '' })

const dialogVisible = ref(false)
const form = reactive({ id: null, title: '', materialId: null, planQty: 1, unitId: null, expectDate: '', remark: '' })
const materials = ref([])
const units = ref([])

const statusText = (s) => ({ DRAFT: '草稿', APPROVING: '审批中', APPROVED: '已通过', REJECTED: '已驳回' }[s] || s)
const statusTag = (s) => ({ DRAFT: 'info', APPROVING: 'warning', APPROVED: 'success', REJECTED: 'danger' }[s] || 'info')

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.post('/purchase/requisition/page', {
      pageNum: page.pageNum, pageSize: page.pageSize,
      condition: { reqNo: query.reqNo || null, reqStatus: query.reqStatus || null }
    })
    tableData.value = res.records
    total.value = res.total
  } finally { loading.value = false }
}

const loadMaterials = async () => { materials.value = await request.get('/material/page', { params: { pageNum: 1, pageSize: 1000 } }).then(r => r.records) }
const loadUnits = async () => { units.value = await request.get('/unit/all') }

const resetQuery = () => { query.reqNo = ''; query.reqStatus = ''; page.pageNum = 1; loadData() }

const openDialog = (row) => {
  Object.assign(form, row ? { ...row } : { id: null, title: '', materialId: null, planQty: 1, unitId: null, expectDate: '', remark: '' })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (form.id) await request.put(`/purchase/requisition/${form.id}`, form)
  else await request.post('/purchase/requisition', form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const submitApproval = async (row) => {
  const { value } = await ElMessageBox.prompt('请输入申请人', '提交审批', { inputValue: 'admin' }).catch(() => ({ value: null }))
  if (value === null) return
  await request.post(`/purchase/requisition/${row.id}/submit?applicant=${encodeURIComponent(value)}`)
  ElMessage.success('已提交审批')
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除采购申请「${row.reqNo}」？`, '提示', { type: 'warning' })
  await request.delete(`/purchase/requisition/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => { loadData(); loadMaterials(); loadUnits() })
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
