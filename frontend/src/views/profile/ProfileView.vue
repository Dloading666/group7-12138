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
          <el-form-item label="头像">
            <div class="avatar-editor">
              <el-avatar :size="88" :src="profileForm.avatar || authStore.user?.avatar || avatarFallback">
                {{ avatarText }}
              </el-avatar>
              <div class="avatar-actions">
                <el-upload
                  :show-file-list="false"
                  :http-request="handleAvatarUpload"
                  :before-upload="beforeAvatarUpload"
                  accept="image/png,image/jpeg,image/webp,image/gif"
                >
                  <el-button :loading="avatarUploading">更换头像</el-button>
                </el-upload>
                <div class="avatar-tip">选择图片后会立即上传并绑定到当前账号。</div>
                <div class="avatar-tip">支持 JPG / PNG / WebP / GIF，大小不超过 2MB。</div>
              </div>
            </div>
          </el-form-item>

          <el-form-item label="用户名">
            <el-input v-model="profileForm.username" disabled />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="profileForm.realName" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="profileForm.phone" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="profileForm.email" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="修改密码" name="password">
        <el-form ref="passwordRef" :model="passwordForm" :rules="passwordRules" label-width="100px" class="profile-form">
          <el-form-item label="原密码" prop="oldPassword">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input v-model="passwordForm.newPassword" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="passwordSaving" @click="submitChangePassword">修改密码</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadProps, type UploadRequestOptions } from 'element-plus'
import { useRoute } from 'vue-router'
import { changePassword, getProfile, updateProfile, uploadProfileAvatar } from '@/api/profile'
import { useAuthStore } from '@/stores/auth'
import type { UserProfile } from '@/types/auth'

const avatarFallback = 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'

const route = useRoute()
const authStore = useAuthStore()
const activeTab = ref(String(route.query.tab || 'profile'))
const profileSaving = ref(false)
const passwordSaving = ref(false)
const avatarUploading = ref(false)
const profileRef = ref<FormInstance>()
const passwordRef = ref<FormInstance>()

const profileForm = reactive<Partial<UserProfile>>({
  username: '',
  realName: '',
  phone: '',
  email: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const avatarText = computed(() => String(profileForm.realName || profileForm.username || 'U').slice(0, 1).toUpperCase())

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
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function isAxiosLikeError(error: unknown) {
  return typeof error === 'object' && error !== null && 'response' in error
}

async function syncCurrentUser() {
  const current = await authStore.fetchCurrentUser().catch(() => null)
  if (current?.user) {
    Object.assign(profileForm, current.user)
    return current.user
  }
  return null
}

async function loadProfile() {
  try {
    const res = await getProfile()
    if (res.code !== 200) throw new Error(res.message || '获取个人信息失败')
    const data = res.data || authStore.user
    if (data) {
      Object.assign(profileForm, data)
    }
  } catch {
    if (authStore.user) {
      Object.assign(profileForm, authStore.user)
    }
  }
}

async function saveProfile() {
  await profileRef.value?.validate(async (valid) => {
    if (!valid) return
    profileSaving.value = true
    try {
      const res = await updateProfile(profileForm)
      if (res.code !== 200) throw new Error(res.message || '保存资料失败')
      const latestUser = (await syncCurrentUser()) || res.data
      if (latestUser) {
        Object.assign(profileForm, latestUser)
      }
      ElMessage.success('资料已保存')
    } catch (error) {
      if (!isAxiosLikeError(error)) {
        ElMessage.error((error as Error).message || '保存资料失败')
      }
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
      const res = await changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      if (res.code !== 200) throw new Error(res.message || '修改密码失败')
      ElMessage.success('密码修改成功')
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    } catch (error) {
      if (!isAxiosLikeError(error)) {
        ElMessage.error((error as Error).message || '修改密码失败')
      }
    } finally {
      passwordSaving.value = false
    }
  })
}

const beforeAvatarUpload: UploadProps['beforeUpload'] = (file) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  return true
}

async function handleAvatarUpload(options: UploadRequestOptions) {
  avatarUploading.value = true
  try {
    const res = await uploadProfileAvatar(options.file as File)
    if (res.code !== 200) throw new Error(res.message || '头像上传失败')
    const latestUser = (await syncCurrentUser()) || res.data
    if (latestUser) {
      Object.assign(profileForm, latestUser)
    }
    options.onSuccess?.(res.data)
    ElMessage.success('头像已更新')
  } catch (error) {
    options.onError?.(error as any)
    if (!isAxiosLikeError(error)) {
      ElMessage.error((error as Error).message || '头像上传失败')
    }
  } finally {
    avatarUploading.value = false
  }
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
  max-width: 720px;
  padding: 12px 0 18px;
}

.avatar-editor {
  display: flex;
  align-items: center;
  gap: 18px;
}

.avatar-actions {
  display: grid;
  gap: 8px;
}

.avatar-tip {
  font-size: 12px;
  color: var(--app-text-muted);
  line-height: 1.5;
}

@media (max-width: 640px) {
  .avatar-editor {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
