import { defineStore } from 'pinia'
import client from '../api/client'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('jap_token') || '',
    user: JSON.parse(localStorage.getItem('jap_user') || 'null')
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token && state.user)
  },
  actions: {
    async login(payload) {
      const data = await client.post('/auth/login', payload)
      this.token = data.token
      this.user = data.user
      localStorage.setItem('jap_token', data.token)
      localStorage.setItem('jap_user', JSON.stringify(data.user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('jap_token')
      localStorage.removeItem('jap_user')
    }
  }
})
