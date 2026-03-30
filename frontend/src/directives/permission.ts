import type { Directive } from 'vue'
import { useAuthStore } from '@/stores/auth'

export const permissionDirective: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    const authStore = useAuthStore()
    const codes = Array.isArray(binding.value) ? binding.value : [binding.value]
    const allowed = codes.some((code) => authStore.hasPermission(code))
    if (!allowed) {
      el.parentElement?.removeChild(el)
    }
  },
  updated(el, binding) {
    const authStore = useAuthStore()
    const codes = Array.isArray(binding.value) ? binding.value : [binding.value]
    const allowed = codes.some((code) => authStore.hasPermission(code))
    el.style.display = allowed ? '' : 'none'
  }
}

