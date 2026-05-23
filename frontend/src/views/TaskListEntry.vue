<template>
  <CreatorTasksView v-if="auth.user?.canCreateTask" />
  <ParticipantTasksView v-else />
</template>

<script setup>
import { useAuthStore } from '../stores/auth'
import CreatorTasksView from './creator/CreatorTasksView.vue'
import ParticipantTasksView from './participant/ParticipantTasksView.vue'

const auth = useAuthStore()
</script>
