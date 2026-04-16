<template>
  <div class="invoice-query">
    <div class="header">
      <div class="header-content">
        <div class="header-title">XX电子税务平台</div>
        <div class="header-nav">
          <router-link to="/">首页</router-link>
          <router-link to="/enterprise-info">企业信息查询</router-link>
          <router-link to="/application">业务申请</router-link>
          <router-link to="/stability-indicator">经营稳定性指标</router-link>
        </div>
      </div>
    </div>

    <div class="content">
      <div class="card">
        <h2 class="card-title">发票信息查询</h2>
        
        <div class="search-section">
          <div class="form-group">
            <label class="form-label">纳税人识别号</label>
            <input 
              type="text" 
              class="form-input" 
              v-model="searchForm.taxNo"
              placeholder="请输入纳税人识别号"
              id="invoice-tax-no"
            />
          </div>
          <div class="form-group">
            <label class="form-label">统一社会信用代码</label>
            <input 
              type="text" 
              class="form-input" 
              v-model="searchForm.uscCode"
              placeholder="请输入统一社会信用代码"
              id="invoice-usc-code"
            />
          </div>
          <div class="form-group">
            <label class="form-label">开票日期起</label>
            <input 
              type="date" 
              class="form-input" 
              v-model="searchForm.startDate"
              id="invoice-start-date"
            />
          </div>
          <div class="form-group">
            <label class="form-label">开票日期止</label>
            <input 
              type="date" 
              class="form-input" 
              v-model="searchForm.endDate"
              id="invoice-end-date"
            />
          </div>
          <button class="btn btn-primary" @click="searchInvoices">查询</button>
        </div>

        <div v-if="invoices.length > 0" class="invoice-list">
          <h3 class="section-title">查询结果</h3>
          <div class="table-wrapper">
            <table class="table">
              <thead>
                <tr>
                  <th>序号</th>
                  <th>发票代码</th>
                  <th>发票号码</th>
                  <th>发票类型</th>
                  <th>发票状态</th>
                  <th>开票时间</th>
                  <th>价税合计</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(invoice, index) in invoices" :key="index">
                  <td>{{ index + 1 }}</td>
                  <td :id="`invoice-code-${index}`">{{ invoice.invoiceCode }}</td>
                  <td :id="`invoice-number-${index}`">{{ invoice.invoiceNumber }}</td>
                  <td>
                    <span 
                      class="status-badge" 
                      :class="invoice.sign === '销项' ? 'status-sale' : 'status-purchase'"
                      :id="`invoice-sign-${index}`"
                    >
                      {{ invoice.sign }}
                    </span>
                  </td>
                  <td>
                    <span 
                      class="status-badge" 
                      :class="invoice.state === '正常' ? 'status-normal' : 'status-abnormal'"
                      :id="`invoice-state-${index}`"
                    >
                      {{ invoice.state }}
                    </span>
                  </td>
                  <td :id="`invoice-time-${index}`">{{ invoice.invoiceTime }}</td>
                  <td :id="`invoice-jshj-${index}`" class="amount">{{ formatAmount(invoice.jshj) }}</td>
                  <td>
                    <button class="btn btn-primary" style="padding: 6px 12px; font-size: 12px;">查看详情</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <div class="summary">
            <div class="summary-item">
              <span class="summary-label">销项发票合计：</span>
              <span class="summary-value" id="total-sale-amount">{{ formatAmount(totalSaleAmount) }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">进项发票合计：</span>
              <span class="summary-value" id="total-purchase-amount">{{ formatAmount(totalPurchaseAmount) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const searchForm = ref({
  taxNo: '',
  uscCode: '',
  startDate: '',
  endDate: ''
})

// 模拟发票数据 - 用于爬虫测试
const invoices = ref([])

const mockInvoices = [
  {
    invoiceCode: '1500012340',
    invoiceNumber: '12345678',
    sign: '销项',
    state: '正常',
    invoiceTime: '2024-01-15 10:30:00',
    jshj: 123456.78901
  },
  {
    invoiceCode: '1500012341',
    invoiceNumber: '12345679',
    sign: '销项',
    state: '正常',
    invoiceTime: '2024-02-20 14:20:00',
    jshj: 234567.89012
  },
  {
    invoiceCode: '1500012342',
    invoiceNumber: '12345680',
    sign: '销项',
    state: '正常',
    invoiceTime: '2024-03-10 09:15:00',
    jshj: 345678.90123
  },
  {
    invoiceCode: '1500012343',
    invoiceNumber: '12345681',
    sign: '进项',
    state: '正常',
    invoiceTime: '2024-01-25 16:45:00',
    jshj: 456789.01234
  },
  {
    invoiceCode: '1500012344',
    invoiceNumber: '12345682',
    sign: '销项',
    state: '异常',
    invoiceTime: '2024-04-05 11:00:00',
    jshj: 567890.12345
  }
]

const totalSaleAmount = computed(() => {
  return invoices.value
    .filter(inv => inv.sign === '销项')
    .reduce((sum, inv) => sum + inv.jshj, 0)
})

const totalPurchaseAmount = computed(() => {
  return invoices.value
    .filter(inv => inv.sign === '进项')
    .reduce((sum, inv) => sum + inv.jshj, 0)
})

const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return amount.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const searchInvoices = () => {
  // 模拟查询结果
  invoices.value = mockInvoices
}
</script>

<style scoped>
.search-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  margin-bottom: 30px;
  align-items: flex-end;
}

.search-section .form-group {
  margin-bottom: 0;
}

.search-section button {
  grid-column: span 1;
}

.table-wrapper {
  overflow-x: auto;
  margin-top: 20px;
}

.amount {
  font-weight: 600;
  color: var(--primary-color);
}

.summary {
  margin-top: 30px;
  padding: 20px;
  background-color: #fafafa;
  border-radius: 4px;
  display: flex;
  gap: 40px;
}

.summary-item {
  display: flex;
  align-items: center;
}

.summary-label {
  font-weight: 600;
  color: #666;
  margin-right: 10px;
}

.summary-value {
  font-size: 18px;
  font-weight: bold;
  color: var(--primary-color);
}

@media (max-width: 768px) {
  .search-section {
    grid-template-columns: 1fr;
  }
  
  .summary {
    flex-direction: column;
    gap: 15px;
  }
}
</style>
