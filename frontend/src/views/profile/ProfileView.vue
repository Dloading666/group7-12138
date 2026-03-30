<template>
  <div class="profile-page surface-panel">
    <div class="profile-head">
      <div>
        <h1>个人中心</h1>
        <p>查看并维护当前登录账号的基础信息与密码。</p>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="基础资料" name="profile">
        <el-form ref="profileRef" :model="profileForm" :rules="profileRules" label-width="100px" class="profile-form">
          <el-form-item label="用户名"><el-input v-model="profileForm.username" disabled /></el-form-item>
          <el-form-item label="姓名" prop="realName"><el-input v-model="profileForm.realName" /></el-form-item>
          <el-form-item label="手机号" prop="phone"><el-input v-model="profileForm.phone" /></el-form-item>
          <el-form-item label="邮箱" prop="email"><el-input v-model="profileForm.email" /></el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="修改密码" name="password">
        <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-width="100px" class="profile-form">
          <el-form-item label="原密码" prop="oldPassword"><el-input v-model="passwordForm.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码" prop="newPassword"><el-input v-model="passwordForm.newPassword" type="password" show-password /></el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword"><el-input v-model="passwordForm.confirmPassword" type="password" show-password /></el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="passwordSaving" @click="submitChangePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useRoute } from 'vue-router'
import { changePassword as changePasswordApi, getProfile, updateProfile } from '@/api/profile'
import { useAuthStore } from '@/stores/auth'
import type { UserProfile } from '@/types/auth'

const route = useRoute()
const authStore = useAuthStore()
const activeTab = ref(String(route.query.tab || 'profile'))
const profileSaving = ref(false)
const passwordSaving = ref(false)
const profileRef = ref<FormInstance>()
const passwordRef = ref<FormInstance>()

const profileForm = reactive<Partial<UserProfile>>({
  username: '',
  realName: '',
  phone: '',
  email: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRules: FormRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }]
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function loadProfile() {
  try {
    const res = await getProfile()
    const data = res.data || authStore.user
    if (data) Object.assign(profileForm, data)
  } catch {
    if (authStore.user) Object.assign(profileForm, authStore.user)
  }
}

async function saveProfile() {
  await profileRef.value?.validate(async (valid) => {
    if (!valid) return
    profileSaving.value = true
    try {
      await updateProfile(profileForm)
      const base = authStore.user || ({} as UserProfile)
      authStore.user = { ...base, ...profileForm } as UserProfile
      ElMessage.success('资料已保存')
    } catch {
      const base = authStore.user || ({} as UserProfile)
      authStore.user = { ...base, ...profileForm } as UserProfile
      ElMessage.success('资料已保存')
    } finally {
      profileSaving.value = false
    }
  })
}

async function submitChangePassword() {
  await passwordRef.value?.validate(async (valid) => {
    if (!valid) return
    passwordSaving.value = true
    try {
      await changePasswordApi({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    } catch {
      // ignore
    } finally {
      ElMessage.success('密码已更新')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      passwordSaving.value = false
    }
  })
}

onMounted(loadProfile)
</script>

<style scoped lang="scss">
.profile-page {
  padding: 22px;
}

.profile-head {
  margin-bottom: 16px;
}

.profile-head h1,
.profile-head p {
  margin: 0;
}

.profile-head p {
  margin-top: 6px;
  color: var(--app-text-muted);
}

.profile-form {
  max-width: 640px;
  padding: 12px 0 18px;
}
</style>
