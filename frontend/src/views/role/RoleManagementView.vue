<template>
  <div class="resource-page" data-testid="role-management-page">
    <section class="surface-panel toolbar">
      <div class="toolbar-head">
        <div>
          <h1>角色管理</h1>
          <p>维护角色模板、状态和默认权限，作为用户授权的基础配置。</p>
        </div>
        <el-button v-permission="'system:role:create'" type="primary" @click="openCreate">+ 新建角色</el-button>
      </div>
    </section>

    <section class="surface-panel table-shell">
      <el-table :data="rows" :loading="loading" height="420">
        <el-table-column prop="name" label="角色名称" width="160" />
        <el-table-column prop="code" label="角色编码" width="160" />
        <el-table-column prop="description" label="描述" min-width="240" />
        <el-table-column prop="userCount" label="用户数" width="100" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 'active'"
              :disabled="Boolean(row.builtIn)"
              @change="toggleStatus(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editRole(row)">编辑</el-button>
            <el-button
              v-permission="'system:role:assign-permissions'"
              size="small"
              type="primary"
              plain
              @click="openAssign(row)"
            >
              分配权限
            </el-button>
            <el-button
              v-permission="'system:role:delete'"
              size="small"
              type="danger"
              :disabled="Boolean(row.builtIn)"
              @click="removeRole(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="540px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="active">启用</el-radio>
            <el-radio label="inactive">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRole">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="assignVisible" title="分配权限" size="780px">
      <template v-if="assignTarget">
        <div class="scope-summary">
          <div class="scope-user">{{ assignTarget.name }}</div>
          <div class="scope-muted">这里设置的是该角色的默认权限模板，保存后会影响该角色下用户的基础可见范围。</div>
        </div>
        <PermissionTreeSelector
          v-model="assignCheckedIds"
          :tree-data="permissionTree"
          title="角色默认权限"
          description="勾选该角色默认拥有的菜单、按钮和接口权限。"
        />
        <div class="scope-footer">
          <el-button @click="assignVisible = false">取消</el-button>
          <el-button type="primary" :loading="assignSaving" @click="saveAssign">保存权限</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PermissionTreeSelector from '@/components/PermissionTreeSelector.vue'
import { demoPermissions, demoRoles, demoUsers } from '@/mock/demo-data'
import { getUsers } from '@/api/users'
import {
  assignRolePermissions,
  createRole,
  deleteRole,
  getPermissionTree,
  getRolePermissions,
  getRoles,
  updateRole,
  updateRoleStatus
} from '@/api/roles'
import type { PermissionNode, RoleItem } from '@/types/domain'
import { buildRoleUserCountMap } from '@/utils/admin'

const loading = ref(false)
const saving = ref(false)
const assignSaving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新建角色')
const assignVisible = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)
const rows = ref<RoleItem[]>(demoRoles)
const permissionTree = ref<PermissionNode[]>(demoPermissions)
const assignTarget = ref<RoleItem | null>(null)
const assignCheckedIds = ref<number[]>([])

const form = reactive({
  name: '',
  code: '',
  description: '',
  status: 'active' as 'active' | 'inactive'
})

const rules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

async function loadData() {
  loading.value = true
  try {
    const [roleRes, userRes, treeRes] = await Promise.all([getRoles(), getUsers(), getPermissionTree()])
    const users = userRes.data?.list || []
    const roleCounter = buildRoleUserCountMap(users)
    const roles = (roleRes.data?.list || []).map((item) => ({
      ...item,
      userCount: roleCounter.get(item.id) || 0
    }))

    rows.value = roles.length ? roles : demoRoles
    permissionTree.value = treeRes.data?.length ? treeRes.data : demoPermissions
  } catch {
    rows.value = demoRoles
    permissionTree.value = demoPermissions
  } finally {
    loading.value = false
  }
}

function resetFormState() {
  Object.assign(form, { name: '', code: '', description: '', status: 'active' })
  formRef.value?.clearValidate()
}

function openCreate() {
  dialogTitle.value = '新建角色'
  editingId.value = null
  resetFormState()
  dialogVisible.value = true
}

function editRole(row: RoleItem) {
  dialogTitle.value = '编辑角色'
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    code: row.code,
    description: row.description || '',
    status: row.status
  })
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

async function saveRole() {
  if (!formRef.value) return
  await formRef.value.validate()

  saving.value = true
  try {
    if (editingId.value) {
      await updateRole(editingId.value, { ...form })
      ElMessage.success('角色已更新')
    } else {
      await createRole({ ...form })
      ElMessage.success('角色已创建')
    }
    dialogVisible.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row: RoleItem) {
  if (row.builtIn) {
    ElMessage.warning('内置角色不能停用')
    return
  }
  const nextStatus = row.status === 'active' ? 'inactive' : 'active'
  await updateRoleStatus(row.id, nextStatus)
  row.status = nextStatus
  ElMessage.success(nextStatus === 'active' ? '角色已启用' : '角色已禁用')
}

async function removeRole(row: RoleItem) {
  if (row.builtIn) {
    ElMessage.warning('内置角色不能删除')
    return
  }

  try {
    await ElMessageBox.confirm(`确认删除角色「${row.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }

  await deleteRole(row.id)
  ElMessage.success('角色已删除')
  await loadData()
}

async function openAssign(row: RoleItem) {
  assignTarget.value = row
  const res = await getRolePermissions(row.id)
  assignCheckedIds.value = res.data?.permissionIds || []
  permissionTree.value = res.data?.tree?.length ? res.data.tree : demoPermissions
  assignVisible.value = true
}

async function saveAssign() {
  if (!assignTarget.value) return

  assignSaving.value = true
  try {
    await assignRolePermissions(assignTarget.value.id, assignCheckedIds.value)
    ElMessage.success('权限已保存')
    assignVisible.value = false
  } finally {
    assignSaving.value = false
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
