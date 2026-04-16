import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import EnterpriseInfo from '../views/EnterpriseInfo.vue'
import InvoiceQuery from '../views/InvoiceQuery.vue'
import ApplicationPage from '../views/ApplicationPage.vue'
import StabilityIndicator from '../views/StabilityIndicator.vue'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: Home
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/enterprise-info',
    name: 'EnterpriseInfo',
    component: EnterpriseInfo
  },
  {
    path: '/invoice-query',
    name: 'InvoiceQuery',
    component: InvoiceQuery
  },
  {
    path: '/application',
    name: 'ApplicationPage',
    component: ApplicationPage
  },
  {
    path: '/stability-indicator',
    name: 'StabilityIndicator',
    component: StabilityIndicator
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
