<template>
  <div class="application-page">
    <div class="header">
      <div class="header-content">
        <div class="header-title">XX电子税务平台</div>
        <div class="header-nav">
          <router-link to="/">首页</router-link>
          <router-link to="/enterprise-info">企业信息查询</router-link>
          <router-link to="/invoice-query">发票查询</router-link>
          <router-link to="/stability-indicator">经营稳定性指标</router-link>
        </div>
      </div>
    </div>

    <div class="content">
      <div class="card">
        <h2 class="card-title">业务申请</h2>
        
        <form @submit.prevent="submitApplication">
          <div class="form-section">
            <h3 class="section-title">申请基本信息</h3>
            
            <div class="form-group">
              <label class="form-label">纳税人识别号 <span class="required">*</span></label>
              <input 
                type="text" 
                class="form-input" 
                v-model="applicationForm.taxNo"
                placeholder="请输入纳税人识别号"
                required
                id="app-tax-no"
              />
            </div>
            
            <div class="form-group">
              <label class="form-label">统一社会信用代码 <span class="required">*</span></label>
              <input 
                type="text" 
                class="form-input" 
                v-model="applicationForm.uscCode"
                placeholder="请输入统一社会信用代码"
                required
                id="app-usc-code"
              />
            </div>
            
            <div class="form-group">
              <label class="form-label">申请日期 <span class="required">*</span></label>
              <input 
                type="date" 
                class="form-input" 
                v-model="applicationForm.appDate"
                required
                id="app-date"
              />
            </div>
            
            <div class="form-group">
              <label class="form-label">申请类型 <span class="required">*</span></label>
              <select class="form-input" v-model="applicationForm.appType" required id="app-type">
                <option value="">请选择申请类型</option>
                <option value="credit">授信申请</option>
                <option value="loan">贷款申请</option>
                <option value="other">其他业务</option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label">申请金额（元）</label>
              <input 
                type="number" 
                class="form-input" 
                v-model="applicationForm.amount"
                placeholder="请输入申请金额"
                id="app-amount"
              />
            </div>
            
            <div class="form-group">
              <label class="form-label">申请说明</label>
              <textarea 
                class="form-input" 
                v-model="applicationForm.description"
                rows="4"
                placeholder="请输入申请说明"
                id="app-description"
              ></textarea>
            </div>
          </div>

          <div class="form-actions">
            <button type="submit" class="btn btn-primary">提交申请</button>
            <button type="button" class="btn" style="background-color: #f5f5f5; margin-left: 10px;" @click="resetForm">重置</button>
          </div>
        </form>

        <div v-if="submitted" class="success-message">
          <h3>申请提交成功！</h3>
          <div class="info-item">
            <span class="info-label">申请编号：</span>
            <span class="info-value" id="app-number">{{ applicationNumber }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">申请日期：</span>
            <span class="info-value" id="app-date-display">{{ applicationForm.appDate }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">纳税人识别号：</span>
            <span class="info-value" id="app-tax-no-display">{{ applicationForm.taxNo }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">统一社会信用代码：</span>
            <span class="info-value" id="app-usc-code-display">{{ applicationForm.uscCode }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const applicationForm = ref({
  taxNo: '',
  uscCode: '',
  appDate: '',
  appType: '',
  amount: '',
  description: ''
})

const submitted = ref(false)
const applicationNumber = ref('')

const submitApplication = () => {
  // 生成申请编号
  applicationNumber.value = 'APP' + Date.now()
  submitted.value = true
}

const resetForm = () => {
  applicationForm.value = {
    taxNo: '',
    uscCode: '',
    appDate: '',
    appType: '',
    amount: '',
    description: ''
  }
  submitted.value = false
  applicationNumber.value = ''
}
</script>

<style scoped>
.form-section {
  margin-bottom: 30px;
}

.required {
  color: var(--danger-color);
}

.form-actions {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid var(--border-color);
}

.success-message {
  margin-top: 30px;
  padding: 20px;
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 4px;
}

.success-message h3 {
  color: var(--success-color);
  margin-bottom: 15px;
}

textarea.form-input {
  resize: vertical;
  font-family: inherit;
}
</style>
