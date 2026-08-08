<template>
  <el-card>
    <template #header><span>发起审批</span></template>
    <el-alert type="info" :closable="false" show-icon title="说明"
      description="选择一个已发布的审批模板（业务类型），填写对应的业务单号与申请人，即可主动发起一条审批流程。业务单号需为对应业务表中真实存在的主键 ID。" />
    <el-form :model="form" label-width="110px" style="max-width:560px;margin-top:20px">
      <el-form-item label="业务类型" required>
        <el-select v-model="form.bizType" filterable placeholder="选择审批模板" style="width:100%"
          @change="onBizTypeChange">
          <el-option v-for="t in templates" :key="t.id" :label="`${t.templateName}（${t.bizType}）`" :value="t.bizType" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务单号" required>
        <el-input v-model="form.bizId" placeholder="对应业务表的主键 ID，如订单 ID=3" />
      </el-form-item>
      <el-form-item label="申请人" required>
        <el-input v-model="form.applicant" placeholder="如 admin" />
      </el-form-item>
      <el-form-item label="审批预览" v-if="previewNodes.length">
        <el-steps :active="0" align-center>
          <el-step v-for="n in previewNodes" :key="n.nodeSeq" :title="n.nodeName"
            :description="`${n.assigneeType}#${n.assigneeId}`" />
        </el-steps>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Promotion" :loading="submitting" @click="handleLaunch">发起审批</el-button>
        <el-button :icon="View" @click="preview">预览审批流</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'
import { Promotion, View } from '@element-plus/icons-vue'

const templates = ref([])
const previewNodes = ref([])
const submitting = ref(false)
const form = reactive({ bizType: '', bizId: '', applicant: 'admin' })

const loadTemplates = async () => {
  const res = await request.post('/approval-template/page', { pageNum: 1, pageSize: 1000 })
  templates.value = (res.records || []).filter(t => t.status === 'PUBLISHED')
}

const onBizTypeChange = () => { previewNodes.value = [] }

const preview = async () => {
  if (!form.bizType) return ElMessage.warning('请先选择业务类型')
  previewNodes.value = await request.get('/approval/route/preview', { params: { bizType: form.bizType } })
}

const handleLaunch = async () => {
  if (!form.bizType || !form.bizId || !form.applicant) return ElMessage.warning('请填写业务类型、业务单号与申请人')
  submitting.value = true
  try {
    await request.post(`/approval/${form.bizType}/${form.bizId}?applicant=${encodeURIComponent(form.applicant)}`)
    ElMessage.success('审批已发起，可在「待办审批」中处理')
    previewNodes.value = []
  } finally { submitting.value = false }
}

onMounted(loadTemplates)
</script>

<style scoped>
.el-step { flex: 1; }
</style>
