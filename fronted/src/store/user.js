import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    permissions: JSON.parse(localStorage.getItem('permissions') || '[]')
  }),
  getters: {
    isLoggedIn: (state) => !!state.token,
    // 是否拥有指定权限码（支持单个或多个，传数组时任一满足即可）
    hasPerm: (state) => (code) => {
      if (!code) return true
      if (Array.isArray(code)) return code.some(c => state.permissions.includes(c))
      return state.permissions.includes(code)
    }
  },
  actions: {
    async login(username, password) {
      const data = await request.post('/auth/login', { username, password })
      this.token = data.token
      this.userInfo = {
        userId: data.userId,
        username: data.username,
        realName: data.realName
      }
      this.permissions = Array.isArray(data.permissions) ? data.permissions : []
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      localStorage.setItem('permissions', JSON.stringify(this.permissions))
      return data
    },
    async refreshInfo() {
      if (!this.token) return
      try {
        const data = await request.get('/auth/info')
        this.token = data.token || this.token
        this.userInfo = {
          userId: data.userId,
          username: data.username,
          realName: data.realName
        }
        this.permissions = Array.isArray(data.permissions) ? data.permissions : []
        localStorage.setItem('token', this.token)
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
        localStorage.setItem('permissions', JSON.stringify(this.permissions))
      } catch (e) {
        // token过期等异常，清除登录状态
        this.token = ''
        this.userInfo = null
        this.permissions = []
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('permissions')
      }
    },
    async logout() {
      try {
        await request.post('/auth/logout')
      } catch (e) {
        // 忽略登出接口错误
      }
      this.token = ''
      this.userInfo = null
      this.permissions = []
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('permissions')
    }
  }
})
