<template>
  <div class="stability-indicator">
    <div class="header">
      <div class="header-content">
        <div class="header-title">XX电子税务平台</div>
        <div class="header-nav">
          <router-link to="/">首页</router-link>
          <router-link to="/enterprise-info">企业信息查询</router-link>
          <router-link to="/invoice-query">发票查询</router-link>
          <router-link to="/application">业务申请</router-link>
        </div>
      </div>
    </div>

    <div class="content">
      <!-- 经营稳定性指标：独立数据页，供指标2 inv_f1_12m_down_sale_month_cv_teach 爬取 -->
      <div class="card">
        <h2 class="card-title">经营稳定性指标（近1-12个月销项发票月度金额波动系数）</h2>
        <p class="card-desc">本页提供指标 inv_f1_12m_down_sale_month_cv_teach 计算所需的全部参数，与经营规模指标数据页分离。</p>

        <!-- 第一步：入参 - 企业身份与申请日期 -->
        <div class="section">
          <h3 class="section-title">一、入参（企业身份与申请日期）</h3>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">企业名称</label>
              <input
                type="text"
                class="form-input"
                v-model="enterpriseName"
                placeholder="请输入企业名称"
                id="stability-enterprise-name-input"
              />
            </div>
            <button class="btn btn-primary" @click="searchEnterprise">查询企业</button>
          </div>
          <div v-if="enterpriseData" class="info-block">
            <div class="info-item">
              <span class="info-label">纳税人识别号：</span>
              <span class="info-value" id="stability-tax-no">{{ enterpriseData.taxNo }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">统一社会信用代码：</span>
              <span class="info-value" id="stability-usc-code">{{ enterpriseData.uscCode }}</span>
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label class="form-label">申请日期（用于计算近1-12个月不含当月）</label>
              <input
                type="date"
                class="form-input"
                v-model="appDate"
                id="stability-app-date"
              />
            </div>
          </div>
        </div>

        <!-- 第二步：发票明细查询 -->
        <div class="section">
          <h3 class="section-title">二、发票明细（用于按月汇总与波动系数计算）</h3>
          <div class="search-section">
            <div class="form-group">
              <label class="form-label">纳税人识别号</label>
              <input
                type="text"
                class="form-input"
                v-model="searchForm.taxNo"
                placeholder="可与上方一致"
                id="stability-invoice-tax-no"
              />
            </div>
            <div class="form-group">
              <label class="form-label">统一社会信用代码</label>
              <input
                type="text"
                class="form-input"
                v-model="searchForm.uscCode"
                placeholder="可与上方一致"
                id="stability-invoice-usc-code"
              />
            </div>
            <div class="form-group">
              <label class="form-label">开票日期起</label>
              <input type="date" class="form-input" v-model="searchForm.startDate" id="stability-invoice-start-date" />
            </div>
            <div class="form-group">
              <label class="form-label">开票日期止</label>
              <input type="date" class="form-input" v-model="searchForm.endDate" id="stability-invoice-end-date" />
            </div>
            <button class="btn btn-primary" @click="searchInvoices">查询发票</button>
          </div>

          <div v-if="invoices.length > 0" class="invoice-list">
            <h4 class="sub-title">查询结果（每条记录含：发票类型、状态、开票时间、价税合计）</h4>
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
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(invoice, index) in invoices" :key="index">
                    <td>{{ index + 1 }}</td>
                    <td :id="`stability-invoice-code-${index}`">{{ invoice.invoiceCode }}</td>
                    <td :id="`stability-invoice-number-${index}`">{{ invoice.invoiceNumber }}</td>
                    <td>
                      <span
                        class="status-badge"
                        :class="invoice.sign === '销项' ? 'status-sale' : 'status-purchase'"
                        :id="`stability-invoice-sign-${index}`"
                      >{{ invoice.sign }}</span>
                    </td>
                    <td>
                      <span
                        class="status-badge"
                        :class="invoice.state === '正常' ? 'status-normal' : 'status-abnormal'"
                        :id="`stability-invoice-state-${index}`"
                      >{{ invoice.state }}</span>
                    </td>
                    <td :id="`stability-invoice-time-${index}`">{{ invoice.invoiceTime }}</td>
                    <td :id="`stability-invoice-jshj-${index}`" class="amount">{{ formatAmount(invoice.jshj) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const enterpriseName = ref('')
const appDate = ref('')
const enterpriseData = ref(null)

const searchForm = ref({
  taxNo: '',
  uscCode: '',
  startDate: '',
  endDate: ''
})

const mockEnterpriseData = {
  taxNo: '91500000MA5U123456',
  uscCode: '91500000MA5U123456'
}

const searchEnterprise = () => {
  enterpriseData.value = mockEnterpriseData
  searchForm.value.taxNo = mockEnterpriseData.taxNo
  searchForm.value.uscCode = mockEnterpriseData.uscCode
}

const invoices = ref([])
const mockInvoices = [
  { invoiceCode: '1500012340', invoiceNumber: '12345678', sign: '销项', state: '正常', invoiceTime: '2024-01-15 10:30:00', jshj: 12000 },
  { invoiceCode: '1500012341', invoiceNumber: '12345679', sign: '销项', state: '正常', invoiceTime: '2024-01-20 14:20:00', jshj: 8000 },
  { invoiceCode: '1500012342', invoiceNumber: '12345680', sign: '销项', state: '正常', invoiceTime: '2024-02-10 09:15:00', jshj: 15000 },
  { invoiceCode: '1500012343', invoiceNumber: '12345681', sign: '进项', state: '正常', invoiceTime: '2024-02-25 16:45:00', jshj: 5000 },
  { invoiceCode: '1500012344', invoiceNumber: '12345682', sign: '销项', state: '正常', invoiceTime: '2024-03-05 11:00:00', jshj: 13000 },
  { invoiceCode: '1500012345', invoiceNumber: '12345683', sign: '销项', state: '异常', invoiceTime: '2024-03-12 10:00:00', jshj: 9000 }
]

const formatAmount = (amount) => {
  if (!amount) return '0.00'
  return amount.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

const searchInvoices = () => {
  invoices.value = mockInvoices
}
</script>

<style scoped>
.card-desc {
  color: #666;
  margin-bottom: 24px;
  font-size: 14px;
}
.section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-color);
}
.section:last-of-type {
  border-bottom: none;
}
.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--primary-color);
  margin-bottom: 16px;
}
.sub-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 20px 0 12px 0;
}
.form-row {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 16px;
}
.form-row .form-group {
  margin-bottom: 0;
  min-width: 200px;
}
.info-block {
  margin: 16px 0;
  padding: 12px;
  background: #fafafa;
  border-radius: 4px;
}
.search-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 20px;
}
.search-section .form-group {
  margin-bottom: 0;
}
.table-wrapper {
  overflow-x: auto;
  margin-top: 12px;
}
.amount {
  font-weight: 600;
  color: var(--primary-color);
}
@media (max-width: 768px) {
  .search-section {
    grid-template-columns: 1fr;
  }
}
</style>
