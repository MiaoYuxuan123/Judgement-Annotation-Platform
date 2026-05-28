import axios from 'axios'
import { ElMessage } from 'element-plus'

const client = axios.create({
    baseURL: '/api',
    timeout: 10000
})

client.interceptors.request.use((config) => {
    const token = localStorage.getItem('jap_token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

client.interceptors.response.use(
    (response) => {
        const body = response.data
        if (body && typeof body.code === 'number' && body.code !== 200) {
            ElMessage.error(body.message || '请求失败')
            return Promise.reject(new Error(body.message || '请求失败'))
        }
        return body?.data ?? body
    },
    (error) => {
        const status = error.response?.status
        const message = error.response?.data?.message || error.message || '网络异常'
        // JWT 过期、签名无效或账号被禁用时清除本地会话
        if (status === 401) {
            localStorage.removeItem('jap_token')
            localStorage.removeItem('jap_user')
            if (!window.location.pathname.startsWith('/login')) {
                ElMessage.warning('登录已过期，请重新登录')
                window.location.assign('/login')
                return Promise.reject(error)
            }
        }
        ElMessage.error(message)
        return Promise.reject(error)
    }
)

export default client
