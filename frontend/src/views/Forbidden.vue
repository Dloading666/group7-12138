<template>
  <div class="forbidden-container">
    <div class="forbidden-content">
      <el-icon :size="120" color="#909399">
        <Lock />
      </el-icon>
      <h1>403</h1>
      <h2>抱歉，您没有权限访问此页面</h2>
      <p class="description">
        您当前的账号角色为普通用户，只有查看权限。<br>
        如需更多权限，请联系系统管理员。
      </p>
      <div class="actions">
        <el-button type="primary" @click="goBack">返回上一页</el-button>
        <el-button @click="goHome">返回首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.go(-1)
}

const goHome = () => {
  router.push('/dashboard')
}
</script>

<style scoped lang="scss">
.forbidden-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e7eb 100%);
  
  .forbidden-content {
    text-align: center;
    padding: 40px;
    
    h1 {
      font-size: 72px;
      color: #909399;
      margin: 20px 0 10px;
      font-weight: bold;
    }
    
    h2 {
      font-size: 24px;
      color: #606266;
      margin: 0 0 20px;
    }
    
    .description {
      color: #909399;
      font-size: 14px;
      line-height: 1.8;
      margin-bottom: 30px;
    }
    
    .actions {
      display: flex;
      justify-content: center;
      gap: 16px;
    }
  }
}
</style>
