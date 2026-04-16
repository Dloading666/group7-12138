<template>
  <div class="profile-page app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>个人中心</h2>
        <p>集中维护账号资料与安全设置，把信息展示、资料编辑和密码修改拆成更清晰的几个区域。</p>
      </div>
      <div class="page-header-actions">
        <el-button @click="handleReset">重置</el-button>
        <el-button type="warning" plain @click="showPasswordDialog = true">修改密码</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存资料</el-button>
      </div>
    </div>

    <div class="profile-layout" v-loading="loading">
      <div class="page-section padded identity-card">
        <div class="profile-hero">
          <el-avatar :size="96" :src="userInfo.avatar || undefined" class="profile-avatar">
            {{ avatarFallback }}
          </el-avatar>
          <div class="profile-summary">
            <h3>{{ userInfo.realName || userInfo.username || '未命名用户' }}</h3>
            <p>@{{ userInfo.username || '-' }}</p>
            <div class="profile-tags">
              <el-tag :type="roleTagType" effect="plain">{{ roleLabel }}</el-tag>
              <el-tag :type="userInfo.status === 'active' ? 'success' : 'info'" effect="plain">
                {{ userInfo.status === 'active' ? '已启用' : '未启用' }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="identity-list">
          <div>
            <span>最近登录时间</span>
            <strong>{{ formatDateTime(userInfo.lastLoginTime) }}</strong>
          </div>
          <div>
            <span>最近登录 IP</span>
            <strong>{{ userInfo.lastLoginIp || '-' }}</strong>
          </div>
          <div>
            <span>账号创建时间</span>
            <strong>{{ formatDateTime(userInfo.createTime) }}</strong>
          </div>
          <div>
            <span>最近更新时间</span>
            <strong>{{ formatDateTime(userInfo.updateTime) }}</strong>
          </div>
        </div>

        <el-form label-position="top" class="avatar-form">
          <el-form-item label="头像地址（可选）">
            <el-input v-model="userInfo.avatar" placeholder="https://example.com/avatar.png" />
          </el-form-item>
        </el-form>
      </div>

      <div class="profile-main-stack">
        <div class="page-section padded">
          <div class="section-heading compact-heading">
            <div>
              <h3>基本资料</h3>
              <p>这里的内容会同步到本地登录信息与后端个人资料接口。</p>
            </div>
          </div>

          <el-form ref="formRef" :model="userInfo" :rules="rules" label-position="top">
            <div class="profile-form-grid">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="userInfo.username" disabled />
              </el-form-item>
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="userInfo.realName" placeholder="请输入姓名" />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="userInfo.email" placeholder="请输入邮箱" />
              </el-form-item>
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="userInfo.phone" placeholder="请输入手机号" />
              </el-form-item>
              <el-form-item label="角色">
                <el-input :model-value="roleLabel" disabled />
              </el-form-item>
              <el-form-item label="账号状态">
                <el-input :model-value="userInfo.status === 'active' ? '已启用' : '未启用'" disabled />
              </el-form-item>
            </div>
          </el-form>
        </div>

        <div class="page-section padded">
          <div class="section-heading compact-heading security-heading">
            <div>
              <h3>安全设置</h3>
              <p>修改密码后会清空当前登录态并跳转到登录页，避免旧 token 继续使用。</p>
            </div>
            <el-button type="warning" @click="showPasswordDialog = true">修改密码</el-button>
          </div>

          <ul class="security-list">
            <li>密码长度建议不少于 8 位，并混合数字与字母。</li>
            <li>如果你在多台设备上登录，修改密码后建议全部重新登录一次。</li>
            <li>头像地址只是展示字段，不会上传到文件服务器。</li>
          </ul>
        </div>
      </div>
    </div>

    <el-dialog v-model="showPasswordDialog" title="修改密码" width="520px" :close-on-click-modal="false">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { changePassword, getProfile, updateProfile } from '../api/user.js'

const formRef = ref(null)
const passwordFormRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const passwordLoading = ref(false)
const showPasswordDialog = ref(false)

const userInfo = reactive({
  id: null,
  username: '',
  realName: '',
  email: '',
  phone: '',
  role: 'USER',
  status: 'active',
  avatar: '',
  createTime: '',
  updateTime: '',
  lastLoginTime: '',
  lastLoginIp: ''
})

const originalUserInfo = ref({})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const avatarFallback = computed(() => (userInfo.realName || userInfo.username || 'U').slice(0, 1).toUpperCase())
const roleLabel = computed(() => {
  const map = {
    ADMIN: '系统管理员',
    USER: '普通用户'
  }
  return map[userInfo.role] || userInfo.role || '-'
})
const roleTagType = computed(() => (userInfo.role === 'ADMIN' ? 'danger' : 'primary'))

const rules = {
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度需在 2 到 20 个字符之间', trigger: 'blur' }
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

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const applyUserData = (data = {}) => {
  userInfo.id = data.id ?? null
  userInfo.username = data.username || ''
  userInfo.realName = data.realName || ''
  userInfo.email = data.email || ''
  userInfo.phone = data.phone || ''
  userInfo.role = data.role || 'USER'
  userInfo.status = data.status || 'active'
  userInfo.avatar = data.avatar || ''
  userInfo.createTime = data.createTime || ''
  userInfo.updateTime = data.updateTime || ''
  userInfo.lastLoginTime = data.lastLoginTime || ''
  userInfo.lastLoginIp = data.lastLoginIp || ''
}

const snapshotOriginal = () => {
  originalUserInfo.value = { ...userInfo }
}

const syncLocalUser = () => {
  try {
    const local = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const next = {
      ...local,
      id: userInfo.id,
      username: userInfo.username,
      realName: userInfo.realName,
      email: userInfo.email,
      phone: userInfo.phone,
      role: userInfo.role,
      status: userInfo.status,
      avatar: userInfo.avatar
    }
    localStorage.setItem('userInfo', JSON.stringify(next))
  } catch (error) {
    console.error('同步本地用户信息失败:', error)
  }
}

const loadProfileData = async () => {
  loading.value = true
  try {
    const res = await getProfile()
    applyUserData(res.data || {})
    snapshotOriginal()
    syncLocalUser()
  } catch (error) {
    console.error('加载个人资料失败:', error)
    const local = JSON.parse(localStorage.getItem('userInfo') || '{}')
    applyUserData(local)
    snapshotOriginal()
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
    saving.value = true
    const res = await updateProfile({
      realName: userInfo.realName,
      email: userInfo.email,
      phone: userInfo.phone,
      avatar: userInfo.avatar || null
    })

    applyUserData(res.data || { ...userInfo })
    snapshotOriginal()
    syncLocalUser()
    ElMessage.success('个人资料已保存')
  } catch (error) {
    if (error) {
      console.error('保存个人资料失败:', error)
      ElMessage.error('保存个人资料失败')
    }
  } finally {
    saving.value = false
  }
}

const handleReset = () => {
  applyUserData(originalUserInfo.value)
}

const handleChangePassword = async () => {
  if (!passwordFormRef.value) {
    return
  }

  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    ElMessage.success('密码修改成功，即将重新登录')
    showPasswordDialog.value = false
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''

    setTimeout(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      window.location.href = '/login'
    }, 1200)
  } catch (error) {
    if (error) {
      console.error('修改密码失败:', error)
      ElMessage.error('修改密码失败')
    }
  } finally {
    passwordLoading.value = false
  }
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadProfileData()
})
</script>

<style scoped lang="scss">
.profile-page {
  padding: 4px;
}

.profile-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 18px;
}

.profile-main-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.identity-card {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-hero {
  display: flex;
  align-items: center;
  gap: 16px;
}

.profile-avatar {
  color: #fff;
  background: linear-gradient(135deg, #0f766e, #2563eb);
  box-shadow: 0 16px 30px rgba(37, 99, 235, 0.2);
}

.profile-summary h3 {
  margin: 0 0 6px;
  font-size: 24px;
}

.profile-summary p {
  margin: 0;
  color: var(--app-text-muted);
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.identity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.identity-list div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(148, 163, 184, 0.08);
}

.identity-list span {
  color: var(--app-text-muted);
  font-size: 13px;
}

.identity-list strong {
  color: var(--app-text);
  word-break: break-all;
}

.profile-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.compact-heading {
  margin-bottom: 18px;
}

.security-heading {
  align-items: center;
}

.security-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.9;
  color: var(--app-text);
}

@media (max-width: 1200px) {
  .profile-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .profile-form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
