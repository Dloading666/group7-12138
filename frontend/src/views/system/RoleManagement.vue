<template>
  <div class="app-page role-management-page">
    <section class="page-header-bar">
      <div class="page-title-block">
        <h2>角色管理</h2>
        <p>管理员可为普通用户与访客角色分配访问权限，未勾选的页面和操作默认不可访问、不可修改。</p>
        <div class="page-meta-pills">
          <span class="meta-pill">总角色 {{ total }}</span>
          <span class="meta-pill">当前页 {{ roleList.length }}</span>
          <span class="meta-pill">系统内置 {{ builtinRoleCodes.length }}</span>
          <span class="meta-pill">已启用 {{ activeRoleCount }}</span>
        </div>
      </div>
      <div class="page-header-actions">
        <el-button v-if="canAddRole" type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增角色
        </el-button>
      </div>
    </section>

    <section class="page-section">
      <div class="page-filter-bar">
        <div class="form-field-inline">
          <span class="label">角色名称</span>
          <el-input
            v-model="searchForm.name"
            placeholder="按角色名称筛选"
            clearable
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="form-field-inline">
          <span class="label">角色编码</span>
          <el-input
            v-model="searchForm.code"
            placeholder="按角色编码筛选"
            clearable
            @keyup.enter="handleSearch"
          />
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
        </div>
      </div>
    </section>

    <section class="page-section page-table-card">
      <el-table :data="roleList" border row-key="id" v-loading="loading">
        <el-table-column prop="id" label="ID" width="78" />
        <el-table-column prop="name" label="角色名称" min-width="160" />
        <el-table-column prop="code" label="角色编码" width="130">
          <template #default="{ row }">
            <el-tag :type="getRoleTagType(row.code)">
              {{ row.code }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="88" />
        <el-table-column prop="status" label="状态" width="108">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="active"
              inactive-value="inactive"
              :disabled="!canEditRole || row.code === 'ADMIN'"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="344">
          <template #default="{ row }">
            <div class="table-actions action-buttons">
              <el-button
                v-if="canEditRole"
                type="primary"
                plain
                size="small"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-if="canAssignRolePermission"
                type="success"
                plain
                size="small"
                @click="handleAssignPermission(row)"
              >
                分配权限
              </el-button>
              <el-button
                v-if="canDeleteRole"
                type="danger"
                plain
                size="small"
                :disabled="isBuiltinRole(row.code)"
                @click="handleDelete(row)"
              >
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
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="620px"
      destroy-on-close
      @close="resetForm"
    >
      <el-form ref="roleFormRef" :model="roleForm" :rules="rules" label-width="96px">
        <div class="dialog-grid">
          <el-form-item label="角色名称" prop="name">
            <el-input v-model="roleForm.name" placeholder="请输入角色名称" maxlength="50" />
          </el-form-item>
          <el-form-item label="角色编码" prop="code">
            <el-input
              v-model="roleForm.code"
              placeholder="请输入大写角色编码"
              maxlength="50"
              :disabled="isEdit"
            />
            <div class="form-tip">角色编码创建后不可修改，建议使用 `ADMIN / USER / GUEST` 这类大写格式。</div>
          </el-form-item>
          <el-form-item class="form-item-full" label="描述" prop="description">
            <el-input
              v-model="roleForm.description"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="请输入角色描述"
            />
          </el-form-item>
          <el-form-item label="排序" prop="sortOrder">
            <el-input-number v-model="roleForm.sortOrder" :min="0" :max="999" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="roleForm.status">
              <el-radio value="active">启用</el-radio>
              <el-radio value="inactive">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="permissionDialogVisible"
      title="分配权限"
      width="840px"
      destroy-on-close
      @close="resetPermissionDialog"
    >
      <div v-if="currentRole" class="permission-dialog-header">
        <div>
          <div class="permission-dialog-title">角色权限树</div>
          <p class="permission-dialog-desc">勾选后角色才拥有对应页面入口、访问能力和按钮操作能力。</p>
        </div>
        <div class="permission-role-summary">
          <el-tag size="large" :type="getRoleTagType(currentRole.code)">{{ currentRole.name }}</el-tag>
          <el-tag size="large" type="info">{{ currentRole.code }}</el-tag>
          <span class="selection-count">已选 {{ selectedPermissionCount }} 项</span>
        </div>
      </div>

      <div class="permission-toolbar">
        <el-input
          v-model="permissionKeyword"
          placeholder="按权限名称或编码筛选"
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <div class="permission-toolbar-actions">
          <el-button @click="expandAllPermissionNodes">展开全部</el-button>
          <el-button @click="collapseAllPermissionNodes">收起全部</el-button>
          <el-button type="primary" plain @click="handleSelectAllPermissions">全选</el-button>
          <el-button @click="handleClearPermissions">清空</el-button>
        </div>
      </div>

      <div class="permission-tree-card" v-loading="permissionLoading">
        <el-scrollbar max-height="420px">
          <el-tree
            ref="permissionTreeRef"
            :data="permissionTree"
            :props="{ label: 'name', children: 'children' }"
            :filter-node-method="filterPermissionNode"
            show-checkbox
            node-key="id"
            default-expand-all
            check-on-click-node
            @check="updatePermissionSelectionState"
          >
            <template #default="{ data }">
              <div class="permission-tree-node">
                <div class="permission-tree-node-main">
                  <span class="permission-tree-node-name">{{ data.name }}</span>
                  <el-tag size="small" :type="getPermissionTypeTag(data.type)">
                    {{ getPermissionTypeLabel(data.type) }}
                  </el-tag>
                </div>
                <span class="permission-tree-node-code">{{ data.code }}</span>
              </div>
            </template>
          </el-tree>
        </el-scrollbar>
      </div>

      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permissionSubmitLoading" @click="handleSavePermissions">
          保存权限
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  assignPermissions,
  createRole,
  deleteRole,
  getRoleList,
  getRolePermissions,
  updateRole,
  updateRoleStatus
} from '../../api/role.js'
import { getPermissionTree } from '../../api/permission.js'

const builtinRoleCodes = ['ADMIN', 'USER', 'GUEST']

const hasPermission = inject('hasPermission', () => false)

const canAddRole = computed(() => hasPermission('system:role:add'))
const canEditRole = computed(() => hasPermission('system:role:edit'))
const canDeleteRole = computed(() => hasPermission('system:role:delete'))
const canAssignRolePermission = computed(() => hasPermission('system:role:permission'))

const loading = ref(false)
const submitLoading = ref(false)
const roleList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const permissionDialogVisible = ref(false)
const permissionLoading = ref(false)
const permissionSubmitLoading = ref(false)
const permissionTree = ref([])
const checkedPermissionIds = ref([])
const selectedPermissionCount = ref(0)
const currentRole = ref(null)
const permissionTreeRef = ref(null)
const permissionKeyword = ref('')

const searchForm = reactive({
  name: '',
  code: '',
  status: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const isEdit = ref(false)
const roleFormRef = ref(null)

const roleForm = reactive({
  id: null,
  name: '',
  code: '',
  description: '',
  sortOrder: 0,
  status: 'active'
})

const rules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 50, message: '角色名称不能超过 50 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '角色编码只能包含大写字母和下划线', trigger: 'blur' },
    { max: 50, message: '角色编码不能超过 50 个字符', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const activeRoleCount = computed(() => roleList.value.filter((item) => item.status === 'active').length)

const getRoleTagType = (roleCode) => {
  if (roleCode === 'ADMIN') return 'danger'
  if (roleCode === 'USER') return 'primary'
  if (roleCode === 'GUEST') return 'warning'
  return 'info'
}

const getPermissionTypeTag = (type) => {
  if (type === 'menu') return 'primary'
  if (type === 'button') return 'success'
  return 'warning'
}

const getPermissionTypeLabel = (type) => {
  if (type === 'menu') return '菜单'
  if (type === 'button') return '按钮'
  return '接口'
}

const isBuiltinRole = (code) => builtinRoleCodes.includes(code)

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const flattenPermissionIds = (nodes) => nodes.flatMap((node) => [
  node.id,
  ...flattenPermissionIds(node.children || [])
])

const loadRoleList = async () => {
  loading.value = true
  try {
    const res = await getRoleList({
      name: searchForm.name || undefined,
      code: searchForm.code || undefined,
      status: searchForm.status || undefined,
      page: currentPage.value,
      size: pageSize.value
    })

    if (res.code === 200) {
      roleList.value = res.data.content || []
      total.value = res.data.totalElements || 0
      return
    }
    ElMessage.error(res.message || '获取角色列表失败')
  } catch (error) {
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadRoleList()
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.code = ''
  searchForm.status = ''
  handleSearch()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  loadRoleList()
}

const handlePageChange = (val) => {
  currentPage.value = val
  loadRoleList()
}

const resetFormData = () => {
  Object.assign(roleForm, {
    id: null,
    name: '',
    code: '',
    description: '',
    sortOrder: 0,
    status: 'active'
  })
}

const handleAdd = () => {
  dialogTitle.value = '新增角色'
  isEdit.value = false
  resetFormData()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑角色'
  isEdit.value = true
  Object.assign(roleForm, {
    id: row.id,
    name: row.name,
    code: row.code,
    description: row.description,
    sortOrder: row.sortOrder || 0,
    status: row.status
  })
  dialogVisible.value = true
}

const handleDelete = (row) => {
  if (isBuiltinRole(row.code)) {
    ElMessage.warning('系统内置角色不允许删除')
    return
  }

  ElMessageBox.confirm(`确定要删除角色“${row.name}”吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      const res = await deleteRole(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadRoleList()
        return
      }
      ElMessage.error(res.message || '删除失败')
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

const handleStatusChange = async (row) => {
  if (row.code === 'ADMIN') {
    ElMessage.warning('管理员角色不能被禁用')
    row.status = 'active'
    return
  }

  const previousStatus = row.status === 'active' ? 'inactive' : 'active'
  try {
    const res = await updateRoleStatus(row.id, row.status)
    if (res.code === 200) {
      ElMessage.success(`角色已${row.status === 'active' ? '启用' : '禁用'}`)
      return
    }
    row.status = previousStatus
    ElMessage.error(res.message || '状态更新失败')
  } catch (error) {
    row.status = previousStatus
    ElMessage.error('状态更新失败')
  }
}

const handleSubmit = async () => {
  const valid = await roleFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const res = isEdit.value
      ? await updateRole(roleForm.id, roleForm)
      : await createRole(roleForm)

    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadRoleList()
      return
    }
    ElMessage.error(res.message || '保存失败')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    submitLoading.value = false
  }
}

const resetForm = () => {
  roleFormRef.value?.resetFields()
  resetFormData()
}

const filterPermissionNode = (value, data) => {
  if (!value) return true
  const keyword = value.toLowerCase()
  return `${data.name || ''} ${data.code || ''}`.toLowerCase().includes(keyword)
}

const updatePermissionSelectionState = () => {
  const checkedIds = permissionTreeRef.value?.getCheckedKeys() || []
  const halfCheckedIds = permissionTreeRef.value?.getHalfCheckedKeys() || []
  selectedPermissionCount.value = new Set([...checkedIds, ...halfCheckedIds]).size
}

const applyCheckedPermissionIds = async () => {
  await nextTick()
  permissionTreeRef.value?.setCheckedKeys(checkedPermissionIds.value, false)
  updatePermissionSelectionState()
}

const setAllTreeNodesExpanded = (expanded) => {
  const nodesMap = permissionTreeRef.value?.store?.nodesMap || {}
  Object.values(nodesMap).forEach((node) => {
    if (node.level > 0) {
      node.expanded = expanded
    }
  })
}

const expandAllPermissionNodes = () => {
  setAllTreeNodesExpanded(true)
}

const collapseAllPermissionNodes = () => {
  setAllTreeNodesExpanded(false)
}

const handleSelectAllPermissions = () => {
  const allIds = flattenPermissionIds(permissionTree.value)
  permissionTreeRef.value?.setCheckedKeys(allIds, false)
  updatePermissionSelectionState()
}

const handleClearPermissions = () => {
  permissionTreeRef.value?.setCheckedKeys([], false)
  updatePermissionSelectionState()
}

const resetPermissionDialog = () => {
  permissionKeyword.value = ''
  permissionTree.value = []
  checkedPermissionIds.value = []
  currentRole.value = null
  selectedPermissionCount.value = 0
}

const handleAssignPermission = async (row) => {
  currentRole.value = row
  permissionDialogVisible.value = true
  permissionLoading.value = true
  permissionKeyword.value = ''
  checkedPermissionIds.value = []

  try {
    const [treeRes, permRes] = await Promise.all([
      getPermissionTree(),
      getRolePermissions(row.id)
    ])

    if (treeRes.code !== 200) {
      ElMessage.error(treeRes.message || '加载权限树失败')
      return
    }

    if (permRes.code !== 200) {
      ElMessage.error(permRes.message || '加载角色权限失败')
      return
    }

    permissionTree.value = treeRes.data || []
    checkedPermissionIds.value = permRes.data || []
    await applyCheckedPermissionIds()
    expandAllPermissionNodes()
  } catch (error) {
    ElMessage.error('加载权限数据失败')
  } finally {
    permissionLoading.value = false
  }
}

const handleSavePermissions = async () => {
  if (!currentRole.value) return

  permissionSubmitLoading.value = true
  try {
    const checkedIds = permissionTreeRef.value?.getCheckedKeys() || []
    const halfCheckedIds = permissionTreeRef.value?.getHalfCheckedKeys() || []
    const allIds = [...new Set([...checkedIds, ...halfCheckedIds])]

    const res = await assignPermissions(currentRole.value.id, allIds)
    if (res.code === 200) {
      ElMessage.success('权限分配成功')
      permissionDialogVisible.value = false
      loadRoleList()
      return
    }
    ElMessage.error(res.message || '权限分配失败')
  } catch (error) {
    ElMessage.error('权限分配失败')
  } finally {
    permissionSubmitLoading.value = false
  }
}

watch(permissionKeyword, (value) => {
  permissionTreeRef.value?.filter(value)
})

onMounted(() => {
  loadRoleList()
})
</script>

<style scoped lang="scss">
.role-management-page {
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
    background: rgba(21, 94, 239, 0.08);
    border: 1px solid rgba(21, 94, 239, 0.14);
    color: var(--app-accent);
    font-size: 13px;
    font-weight: 700;
  }

  .action-buttons {
    justify-content: flex-start;
    flex-wrap: nowrap;
  }

  .action-buttons :deep(.el-button) {
    margin-left: 0;
    min-width: 92px;
  }

  .dialog-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 0 16px;
  }

  .form-item-full {
    grid-column: 1 / -1;
  }

  .form-tip {
    margin-top: 6px;
    color: var(--app-text-muted);
    font-size: 12px;
    line-height: 1.5;
  }

  .permission-dialog-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
  }

  .permission-dialog-title {
    font-size: 18px;
    font-weight: 700;
    color: var(--app-text);
  }

  .permission-dialog-desc {
    margin: 6px 0 0;
    color: var(--app-text-muted);
    line-height: 1.6;
  }

  .permission-role-summary {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: flex-end;
    gap: 8px;
  }

  .selection-count {
    padding: 0 12px;
    min-height: 32px;
    display: inline-flex;
    align-items: center;
    border-radius: 999px;
    background: rgba(15, 118, 110, 0.08);
    color: var(--app-primary);
    font-size: 13px;
    font-weight: 700;
  }

  .permission-toolbar {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    gap: 12px;
    margin-bottom: 16px;
  }

  .permission-toolbar-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: flex-end;
  }

  .permission-tree-card {
    border: 1px solid rgba(219, 228, 239, 0.95);
    border-radius: 16px;
    background: rgba(248, 250, 252, 0.76);
    padding: 14px;
  }

  .permission-tree-node {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 14px;
    width: 100%;
    padding-right: 10px;
  }

  .permission-tree-node-main {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .permission-tree-node-name {
    font-weight: 600;
    color: var(--app-text);
  }

  .permission-tree-node-code {
    color: var(--app-text-muted);
    font-size: 12px;
    word-break: break-all;
  }
}

@media (max-width: 960px) {
  .role-management-page {
    .dialog-grid {
      grid-template-columns: 1fr;
    }

    .permission-dialog-header,
    .permission-toolbar {
      grid-template-columns: 1fr;
      display: block;
    }

    .permission-role-summary {
      margin-top: 12px;
      justify-content: flex-start;
    }

    .permission-toolbar-actions {
      margin-top: 12px;
      justify-content: flex-start;
    }
  }
}

@media (max-width: 768px) {
  .role-management-page {
    .action-buttons {
      flex-wrap: wrap;
    }

    .permission-tree-node {
      flex-direction: column;
      align-items: flex-start;
    }
  }
}
</style>
