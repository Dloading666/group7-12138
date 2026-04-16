<template>
  <div class="home">
    <div class="header">
      <div class="header-content">
        <div class="header-title">XX电子税务平台</div>
        <div class="header-nav">
          <router-link to="/login">登录</router-link>
          <router-link to="/enterprise-info">企业信息查询</router-link>
          <router-link to="/invoice-query">发票查询</router-link>
          <router-link to="/application">业务申请</router-link>
          <router-link to="/stability-indicator">经营稳定性指标</router-link>
        </div>
      </div>
    </div>

    <div class="content">
      <div class="card">
        <h1 class="card-title">欢迎使用XX电子税务平台</h1>
        <div class="welcome-content">
          <p>本系统提供以下服务：</p>
          <ul>
            <li>企业信息查询</li>
            <li>发票信息查询</li>
            <li>业务申请办理</li>
          </ul>
        </div>
      </div>

      <div class="card">
        <h2 class="card-title">快速入口</h2>
        <div class="quick-links">
          <router-link to="/enterprise-info" class="quick-link">
            <div class="link-icon">🏢</div>
            <div class="link-text">企业信息查询</div>
          </router-link>
          <router-link to="/invoice-query" class="quick-link">
            <div class="link-icon">📄</div>
            <div class="link-text">发票查询</div>
          </router-link>
          <router-link to="/application" class="quick-link">
            <div class="link-icon">📝</div>
            <div class="link-text">业务申请</div>
          </router-link>
          <router-link to="/stability-indicator" class="quick-link">
            <div class="link-icon">📊</div>
            <div class="link-text">经营稳定性指标</div>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.welcome-content {
  line-height: 1.8;
}

.welcome-content ul {
  margin-left: 20px;
  margin-top: 10px;
}

.welcome-content li {
  margin-bottom: 8px;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.quick-link {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-radius: 8px;
  text-decoration: none;
  color: #333;
  transition: transform 0.3s, box-shadow 0.3s;
}

.quick-link:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.link-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.link-text {
  font-size: 16px;
  font-weight: 600;
}
</style>
