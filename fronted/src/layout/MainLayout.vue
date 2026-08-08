<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">MES 控制台</div>
      <el-menu :default-active="activeMenu" router class="menu" background-color="#001529" text-color="#fff" active-text-color="#409eff">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon><span>工作台</span>
        </el-menu-item>
        <el-sub-menu index="material">
          <template #title><el-icon><Box /></el-icon><span>物料管理</span></template>
          <el-menu-item index="/material">物料列表</el-menu-item>
          <el-menu-item index="/material/category">物料分类</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="order">
          <template #title><el-icon><Document /></el-icon><span>订单管理</span></template>
          <el-menu-item index="/order">生产订单</el-menu-item>
          <el-menu-item index="/order/erp-sync">ERP订单同步</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="approval">
          <template #title><el-icon><Stamp /></el-icon><span>审批中心</span></template>
          <el-menu-item index="/approval/todo">待办审批</el-menu-item>
          <el-menu-item index="/approval/launch">发起审批</el-menu-item>
          <el-menu-item index="/approval/template">审批模板</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="purchase">
          <template #title><el-icon><ShoppingCart /></el-icon><span>采购管理</span></template>
          <el-menu-item index="/purchase/requisition">采购申请</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/warehouse">
          <el-icon><House /></el-icon><span>仓库管理</span>
        </el-menu-item>
        <el-menu-item index="/process">
          <el-icon><Operation /></el-icon><span>工艺路线</span>
        </el-menu-item>
        <el-menu-item index="/schedule">
          <el-icon><Calendar /></el-icon><span>生产排产</span>
        </el-menu-item>
        <el-sub-menu index="system">
          <template #title><el-icon><Setting /></el-icon><span>系统管理</span></template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/permission">权限管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-title">{{ currentTitle }}</div>
        <div class="header-right">
          <span class="user-name">{{ userName }}</span>
          <el-button text :icon="SwitchButton" @click="handleLogout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessageBox } from 'element-plus'
import { Odometer, Box, Document, Stamp, House, Operation, Calendar, Setting, SwitchButton, ShoppingCart } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || 'MES')
const userName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '未登录')

const handleLogout = async () => {
  await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' }).catch(() => Promise.reject())
  await userStore.logout()
  router.replace('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background-color: #001529;
  overflow: hidden;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  background-color: #002140;
}
.menu {
  border-right: none;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}
.header-title {
  font-size: 16px;
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-name {
  font-size: 14px;
  color: #666;
}
.main {
  background: #f0f2f5;
  padding: 16px;
}
</style>
