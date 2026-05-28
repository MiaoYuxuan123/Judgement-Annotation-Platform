import { defineStore } from 'pinia'
import client from '../api/client'
import { isJwtExpired } from '../utils/jwt'

const TOKEN_KEY = 'jap_token'
const USER_KEY = 'jap_user'

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

function clearStoredSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

function loadSession() {
  const token = localStorage.getItem(TOKEN_KEY) || ''
  const user = readStoredUser()
  if (!token || !user || isJwtExpired(token)) {
    clearStoredSession()
    return { token: '', user: null }
  }
  return { token, user }
}

export const useAuthStore = defineStore('auth', {
  state: () => loadSession(),
  getters: {
    isLoggedIn: (state) => Boolean(state.token && state.user && !isJwtExpired(state.token))
  },
  actions: {
    async login(payload) {
      const data = await client.post('/auth/login', payload)
      if (!data?.token || !data?.user) {
        throw new Error('登录响应异常')
      }
      if (isJwtExpired(data.token)) {
        throw new Error('服务器返回的令牌已过期')
      }
      this.token = data.token
      this.user = data.user
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(data.user))
    },
    logout() {
      this.token = ''
      this.user = null
      clearStoredSession()
    },
    ensureSession() {
      if (!this.isLoggedIn) {
        this.logout()
      }
    }
  }
})
