import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'material',
        name: 'Material',
        component: () => import('@/views/material/MaterialList.vue'),
        meta: { title: '物料管理' }
      },
      {
        path: 'material/category',
        name: 'MaterialCategory',
        component: () => import('@/views/material/MaterialCategory.vue'),
        meta: { title: '物料分类' }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理' }
      },
      {
        path: 'approval/todo',
        name: 'ApprovalTodo',
        component: () => import('@/views/approval/ApprovalTodo.vue'),
        meta: { title: '待办审批' }
      },
      {
        path: 'approval/template',
        name: 'ApprovalTemplate',
        component: () => import('@/views/approval/ApprovalTemplate.vue'),
        meta: { title: '审批模板' }
      },
      {
        path: 'warehouse',
        name: 'Warehouse',
        component: () => import('@/views/warehouse/WarehouseList.vue'),
        meta: { title: '仓库管理' }
      },
      {
        path: 'process',
        name: 'ProcessRoute',
        component: () => import('@/views/process/ProcessRoute.vue'),
        meta: { title: '工艺路线' }
      },
      {
        path: 'schedule',
        name: 'Schedule',
        component: () => import('@/views/schedule/ScheduleList.vue'),
        meta: { title: '生产排产' }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/RoleList.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'system/permission',
        name: 'SystemPermission',
        component: () => import('@/views/system/PermissionList.vue'),
        meta: { title: '权限管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
