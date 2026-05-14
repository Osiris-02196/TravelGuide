<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()

onMounted(async () => {
  await loginUserStore.fetchLoginUser()
})
</script>