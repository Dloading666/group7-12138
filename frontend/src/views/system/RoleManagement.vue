<template>
  <div class="role-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            添加角色
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="角色名称">
          <el-input v-model="searchForm.name" placeholder="请输入角色名称" clearable />
        </el-form-item>
        <el-form-item label="角色编码">
          <el-input v-model="searchForm.code" placeholder="请输入角色编码" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="启用" value="active" />
            <el-option label="禁用" value="inactive" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="roleList" style="width: 100%" border v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" width="150" />
        <el-table-column prop="code" label="角色编码" width="150">
          <template #default="{ row }">
            <el-tag type="info">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              active-value="active"
              inactive-value="inactive"
              @change="handleStatusChange(row)"
              :disabled="row.code === 'ADMIN'"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" fixed="right" width="250">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" size="small" @click="handleAssignPermission(row)">分配权限</el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="handleDelete(row)"
              :disabled="row.code === 'ADMIN' || row.code === 'USER'"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        class="pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </el-card>

    <!-- 角色表单对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="resetForm">
      <el-form :model="roleForm" :rules="rules" ref="roleFormRef" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="roleForm.name" placeholder="请输入角色名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input 
            v-model="roleForm.code" 
            placeholder="请输入角色编码（英文大写）" 
            maxlength="50"
            :disabled="isEdit"
          />
          <div class="form-tip">角色编码创建后不可修改</div>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="roleForm.description" 
            type="textarea" 
            placeholder="请输入描述" 
            maxlength="200"
            show-word-limit
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="roleForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="roleForm.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 分配权限弹窗 -->
    <el-dialog v-model="permissionDialogVisible" title="分配权限" width="600px">
      <div v-if="currentRole" style="margin-bottom: 15px;">
        <strong>当前角色：</strong>
        <el-tag type="primary" style="margin-left: 8px;">{{ currentRole.name }}</el-tag>
        <el-tag type="info" style="margin-left: 8px;">{{ currentRole.code }}</el-tag>
      </div>
      
      <el-tree
        ref="permissionTreeRef"
        :data="permissionTree"
        :props="{ label: 'name', children: 'children' }"
        show-checkbox
        node-key="id"
        default-expand-all
        :default-checked-keys="checkedPermissionIds"
        v-loading="permissionLoading"
      >
        <template #default="{ data }">
          <span>
            {{ data.name }}
            <el-tag size="small" :type="data.type === 'menu' ? 'primary' : data.type === 'button' ? 'success' : 'warning'" style="margin-left: 5px;">
              {{ data.type === 'menu' ? '菜单' : data.type === 'button' ? '按钮' : '接口' }}
            </el-tag>
          </span>
        </template>
      </el-tree>
      
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermissions" :loading="permissionSubmitLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getRoleList, 
  createRole, 
  updateRole, 
  deleteRole,
  updateRoleStatus,
  assignPermissions,
  getRolePermissions
} from '../../api/role.js'
import { getPermissionTree } from '../../api/permission.js'

// 数据
const loading = ref(false)
const submitLoading = ref(false)
const roleList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 分配权限相关
const permissionDialogVisible = ref(false)
const permissionLoading = ref(false)
const permissionSubmitLoading = ref(false)
const permissionTree = ref([])
const checkedPermissionIds = ref([])
const currentRole = ref(null)
const permissionTreeRef = ref(null)

const searchForm = reactive({
  name: '',
  code: '',
  status: ''
})

const dialogVisible = ref(false)
const dialogTitle = ref('添加角色')
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
    { max: 50, message: '角色名称长度不能超过50个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '角色编码只能包含大写字母和下划线', trigger: 'blur' },
    { max: 50, message: '角色编码长度不能超过50个字符', trigger: 'blur' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

// 加载角色列表
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
      roleList.value = res.data.content
      total.value = res.data.totalElements
    } else {
      ElMessage.error(res.message || '获取角色列表失败')
    }
  } catch (error) {
    console.error('获取角色列表失败:', error)
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadRoleList()
}

// 重置
const handleReset = () => {
  searchForm.name = ''
  searchForm.code = ''
  searchForm.status = ''
  currentPage.value = 1
  loadRoleList()
}

// 分页
const handleSizeChange = (val) => {
  pageSize.value = val
  loadRoleList()
}

const handlePageChange = (val) => {
  currentPage.value = val
  loadRoleList()
}

// 添加角色
const handleAdd = () => {
  dialogTitle.value = '添加角色'
  isEdit.value = false
  resetFormData()
  dialogVisible.value = true
}

// 编辑角色
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

// 删除角色
const handleDelete = (row) => {
  if (row.code === 'ADMIN' || row.code === 'USER') {
    ElMessage.warning('系统内置角色不能删除')
    return
  }
  
  ElMessageBox.confirm(
    `确定要删除角色"${row.name}"吗？`, 
    '提示', 
    {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }
  ).then(async () => {
    try {
      const res = await deleteRole(row.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        loadRoleList()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      console.error('删除角色失败:', error)
      ElMessage.error('删除角色失败')
    }
  }).catch(() => {})
}

// 状态变更
const handleStatusChange = async (row) => {
  if (row.code === 'ADMIN') {
    ElMessage.warning('管理员角色不能禁用')
    row.status = 'active'
    return
  }
  
  try {
    const res = await updateRoleStatus(row.id, row.status)
    if (res.code === 200) {
      ElMessage.success(`角色已${row.status === 'active' ? '启用' : '禁用'}`)
    } else {
      // 恢复原状态
      row.status = row.status === 'active' ? 'inactive' : 'active'
      ElMessage.error(res.message || '状态更新失败')
    }
  } catch (error) {
    console.error('状态更新失败:', error)
    row.status = row.status === 'active' ? 'inactive' : 'active'
    ElMessage.error('状态更新失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!roleFormRef.value) return
  
  await roleFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      let res
      if (isEdit.value) {
        res = await updateRole(roleForm.id, roleForm)
      } else {
        res = await createRole(roleForm)
      }
      
      if (res.code === 200) {
        ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
        dialogVisible.value = false
        loadRoleList()
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 重置表单数据
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

// 重置表单
const resetForm = () => {
  roleFormRef.value?.resetFields()
  resetFormData()
}

// 打开分配权限弹窗
const handleAssignPermission = async (row) => {
  currentRole.value = row
  permissionDialogVisible.value = true
  permissionLoading.value = true
  
  try {
    // 并行加载权限树和角色已有权限
    const [treeRes, permRes] = await Promise.all([
      getPermissionTree(),
      getRolePermissions(row.id)
    ])
    
    if (treeRes.code === 200) {
      permissionTree.value = treeRes.data || []
    }
    
    if (permRes.code === 200) {
      checkedPermissionIds.value = permRes.data || []
    }
  } catch (error) {
    console.error('加载权限数据失败:', error)
  } finally {
    permissionLoading.value = false
  }
}

// 保存权限分配
const handleSavePermissions = async () => {
  if (!currentRole.value) return
  
  permissionSubmitLoading.value = true
  try {
    // 获取选中的权限ID（包括半选的父节点）
    const checkedIds = permissionTreeRef.value?.getCheckedKeys() || []
    const halfCheckedIds = permissionTreeRef.value?.getHalfCheckedKeys() || []
    const allIds = [...checkedIds, ...halfCheckedIds]
    
    const res = await assignPermissions(currentRole.value.id, allIds)
    
    if (res.code === 200) {
      ElMessage.success('权限分配成功')
      permissionDialogVisible.value = false
      loadRoleList()
    } else {
      ElMessage.error(res.message || '权限分配失败')
    }
  } catch (error) {
    console.error('权限分配失败:', error)
    ElMessage.error('权限分配失败')
  } finally {
    permissionSubmitLoading.value = false
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 初始化
onMounted(() => {
  loadRoleList()
})
</script>

<style scoped lang="scss">
.role-management {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-form {
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}
</style>
