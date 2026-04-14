<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人中心</span>
        </div>
      </template>
      
      <div class="profile-content">
        <!-- 头像和基本信息 -->
        <div class="profile-header">
          <div class="avatar-section">
            <el-avatar :size="100" :src="userInfo.avatar" class="user-avatar">
              <el-icon :size="50"><User /></el-icon>
            </el-avatar>
            <el-upload
              class="avatar-upload"
              action="#"
              :show-file-list="false"
              :auto-upload="false"
              @change="handleAvatarChange"
            >
              <el-button size="small" type="primary" link>更换头像</el-button>
            </el-upload>
          </div>
          
          <div class="user-basic-info">
            <h2 class="user-name">{{ userInfo.realName }}</h2>
            <div class="user-role-section">
              <el-tag 
                :type="userInfo.role === 'ADMIN' ? 'danger' : 'primary'" 
                size="large"
                effect="plain"
              >
                {{ roleDisplayName }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- 详细信息表单 -->
        <el-divider />
        
        <el-form
          ref="formRef"
          :model="userInfo"
          :rules="rules"
          label-width="100px"
          class="profile-form"
        >
          <el-row :gutter="40">
            <el-col :span="12">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="userInfo.username" disabled placeholder="用户名不可修改" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="userInfo.realName" placeholder="请输入姓名" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="40">
            <el-col :span="12">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="userInfo.email" placeholder="请输入邮箱" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="userInfo.phone" placeholder="请输入手机号" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="40">
            <el-col :span="12">
              <el-form-item label="角色">
                <el-select v-model="userInfo.role" disabled placeholder="角色不可修改" style="width: 100%;">
                  <el-option label="管理员" value="ADMIN" />
                  <el-option label="普通用户" value="USER" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态">
                <el-tag :type="userInfo.status === 'active' ? 'success' : 'danger'">
                  {{ userInfo.status === 'active' ? '启用' : '禁用' }}
                </el-tag>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item>
            <el-button type="primary" @click="handleSave" :loading="loading">
              保存修改
            </el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button type="warning" @click="showPasswordDialog = true">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword" :loading="passwordLoading">
          确认修改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { updateProfile, changePassword } from '../api/user.js'

// 表单引用
const formRef = ref(null)
const passwordFormRef = ref(null)

// 加载状态
const loading = ref(false)
const passwordLoading = ref(false)
const showPasswordDialog = ref(false)

// 用户信息
const userInfo = reactive({
  id: null,
  username: '',
  realName: '',
  email: '',
  phone: '',
  role: 'USER',
  status: 'active',
  avatar: ''
})

// 原始用户信息（用于重置）
const originalUserInfo = ref({})

// 角色显示名称
const roleDisplayName = computed(() => {
  const roleMap = {
    'ADMIN': '管理员',
    'USER': '普通用户'
  }
  return roleMap[userInfo.role] || '未知角色'
})

// 表单验证规则
const rules = {
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 修改密码表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码验证规则
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 加载用户信息
const loadUserInfo = () => {
  const userInfoStr = localStorage.getItem('userInfo')
  if (userInfoStr) {
    const data = JSON.parse(userInfoStr)
    userInfo.id = data.id
    userInfo.username = data.username
    userInfo.realName = data.realName || ''
    userInfo.email = data.email || ''
    userInfo.phone = data.phone || ''
    userInfo.role = data.role || 'USER'
    userInfo.status = data.status || 'active'
    userInfo.avatar = data.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
    
    // 保存原始数据
    originalUserInfo.value = { ...userInfo }
  }
}

// 更换头像
const handleAvatarChange = (file) => {
  // 这里可以实现头像上传逻辑
  ElMessage.info('头像上传功能开发中')
}

// 保存修改
const handleSave = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await updateProfile({
          realName: userInfo.realName,
          email: userInfo.email,
          phone: userInfo.phone
        })
        
        // 更新本地存储
        const userInfoStr = localStorage.getItem('userInfo')
        if (userInfoStr) {
          const data = JSON.parse(userInfoStr)
          data.realName = userInfo.realName
          data.email = userInfo.email
          data.phone = userInfo.phone
          localStorage.setItem('userInfo', JSON.stringify(data))
        }
        
        // 更新原始数据
        originalUserInfo.value = { ...userInfo }
        
        ElMessage.success('保存成功')
      } catch (error) {
        ElMessage.error(error.message || '保存失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 重置表单
const handleReset = () => {
  userInfo.realName = originalUserInfo.value.realName
  userInfo.email = originalUserInfo.value.email
  userInfo.phone = originalUserInfo.value.phone
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      passwordLoading.value = true
      try {
        await changePassword({
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        })
        
        ElMessage.success('密码修改成功，请重新登录')
        showPasswordDialog.value = false
        
        // 清空密码表单
        passwordForm.oldPassword = ''
        passwordForm.newPassword = ''
        passwordForm.confirmPassword = ''
        
        // 3秒后跳转登录页
        setTimeout(() => {
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          window.location.href = '/login'
        }, 1500)
      } catch (error) {
        ElMessage.error(error.message || '密码修改失败')
      } finally {
        passwordLoading.value = false
      }
    }
  })
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped lang="scss">
.profile-container {
  padding: 20px;

  .profile-card {
    max-width: 900px;
    margin: 0 auto;

    .card-header {
      font-size: 18px;
      font-weight: 600;
    }
  }

  .profile-content {
    .profile-header {
      display: flex;
      align-items: center;
      padding: 20px 0;

      .avatar-section {
        display: flex;
        flex-direction: column;
        align-items: center;
        margin-right: 40px;

        .user-avatar {
          border: 3px solid #e4e7ed;
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }

        .avatar-upload {
          margin-top: 12px;
        }
      }

      .user-basic-info {
        flex: 1;

        .user-name {
          margin: 0 0 12px 0;
          font-size: 24px;
          font-weight: 600;
          color: #303133;
        }

        .user-role-section {
          display: flex;
          align-items: center;
          gap: 12px;
        }
      }
    }

    .profile-form {
      padding: 20px 0;
    }
  }
}
</style>
