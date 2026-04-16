<template>
  <div class="enterprise-info">
    <div class="header">
      <div class="header-content">
        <div class="header-title">XX电子税务平台</div>
        <div class="header-nav">
          <router-link to="/">首页</router-link>
          <router-link to="/invoice-query">发票查询</router-link>
          <router-link to="/application">业务申请</router-link>
          <router-link to="/stability-indicator">经营稳定性指标</router-link>
        </div>
      </div>
    </div>

    <div class="content">
      <div class="card">
        <h2 class="card-title">企业信息查询</h2>
        
        <div class="search-section">
          <div class="form-group">
            <label class="form-label">企业名称</label>
            <input
              type="text"
              class="form-input"
              v-model="enterpriseName"
              placeholder="请输入企业名称"
              id="enterprise-name-input"
            />
          </div>
          <div class="form-group">
            <label class="form-label">纳税人识别号</label>
            <input
              type="text"
              class="form-input"
              v-model="taxNo"
              placeholder="请输入纳税人识别号"
              id="enterprise-tax-no"
            />
          </div>
          <button class="btn btn-primary" @click="searchEnterprise">查询</button>
        </div>

        <div v-if="enterpriseData" class="enterprise-details">
          <h3 class="section-title">企业基本信息</h3>
          
          <div class="info-item">
            <span class="info-label">企业名称：</span>
            <span class="info-value" id="enterprise-name">{{ enterpriseData.name }}</span>
          </div>
          
          <div class="info-item">
            <span class="info-label">纳税人识别号：</span>
            <span class="info-value" id="tax-no">{{ enterpriseData.taxNo }}</span>
          </div>
          
          <div class="info-item">
            <span class="info-label">统一社会信用代码：</span>
            <span class="info-value" id="usc-code">{{ enterpriseData.uscCode }}</span>
          </div>
          
          <div class="info-item">
            <span class="info-label">注册地址：</span>
            <span class="info-value">{{ enterpriseData.address }}</span>
          </div>
          
          <div class="info-item">
            <span class="info-label">法定代表人：</span>
            <span class="info-value">{{ enterpriseData.legalPerson }}</span>
          </div>
          
          <div class="info-item">
            <span class="info-label">成立日期：</span>
            <span class="info-value">{{ enterpriseData.establishDate }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const enterpriseName = ref('')
const taxNo = ref('')
const enterpriseData = ref(null)

// 模拟数据 - 用于爬虫测试
const mockEnterpriseData = {
  name: '重庆某某科技有限公司',
  taxNo: '91500000MA5U123456',
  uscCode: '91500000MA5U123456',
  address: '重庆市渝北区某某街道123号',
  legalPerson: '张三',
  establishDate: '2018-05-15'
}

const searchEnterprise = () => {
  // 模拟查询结果 - 支持按企业名称或纳税人识别号查询
  if (enterpriseName.value || taxNo.value) {
    enterpriseData.value = mockEnterpriseData
  }
}
</script>

<style scoped>
.search-section {
  display: flex;
  gap: 15px;
  margin-bottom: 30px;
  align-items: flex-end;
}

.search-section .form-group {
  flex: 1;
  margin-bottom: 0;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--primary-color);
  margin: 20px 0 15px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color);
}

.enterprise-details {
  margin-top: 30px;
}
</style>
