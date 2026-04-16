<template>
  <div class="app-page user-management-page">
    <section class="page-header-bar">
      <div class="page-title-block">
        <h2>用户管理</h2>
        <p>统一维护管理员、普通用户与访客账号，未分配的权限将无法访问对应页面，也不能执行相关操作。</p>
        <div class="page-meta-pills">
          <span class="meta-pill">总用户 {{ total }}</span>
          <span class="meta-pill">当前页 {{ userList.length }}</span>
          <span class="meta-pill">访客 {{ guestCount }}</span>
          <span class="meta-pill" v-if="canDeleteUser">已选 {{ selectedIds.length }}</span>
        </div>
      </div>
      <div class="page-header-actions">
        <el-button v-if="canAddUser" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
      </div>
    </section>

    <section class="page-section">
      <div class="page-filter-bar">
        <div class="form-field-inline">
          <span class="label">用户名</span>
          <el-input
            v-model="searchForm.username"
            placeholder="按用户名筛选"
            clearable
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="form-field-inline">
          <span class="label">姓名</span>
          <el-input
            v-model="searchForm.realName"
            placeholder="按姓名筛选"
            clearable
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="form-field-inline">
          <span class="label">角色</span>
          <el-select v-model="searchForm.role" placeholder="全部角色" clearable>
            <el-option
              v-for="role in ROLE_OPTIONS"
              :key="role.value"
              :label="role.label"
              :value="role.value"
            />
          </el-select>
        </div>
        <div class="form-field-inline">
          <span class="label">状态</span>
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable>
            <el-option label="启用" value="active" />
            <el-option label="禁用" value="inactive" />
          </el-select>
        </div>
        <div class="page-filter-actions">
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
          <el-button
            v-if="canDeleteUser"
            type="danger"
            :disabled="selectedIds.length === 0"
            @click="handleBatchDelete"
          >
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
        </div>
      </div>
    </section>

    <section class="page-section page-table-card">
      <el-table
        :data="userList"
        border
        row-key="id"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column
          v-if="canDeleteUser"
          type="selection"
          width="52"
          :selectable="isSelectableUser"
        />
        <el-table-column prop="id" label="ID" width="78" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="220" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.role)">
              {{ getRoleLabel(row.role) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="108">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="active"
              inactive-value="inactive"
              :disabled="!canEditUser || row.role === 'ADMIN'"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column prop="updateTime" label="更新时间" min-width="170" />
        <el-table-column label="操作" fixed="right" width="308">
          <template #default="{ row }">
            <div class="table-actions action-buttons">
              <el-button
                v-if="canEditUser"
                type="primary"
                plain
                size="small"
                @click="handleEdit(row)"
              >
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-button
                v-if="canResetUserPassword"
                type="warning"
                plain
                size="small"
                :disabled="row.role === 'ADMIN'"
                @click="handleResetPassword(row)"
              >
                <el-icon><Key /></el-icon>
                重置密码
              </el-button>
              <el-button
                v-if="canDeleteUser"
                type="danger"
                plain
                size="small"
                :disabled="row.role === 'ADMIN'"
                @click="handleDelete(row)"
              >
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="page-pagination-bar">
        <span class="pagination-total">共 {{ total }} 条</span>
        <el-pagination
          background
          layout="sizes, prev, pager, next, jumper"
          :total="total"
          :page-size="pageSize"
          :current-page="currentPage"
          :page-sizes="[10, 20, 50, 100]"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="640px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="userFormRef" :model="userForm" :rules="rules" label-width="96px">
        <div class="dialog-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="isEdit" />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="userForm.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="userForm.phone" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-select v-model="userForm.role" placeholder="请选择角色" style="width: 100%">
              <el-option
                v-for="role in ROLE_OPTIONS"
                :key="role.value"
                :label="role.label"
                :value="role.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="userForm.status">
              <el-radio value="active">启用</el-radio>
              <el-radio value="inactive">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="!isEdit" class="form-item-full" label="密码" prop="password">
            <el-input
              v-model="userForm.password"
              type="password"
              placeholder="请输入初始密码"
              show-password
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetPasswordVisible" title="重置密码" width="420px">
      <el-form ref="resetPasswordRef" :model="resetPasswordForm" :rules="resetPasswordRules" label-width="96px">
        <el-form-item label="用户名">
          <el-input v-model="resetPasswordForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="resetPasswordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="resetPasswordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPasswordVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetPasswordLoading" @click="handleResetPasswordSubmit">
          确认重置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Key, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  batchDeleteUsers,
  createUser,
  deleteUser,
  getUserList,
  resetPassword,
  updateUser,
  updateUserStatus
} from '../../api/user.js'

const ROLE_OPTIONS = [
  { label: '管理员', value: 'ADMIN' },
  { label: '普通用户', value: 'USER' },
  { label: '访客', value: 'GUEST' }
]

const ROLE_LABELS = {
  ADMIN: '管理员',
  USER: '普通用户',
  GUEST: '访客'
}

const ROLE_TAG_TYPES = {
  ADMIN: 'danger',
  USER: 'primary',
  GUEST: 'warning'
}

const hasPermission = inject('hasPermission', () => false)

const canAddUser = computed(() => hasPermission('system:user:add'))
const canEditUser = computed(() => hasPermission('system:user:edit'))
const canDeleteUser = computed(() => hasPermission('system:user:delete'))
const canResetUserPassword = computed(() => hasPermission('system:user:edit'))

const loading = ref(false)
const submitLoading = ref(false)
const resetPasswordLoading = ref(false)
const userList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedIds = ref([])

const searchForm = reactive({
  username: '',
  realName: '',
  role: '',
  status: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const userFormRef = ref(null)
const isEdit = computed(() => !!userForm.id)

const userForm = reactive({
  id: null,
  username: '',
  realName: '',
  email: '',
  phone: '',
  role: 'USER',
  status: 'active',
  password: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { max: 50, message: '真实姓名不能超过 50 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' }
  ]
}

const resetPasswordVisible = ref(false)
const resetPasswordRef = ref(null)
const resetPasswordForm = reactive({
  id: null,
  username: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== resetPasswordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

const resetPasswordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const guestCount = computed(() => userList.value.filter((item) => item.role === 'GUEST').length)

const getRoleLabel = (role) => ROLE_LABELS[role] || role || '-'
const getRoleTagType = (role) => ROLE_TAG_TYPES[role] || 'info'

const isSelectableUser = (row) => row.role !== 'ADMIN'

const loadUserList = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchForm.username) params.username = searchForm.username
    if (searchForm.realName) params.realName = searchForm.realName
    if (searchForm.role) params.role = searchForm.role
    if (searchForm.status) params.status = searchForm.status

    const res = await getUserList(params)
    if (res.code === 200) {
      userList.value = res.data.content || []
      total.value = res.data.totalElements || 0
      return
    }
    ElMessage.error(res.message || '获取用户列表失败')
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadUserList()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.realName = ''
  searchForm.role = ''
  searchForm.status = ''
  handleSearch()
}

const resetForm = () => {
  Object.assign(userForm, {
    id: null,
    username: '',
    realName: '',
    email: '',
    phone: '',
    role: 'USER',
    status: 'active',
    password: ''
  })
}

const handleAdd = () => {
  dialogTitle.value = '新增用户'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑用户'
  Object.assign(userForm, {
    id: row.id,
    username: row.username,
    realName: row.realName || '',
    email: row.email || '',
    phone: row.phone || '',
    role: row.role,
    status: row.status,
    password: ''
  })
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户“${row.username}”吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteUser(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadUserList()
        return
      }
      ElMessage.error(res.message || '删除失败')
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleBatchDelete = () => {
  ElMessageBox.confirm(`确定要删除已选中的 ${selectedIds.value.length} 个用户吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await batchDeleteUsers(selectedIds.value)
      if (res.code === 200) {
        ElMessage.success('批量删除成功')
        selectedIds.value = []
        loadUserList()
        return
      }
      ElMessage.error(res.message || '批量删除失败')
    } catch (error) {
      ElMessage.error('批量删除失败')
    }
  }).catch(() => {})
}

const handleStatusChange = async (row) => {
  const previousStatus = row.status === 'active' ? 'inactive' : 'active'
  try {
    const res = await updateUserStatus(row.id, row.status)
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
      return
    }
    row.status = previousStatus
    ElMessage.error(res.message || '状态更新失败')
  } catch (error) {
    row.status = previousStatus
    ElMessage.error('状态更新失败')
  }
}

const handleResetPassword = (row) => {
  if (row.role === 'ADMIN') {
    ElMessage.warning('不能重置管理员密码')
    return
  }

  resetPasswordForm.id = row.id
  resetPasswordForm.username = row.username
  resetPasswordForm.newPassword = ''
  resetPasswordForm.confirmPassword = ''
  resetPasswordVisible.value = true
}

const handleResetPasswordSubmit = async () => {
  const valid = await resetPasswordRef.value?.validate().catch(() => false)
  if (!valid) return

  resetPasswordLoading.value = true
  try {
    const res = await resetPassword(resetPasswordForm.id, resetPasswordForm.newPassword)
    if (res.code === 200) {
      ElMessage.success('密码重置成功')
      resetPasswordVisible.value = false
      return
    }
    ElMessage.error(res.message || '密码重置失败')
  } catch (error) {
    ElMessage.error('密码重置失败')
  } finally {
    resetPasswordLoading.value = false
  }
}

const handleSubmit = async () => {
  const valid = await userFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    let res
    if (isEdit.value) {
      const updateData = { ...userForm }
      if (!updateData.password) {
        delete updateData.password
      }
      res = await updateUser(userForm.id, updateData)
    } else {
      res = await createUser(userForm)
    }

    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadUserList()
      return
    }
    ElMessage.error(res.message || '保存失败')
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDialogClose = () => {
  userFormRef.value?.resetFields()
  resetForm()
}

const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map((item) => item.id)
}

const handleSizeChange = (size) => {
  pageSize.value = size
  loadUserList()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  loadUserList()
}

onMounted(() => {
  loadUserList()
})
</script>

<style scoped lang="scss">
.user-management-page {
  .page-meta-pills {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 10px;
  }

  .meta-pill {
    display: inline-flex;
    align-items: center;
    min-height: 30px;
    padding: 0 12px;
    border-radius: 999px;
    background: rgba(15, 118, 110, 0.08);
    border: 1px solid rgba(15, 118, 110, 0.16);
    color: var(--app-primary);
    font-size: 13px;
    font-weight: 700;
  }

  .action-buttons {
    justify-content: flex-start;
    flex-wrap: nowrap;
  }

  .action-buttons :deep(.el-button) {
    margin-left: 0;
    min-width: 88px;
  }

  .dialog-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0 16px;
  }

  .form-item-full {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .user-management-page {
    .dialog-grid {
      grid-template-columns: 1fr;
    }
  }
}

@media (max-width: 768px) {
  .user-management-page {
    .action-buttons {
      flex-wrap: wrap;
    }
  }
}
</style>
