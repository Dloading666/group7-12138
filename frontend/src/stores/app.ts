import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const pageLoading = ref(false)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setPageLoading(value: boolean) {
    pageLoading.value = value
  }

  return {
    sidebarCollapsed,
    pageLoading,
    toggleSidebar,
    setPageLoading
  }
})

