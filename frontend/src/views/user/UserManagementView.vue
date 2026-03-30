<template>
  <div class="resource-page" data-testid="user-management-page">
    <section class="surface-panel toolbar">
      <div class="toolbar-head">
        <div>
          <h1>用户管理</h1>
          <p>创建账号、分配角色，并按用户维度设置最终权限范围。</p>
        </div>
        <el-button v-permission="'system:user:create'" type="primary" @click="openCreate">+ 新建用户</el-button>
      </div>

      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="用户名">
          <el-input v-model="filters.username" clearable placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="filters.realName" clearable placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="filters.roleId" clearable placeholder="全部角色" style="width: 160px">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 140px">
            <el-option label="启用" value="active" />
            <el-option label="禁用" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="surface-panel table-shell">
      <el-table :data="filteredRows" :loading="loading" height="560">
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="roleName" label="角色" width="140" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="email" label="邮箱" min-width="220" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'active'"
              :disabled="isProtected(row)"
              @change="toggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" width="180">
          <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="380" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editUser(row)">编辑</el-button>
            <el-button
              v-permission="'system:user:assign-scope'"
              size="small"
              type="primary"
              plain
              @click="openScope(row)"
            >
              权限范围
            </el-button>
            <el-button
              v-permission="'system:user:reset-password'"
              size="small"
              type="warning"
              plain
              @click="openResetPassword(row)"
            >
              重置密码
            </el-button>
            <el-button
              v-permission="'system:user:delete'"
              size="small"
              type="danger"
              :disabled="isProtected(row)"
              @click="removeUser(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="Boolean(editingId)" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" style="width: 100%" placeholder="请选择角色">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="active">启用</el-radio>
            <el-radio label="inactive">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!editingId" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入初始密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <el-form ref="resetRef" :model="resetForm" :rules="resetRules" label-width="96px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetForm.password" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="submitResetPassword">确认</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="scopeVisible" title="权限范围" size="780px">
      <template v-if="scopeTarget">
        <div class="scope-summary">
          <div class="scope-user">{{ scopeTarget.realName || scopeTarget.username }}</div>
          <div class="scope-muted">以角色默认权限为基础，勾选后将生成该用户的最终有效权限。</div>
        </div>
        <PermissionTreeSelector
          v-model="scopeCheckedIds"
          :tree-data="permissionTree"
          title="最终权限范围"
          description="勾选后将直接作为该用户的最终可见菜单与操作权限。"
        />
        <div class="scope-footer">
          <el-button @click="scopeVisible = false">取消</el-button>
          <el-button type="primary" :loading="scopeSaving" @click="saveScope">保存权限范围</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PermissionTreeSelector from '@/components/PermissionTreeSelector.vue'
import { demoPermissions, demoRoles, demoUsers } from '@/mock/demo-data'
import { getRoles } from '@/api/roles'
import {
  createUser,
  deleteUser,
  getPermissionTree,
  getUserPermissionOverrides,
  getUsers,
  resetPassword,
  updateUser,
  updateUserPermissionOverrides,
  updateUserStatus
} from '@/api/users'
import type { PermissionNode, RoleItem, UserItem } from '@/types/domain'
import { formatDateTime } from '@/utils/format'
import { diffPermissions } from '@/utils/permission'
import { buildRoleUserCountMap, flattenPermissionIds } from '@/utils/admin'

const loading = ref(false)
const saving = ref(false)
const resetting = ref(false)
const scopeSaving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建用户')
const resetVisible = ref(false)
const scopeVisible = ref(false)
const formRef = ref<FormInstance>()
const resetRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const resetTarget = ref<UserItem | null>(null)
const scopeTarget = ref<UserItem | null>(null)
const rows = ref<UserItem[]>(demoUsers)
const roleOptions = ref<RoleItem[]>(demoRoles)
const permissionTree = ref<PermissionNode[]>(demoPermissions)
const scopeCheckedIds = ref<number[]>([])
const scopeBaseIds = ref<number[]>([])

const filters = reactive({
  username: '',
  realName: '',
  roleId: undefined as number | undefined,
  status: ''
})

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  roleId: undefined as number | undefined,
  status: 'active' as 'active' | 'inactive',
  password: ''
})

const resetForm = reactive({ password: '' })

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const resetRules: FormRules<typeof resetForm> = {
  password: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
}

const filteredRows = computed(() =>
  rows.value.filter((row) => {
    const matchUsername = !filters.username || row.username.includes(filters.username)
    const matchName = !filters.realName || (row.realName || '').includes(filters.realName)
    const matchRole = !filters.roleId || row.roleId === filters.roleId
    const matchStatus = !filters.status || row.status === filters.status
    return matchUsername && matchName && matchRole && matchStatus
  })
)

function isProtected(row: UserItem) {
  return Boolean(row.superAdmin) || row.username === 'admin' || row.roleCode === 'ADMIN'
}

async function loadData() {
  loading.value = true
  try {
    const [userRes, roleRes, permissionRes] = await Promise.all([getUsers(), getRoles(), getPermissionTree()])
    const users = userRes.data?.list || []
    const roleCounter = buildRoleUserCountMap(users)
    const roles = (roleRes.data?.list || []).map((item) => ({
      ...item,
      userCount: roleCounter.get(item.id) || 0
    }))

    rows.value = users.length ? users : demoUsers
    roleOptions.value = roles.length ? roles : demoRoles
    permissionTree.value = permissionRes.data?.length ? permissionRes.data : demoPermissions
  } catch {
    rows.value = demoUsers
    roleOptions.value = demoRoles
    permissionTree.value = demoPermissions
  } finally {
    loading.value = false
  }
}

function search() {
  void loadData()
}

function reset() {
  filters.username = ''
  filters.realName = ''
  filters.roleId = undefined
  filters.status = ''
  void loadData()
}

function resetFormState() {
  Object.assign(form, {
    username: '',
    realName: '',
    phone: '',
    email: '',
    roleId: undefined,
    status: 'active',
    password: ''
  })
  formRef.value?.clearValidate()
}

function openCreate() {
  dialogTitle.value = '新建用户'
  editingId.value = null
  resetFormState()
  dialogVisible.value = true
}

function editUser(row: UserItem) {
  dialogTitle.value = '编辑用户'
  editingId.value = row.id
  Object.assign(form, {
    username: row.username,
    realName: row.realName || '',
    phone: row.phone || '',
    email: row.email || '',
    roleId: row.roleId,
    status: row.status,
    password: ''
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function saveUser() {
  if (!formRef.value) return
  await formRef.value.validate()

  saving.value = true
  try {
    if (editingId.value) {
      await updateUser(editingId.value, {
        username: form.username.trim(),
        realName: form.realName.trim(),
        phone: form.phone.trim(),
        email: form.email.trim(),
        roleId: form.roleId,
        status: form.status
      })
      ElMessage.success('用户已更新')
    } else {
      await createUser({
        username: form.username.trim(),
        realName: form.realName.trim(),
        phone: form.phone.trim(),
        email: form.email.trim(),
        roleId: form.roleId,
        status: form.status,
        password: form.password
      })
      ElMessage.success('用户已创建')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: UserItem) {
  if (isProtected(row)) return
  const nextStatus = row.status === 'active' ? 'inactive' : 'active'
  await updateUserStatus(row.id, nextStatus)
  row.status = nextStatus
  ElMessage.success(nextStatus === 'active' ? '用户已启用' : '用户已禁用')
}

async function removeUser(row: UserItem) {
  if (isProtected(row)) {
    ElMessage.warning('固定管理员账号不能删除')
    return
  }

  try {
    await ElMessageBox.confirm(`确认删除用户「${row.username}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }

  await deleteUser(row.id)
  ElMessage.success('用户已删除')
  await loadData()
}

function openResetPassword(row: UserItem) {
  resetTarget.value = row
  resetForm.password = ''
  resetRef.value?.clearValidate()
  resetVisible.value = true
}

async function submitResetPassword() {
  if (!resetRef.value || !resetTarget.value) return
  await resetRef.value.validate()

  resetting.value = true
  try {
    await resetPassword(resetTarget.value.id, resetForm.password)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } finally {
    resetting.value = false
  }
}

async function openScope(row: UserItem) {
  scopeTarget.value = row
  const scopeRes = await getUserPermissionOverrides(row.id)
  const scope = scopeRes.data

  scopeBaseIds.value = scope?.rolePermissionIds || []
  scopeCheckedIds.value = scope?.effectivePermissionIds || []

  if (!scopeCheckedIds.value.length) {
    scopeCheckedIds.value = [...scopeBaseIds.value]
  }
  if (!permissionTree.value.length) {
    permissionTree.value = demoPermissions
    scopeCheckedIds.value = flattenPermissionIds(permissionTree.value)
  }

  scopeVisible.value = true
}

async function saveScope() {
  if (!scopeTarget.value) return

  scopeSaving.value = true
  try {
    const { grants, revokes } = diffPermissions(scopeBaseIds.value, scopeCheckedIds.value)
    await updateUserPermissionOverrides(scopeTarget.value.id, { grants, revokes })
    ElMessage.success('权限范围已保存')
    scopeVisible.value = false
  } finally {
    scopeSaving.value = false
  }
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped lang="scss">
.resource-page {
  display: grid;
  gap: 16px;
}

.toolbar,
.table-shell {
  padding: 20px;
}

.toolbar-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.toolbar-head h1,
.toolbar-head p {
  margin: 0;
}

.toolbar-head p {
  margin-top: 6px;
  color: var(--app-text-muted);
}

.scope-summary {
  margin-bottom: 18px;
}

.scope-user {
  font-size: 22px;
  font-weight: 700;
}

.scope-muted {
  color: var(--app-text-muted);
  margin-top: 8px;
}

.scope-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
</style>
