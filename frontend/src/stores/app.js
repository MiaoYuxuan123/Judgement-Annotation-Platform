import { defineStore } from 'pinia'
import client from '../api/client'

export const useAppStore = defineStore('app', {
  state: () => ({
    tasks: [],
    documents: [],
    users: [],
    configs: []
  }),
  actions: {
    async refreshBase() {
      const user = JSON.parse(localStorage.getItem('jap_user') || 'null')
      const taskUrl = user?.canCreateTask ? '/tasks' : '/tasks/my'
      const [tasks, documents, users, configs] = await Promise.all([
        client.get(taskUrl),
        client.get('/documents'),
        client.get('/users'),
        client.get('/configs/versions')
      ])
      this.tasks = tasks.list || []
      this.documents = documents.list || []
      this.users = users
      this.configs = configs
    }
  }
})
