<template>
  <div class="login">
    <div class="header">
      <div class="header-content">
        <div class="header-title">XX电子税务平台</div>
        <div class="header-nav">
          <router-link to="/">首页</router-link>
        </div>
      </div>
    </div>

    <div class="content">
      <div class="login-card">
        <h2 class="card-title">用户登录</h2>
        <form @submit.prevent="handleLogin">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input 
              type="text" 
              class="form-input" 
              v-model="username"
              placeholder="请输入用户名"
              id="login-username"
            />
          </div>
          <div class="form-group">
            <label class="form-label">密码</label>
            <input 
              type="password" 
              class="form-input" 
              v-model="password"
              placeholder="请输入密码"
              id="login-password"
            />
          </div>
          <div class="form-group">
            <label class="form-label">验证码</label>
            <div style="display: flex; gap: 10px;">
              <input 
                type="text" 
                class="form-input" 
                v-model="captcha"
                placeholder="请输入验证码"
                id="login-captcha"
                style="flex: 1;"
              />
              <div class="captcha-img" id="captcha-image">1234</div>
            </div>
          </div>
          <button type="submit" class="btn btn-primary" style="width: 100%;">登录</button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const username = ref('')
const password = ref('')
const captcha = ref('')

const handleLogin = () => {
  // 模拟登录，直接跳转到企业信息页面
  router.push('/enterprise-info')
}
</script>

<style scoped>
.login-card {
  max-width: 500px;
  margin: 50px auto;
  background: var(--content-bg);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 40px;
}

.captcha-img {
  width: 100px;
  height: 40px;
  background-color: #f0f0f0;
  border: 1px solid var(--border-color);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  color: var(--primary-color);
  cursor: pointer;
  user-select: none;
}
</style>
