import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),
  getters: {
    isLoggedIn: (state) => !!state.token
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
      localStorage.setItem('token', data.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      return data
    },
    async logout() {
      try {
        await request.post('/auth/logout')
      } catch (e) {
        // 忽略登出接口错误
      }
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})
