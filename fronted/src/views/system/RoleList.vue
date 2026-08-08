<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>角色管理</span>
        <el-button type="success" :icon="Plus" v-if="can('system:role:add')" @click="openDialog()">新增角色</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="roleCode" label="角色编码" />
      <el-table-column prop="roleName" label="角色名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" v-if="can('system:role:edit')" @click="openDialog(row)">编辑</el-button>
          <el-button link type="warning" :icon="Setting" @click="openPerm(row)">授权</el-button>
          <el-button link type="danger" :icon="Delete" v-if="can('system:role:del')" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" v-model:current-page="page.pageNum" v-model:page-size="page.pageSize"
      :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadData" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="角色编码"><el-input v-model="form.roleCode" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="角色名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permVisible" title="角色授权" width="560px">
      <el-tree
        ref="permTree"
        :data="permTreeData"
        :props="{ label: 'permName', children: 'children' }"
        node-key="id"
        show-checkbox
        default-expand-all
      />
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPerm">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Setting, Delete } from '@element-plus/icons-vue'
import { usePerm } from '@/composables/usePerm'

const { can } = usePerm()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 10 })

const dialogVisible = ref(false)
const form = reactive({ id: null, roleCode: '', roleName: '', description: '', status: 1 })

const permVisible = ref(false)
const permTree = ref()
const permTreeData = ref([])
const permRoleId = ref(null)
const allPermIds = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.post('/system/role/page', { pageNum: page.pageNum, pageSize: page.pageSize })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const openDialog = (row) => {
  Object.assign(form, row ? { ...row } : { id: null, roleCode: '', roleName: '', description: '', status: 1 })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (form.id) {
    await request.put(`/system/role/${form.id}`, form)
  } else {
    await request.post('/system/role', form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadData()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除角色「${row.roleName}」？`, '提示', { type: 'warning' })
  await request.delete(`/system/role/${row.id}`)
  ElMessage.success('删除成功')
  loadData()
}

const openPerm = async (row) => {
  if (!permTreeData.value.length) {
    permTreeData.value = await request.get('/system/permission/tree')
  }
  if (!allPermIds.value.length) {
    const all = await request.get('/system/permission/all')
    allPermIds.value = all.map((p) => p.id)
  }
  permRoleId.value = row.id
  const checked = await request.get(`/system/role/${row.id}/permissions`)
  // 仅勾选叶子节点
  const leafChecked = checked.filter((id) => !allPermIds.value.includes(id) || isLeaf(id, permTreeData.value))
  permVisible.value = true
  await new Promise((r) => setTimeout(r, 100))
  permTree.value.setCheckedKeys(checked)
}

const isLeaf = (id, nodes) => {
  for (const n of nodes) {
    if (n.id === id) return !n.children || n.children.length === 0
    if (n.children && isLeaf(id, n.children)) return true
  }
  return false
}

const submitPerm = async () => {
  const checked = permTree.value.getCheckedKeys()
  await request.post(`/system/role/${permRoleId.value}`, { permissionIds: checked })
  ElMessage.success('授权成功')
  permVisible.value = false
}

onMounted(loadData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; }
.pager { margin-top: 16px; justify-content: flex-end; }
</style>
