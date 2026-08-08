<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: card.color }">
            <el-icon><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-title">{{ card.title }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="welcome-card" shadow="never">
      <template #header>欢迎使用 MES 制造执行系统</template>
      <p>本系统涵盖物料、订单、工艺、排产、审批、仓库与系统管理等核心制造执行环节。</p>
      <p>请从左侧菜单进入对应模块。当前登录用户：<b>{{ userName }}</b></p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { Box, Document, Stamp, House } from '@element-plus/icons-vue'

const userStore = useUserStore()
const userName = computed(() => userStore.userInfo?.realName || '未登录')

const cards = ref([
  { title: '物料总数', value: '—', icon: Box, color: '#409eff' },
  { title: '订单总数', value: '—', icon: Document, color: '#67c23a' },
  { title: '待办审批', value: '—', icon: Stamp, color: '#e6a23c' },
  { title: '仓库数量', value: '—', icon: House, color: '#f56c6c' }
])
</script>

<style scoped>
.stat-card {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  margin-right: 16px;
}
.stat-value {
  font-size: 22px;
  font-weight: 700;
}
.stat-title {
  color: #909399;
  font-size: 13px;
}
.welcome-card {
  margin-top: 8px;
}
</style>
