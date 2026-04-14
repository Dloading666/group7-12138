<template>
  <div class="login-container">
    <!-- 左侧品牌区域 -->
    <div class="brand-section">
      <div class="brand-content">
        <div class="brand-logo">
          <el-icon :size="80"><Monitor /></el-icon>
        </div>
        <h1 class="brand-title">RPA 管理系统</h1>
        <p class="brand-subtitle">智能自动化 · 高效管理</p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>自动化任务管理</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>流程可视化设计</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>实时数据监控</span>
          </div>
          <div class="feature-item">
            <el-icon><Check /></el-icon>
            <span>智能数据分析</span>
          </div>
        </div>
      </div>
      <!-- 装饰性背景 -->
      <div class="bg-decoration">
        <div class="circle circle-1"></div>
        <div class="circle circle-2"></div>
        <div class="circle circle-3"></div>
      </div>
    </div>

    <!-- 右侧登录区域 -->
    <div class="login-section">
      <div class="login-box">
        <div class="login-header">
          <h2>欢迎登录</h2>
          <p>请输入您的账号信息</p>
        </div>
        
        <el-form :model="loginForm" :rules="rules" ref="loginFormRef" class="login-form">
          <el-form-item prop="username">
            <el-input 
              v-model="loginForm.username" 
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入密码"
              :prefix-icon="Lock"
              size="large"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button 
              type="primary" 
              size="large" 
              class="login-btn"
              @click="handleLogin"
              :loading="loading"
            >
              <span v-if="!loading">登 录</span>
              <span v-else>登录中...</span>
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 版权信息 -->
      <div class="copyright">
        © 2024 RPA Management System. All rights reserved.
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Check, Monitor } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login, getUserPermissions } from '../api/user.js'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = () => {
  loginFormRef.value.validate((valid) => {
    if (valid) {
      loading.value = true
      
      login({
        username: loginForm.username,
        password: loginForm.password
      }).then(response => {
        const data = response.data
        
        localStorage.setItem('token', data.token)
        
        const userInfo = {
          userId: data.userId,
          username: data.username,
          realName: data.realName,
          role: data.role,
          roleDisplayName: data.roleDisplayName,
          email: data.email,
          avatar: data.avatar
        }
        localStorage.setItem('userInfo', JSON.stringify(userInfo))
        
        // 获取用户权限
        return getUserPermissions()
      }).then(permRes => {
        // 存储用户权限
        const permissions = permRes.data || []
        const permissionCodes = permissions.map(p => p.code)
        localStorage.setItem('userPermissions', JSON.stringify(permissionCodes))
        
        ElMessage.success('登录成功，欢迎回来！')
        router.push('/')
      }).catch(error => {
        console.error('登录失败:', error)
      }).finally(() => {
        loading.value = false
      })
    }
  })
}
</script>

<style scoped lang="scss">
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  background: #f5f7fa;
}

// 左侧品牌区域
.brand-section {
  flex: 1;
  background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;

  .brand-content {
    text-align: center;
    color: #fff;
    z-index: 10;
    padding: 40px;

    .brand-logo {
      width: 120px;
      height: 120px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 30px;
      backdrop-filter: blur(10px);
      animation: float 3s ease-in-out infinite;
    }

    .brand-title {
      font-size: 36px;
      font-weight: 700;
      margin-bottom: 10px;
      letter-spacing: 2px;
    }

    .brand-subtitle {
      font-size: 16px;
      opacity: 0.9;
      margin-bottom: 40px;
    }

    .brand-features {
      display: flex;
      flex-direction: column;
      gap: 16px;

      .feature-item {
        display: flex;
        align-items: center;
        gap: 12px;
        font-size: 15px;
        opacity: 0.9;
        padding: 12px 24px;
        background: rgba(255, 255, 255, 0.1);
        border-radius: 8px;
        transition: all 0.3s;

        &:hover {
          background: rgba(255, 255, 255, 0.2);
          transform: translateX(10px);
        }

        .el-icon {
          font-size: 18px;
        }
      }
    }
  }

  // 装饰性背景圆
  .bg-decoration {
    position: absolute;
    inset: 0;
    pointer-events: none;

    .circle {
      position: absolute;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.1);
    }

    .circle-1 {
      width: 300px;
      height: 300px;
      top: -100px;
      left: -100px;
      animation: pulse 4s ease-in-out infinite;
    }

    .circle-2 {
      width: 200px;
      height: 200px;
      bottom: 100px;
      right: -50px;
      animation: pulse 3s ease-in-out infinite 1s;
    }

    .circle-3 {
      width: 150px;
      height: 150px;
      bottom: -50px;
      left: 50px;
      animation: pulse 5s ease-in-out infinite 0.5s;
    }
  }
}

// 右侧登录区域
.login-section {
  width: 480px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  background: #fff;

  .login-box {
    width: 100%;
    max-width: 380px;
  }

  .login-header {
    text-align: center;
    margin-bottom: 40px;

    h2 {
      font-size: 28px;
      font-weight: 600;
      color: #1a1a2e;
      margin-bottom: 8px;
    }

    p {
      font-size: 14px;
      color: #8e8e8e;
    }
  }

  .login-form {
    .el-form-item {
      margin-bottom: 24px;
    }

    :deep(.el-input__wrapper) {
      padding: 8px 15px; 
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
      border: 1px solid #e8e8e8;
      transition: all 0.3s;

      &:hover, &.is-focus {
        border-color: #11998e;
        box-shadow: 0 4px 12px rgba(17, 153, 142, 0.15);
      }
    }

    :deep(.el-input__prefix-inner) {
      color: #8e8e8e;
    }

    .login-btn {
      width: 100%;
      height: 48px;
      font-size: 16px;
      font-weight: 500;
      border-radius: 8px;
      background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
      border: none;
      letter-spacing: 4px;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 20px rgba(17, 153, 142, 0.4);
      }

      &:active {
        transform: translateY(0);
      }
    }
  }

  .copyright {
    margin-top: 40px;
    font-size: 12px;
    color: #bfbfbf;
  }
}

// 动画
@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.15;
  }
}

// 响应式适配
@media screen and (max-width: 900px) {
  .brand-section {
    display: none;
  }

  .login-section {
    width: 100%;
  }
}
</style>
