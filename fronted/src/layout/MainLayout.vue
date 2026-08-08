<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">MES 控制台</div>
      <el-menu :default-active="activeMenu" router class="menu" background-color="#001529" text-color="#fff" active-text-color="#409eff">
        <el-menu-item v-for="m in visibleMenus" :key="m.index" :index="m.index">
          <el-icon v-if="m.icon"><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
        <el-sub-menu v-for="g in visibleGroups" :key="g.index" :index="g.index">
          <template #title><el-icon v-if="g.icon"><component :is="g.icon" /></el-icon><span>{{ g.title }}</span></template>
          <el-menu-item v-for="m in g.children" :key="m.index" :index="m.index">
            <span>{{ m.title }}</span>
          </el-menu-item>
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

// 菜单定义：perm 为该菜单所需的权限码（无 perm 表示所有人可见，如工作台）
const menuGroups = [
  {
    index: 'material', title: '物料管理', icon: 'Box', perm: 'material',
    children: [
      { index: '/material', title: '物料列表', perm: 'material:list' },
      { index: '/material/category', title: '物料分类', perm: 'material:category' }
    ]
  },
  {
    index: 'order', title: '订单管理', icon: 'Document', perm: 'order',
    children: [
      { index: '/order', title: '生产订单', perm: 'order:list' },
      { index: '/order/erp-sync', title: 'ERP订单同步', perm: 'order:erp' }
    ]
  },
  {
    index: 'approval', title: '审批中心', icon: 'Stamp', perm: 'approval',
    children: [
      { index: '/approval/todo', title: '待办审批', perm: 'approval:todo' },
      { index: '/approval/launch', title: '发起审批', perm: 'approval:launch' },
      { index: '/approval/template', title: '审批模板', perm: 'approval:template' }
    ]
  },
  {
    index: 'purchase', title: '采购管理', icon: 'ShoppingCart', perm: 'purchase',
    children: [
      { index: '/purchase/requisition', title: '采购申请', perm: 'purchase:requisition' }
    ]
  },
  {
    index: 'system', title: '系统管理', icon: 'Setting', perm: 'system',
    children: [
      { index: '/system/user', title: '用户管理', perm: 'system:user' },
      { index: '/system/role', title: '角色管理', perm: 'system:role' },
      { index: '/system/permission', title: '权限管理', perm: 'system:perm' }
    ]
  }
]

const singleMenus = [
  { index: '/dashboard', title: '工作台', icon: 'Odometer' },
  { index: '/warehouse', title: '仓库管理', icon: 'House', perm: 'warehouse:inventory' },
  { index: '/process', title: '工艺路线', icon: 'Operation', perm: 'process:route' },
  { index: '/schedule', title: '生产排产', icon: 'Calendar', perm: 'schedule:plan' }
]

const iconMap = { Odometer, Box, Document, Stamp, House, Operation, Calendar, Setting, ShoppingCart }

const canShow = (perm) => !perm || userStore.hasPerm(perm)

const visibleGroups = computed(() =>
  menuGroups
    .filter(g => canShow(g.perm))
    .map(g => ({ ...g, icon: iconMap[g.icon], children: g.children.filter(c => canShow(c.perm)) }))
    .filter(g => g.children.length > 0)
)
const visibleMenus = computed(() =>
  singleMenus.filter(m => canShow(m.perm)).map(m => ({ ...m, icon: iconMap[m.icon] }))
)

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
