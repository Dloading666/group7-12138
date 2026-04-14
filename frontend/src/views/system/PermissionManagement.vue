<template>
  <div class="permission-management">
    <el-row :gutter="20">
      <!-- 左侧权限树 -->
      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>权限树</span>
              <el-button type="primary" size="small" @click="handleAddTop">
                <el-icon><Plus /></el-icon>
                添加顶级
              </el-button>
            </div>
          </template>
          
          <el-input
            v-model="filterText"
            placeholder="输入关键字筛选"
            clearable
            style="margin-bottom: 15px;"
          />
          
          <el-tree
            ref="treeRef"
            :data="permissionTree"
            :props="treeProps"
            :filter-node-method="filterNode"
            node-key="id"
            highlight-current
            default-expand-all
            @node-click="handleNodeClick"
          >
            <template #default="{ node, data }">
              <span class="tree-node">
                <el-icon v-if="data.icon" style="margin-right: 5px;"><component :is="data.icon" /></el-icon>
                <span>{{ data.name }}</span>
                <el-tag size="small" :type="getTypeTag(data.type)" style="margin-left: 8px;">
                  {{ getTypeName(data.type) }}
                </el-tag>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>
      
      <!-- 右侧权限详情/编辑 -->
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>{{ formTitle }}</span>
              <div v-if="selectedNode">
                <el-button type="primary" size="small" @click="handleAddChild" v-if="selectedNode.type === 'menu'">
                  添加子权限
                </el-button>
                <el-button type="primary" size="small" @click="handleEdit">
                  编辑
                </el-button>
                <el-button type="danger" size="small" @click="handleDelete">
                  删除
                </el-button>
              </div>
            </div>
          </template>
          
          <el-empty v-if="!selectedNode && !formVisible" description="请在左侧选择权限节点" />
          
          <el-form v-else-if="formVisible" ref="formRef" :model="permissionForm" :rules="rules" label-width="100px">
            <el-form-item label="权限名称" prop="name">
              <el-input v-model="permissionForm.name" placeholder="请输入权限名称" />
            </el-form-item>
            
            <el-form-item label="权限编码" prop="code">
              <el-input v-model="permissionForm.code" placeholder="请输入权限编码，如：system:user:add" />
            </el-form-item>
            
            <el-form-item label="权限类型" prop="type">
              <el-select v-model="permissionForm.type" placeholder="请选择类型" style="width: 100%;">
                <el-option label="菜单" value="menu" />
                <el-option label="按钮" value="button" />
                <el-option label="接口" value="api" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="上级权限">
              <el-tree-select
                v-model="permissionForm.parentId"
                :data="permissionTree"
                :props="{ label: 'name', value: 'id' }"
                placeholder="请选择上级权限"
                clearable
                check-strictly
                style="width: 100%;"
              />
            </el-form-item>
            
            <el-form-item label="路由路径" prop="path" v-if="permissionForm.type === 'menu'">
              <el-input v-model="permissionForm.path" placeholder="请输入路由路径" />
            </el-form-item>
            
            <el-form-item label="图标" v-if="permissionForm.type === 'menu'">
              <el-input v-model="permissionForm.icon" placeholder="请输入图标名称" />
            </el-form-item>
            
            <el-form-item label="排序号">
              <el-input-number v-model="permissionForm.sortOrder" :min="0" :max="999" />
            </el-form-item>
            
            <el-form-item label="状态">
              <el-radio-group v-model="permissionForm.status">
                <el-radio value="active">启用</el-radio>
                <el-radio value="inactive">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
            
            <el-form-item label="描述">
              <el-input v-model="permissionForm.description" type="textarea" :rows="2" placeholder="请输入描述" />
            </el-form-item>
            
            <el-form-item>
              <el-button type="primary" @click="handleSubmit" :loading="submitLoading">保存</el-button>
              <el-button @click="handleCancel">取消</el-button>
            </el-form-item>
          </el-form>
          
          <el-descriptions v-else-if="selectedNode" :column="2" border>
            <el-descriptions-item label="权限名称">{{ selectedNode.name }}</el-descriptions-item>
            <el-descriptions-item label="权限编码">
              <el-tag type="info">{{ selectedNode.code }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="权限类型">
              <el-tag :type="getTypeTag(selectedNode.type)">{{ getTypeName(selectedNode.type) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="selectedNode.status === 'active' ? 'success' : 'danger'">
                {{ selectedNode.status === 'active' ? '启用' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="路由路径">{{ selectedNode.path || '-' }}</el-descriptions-item>
            <el-descriptions-item label="图标">{{ selectedNode.icon || '-' }}</el-descriptions-item>
            <el-descriptions-item label="排序号">{{ selectedNode.sortOrder }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(selectedNode.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ selectedNode.description || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getPermissionTree, 
  createPermission, 
  updatePermission, 
  deletePermission 
} from '../../api/permission.js'

// 树配置
const treeProps = {
  children: 'children',
  label: 'name'
}

const treeRef = ref(null)
const filterText = ref('')
const permissionTree = ref([])
const selectedNode = ref(null)
const formVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)

// 表单标题
const formTitle = computed(() => {
  if (!formVisible.value && selectedNode.value) {
    return '权限详情'
  }
  return isEdit.value ? '编辑权限' : '添加权限'
})

// 表单数据
const permissionForm = reactive({
  id: null,
  name: '',
  code: '',
  type: 'menu',
  parentId: 0,
  path: '',
  icon: '',
  sortOrder: 0,
  status: 'active',
  description: ''
})

// 验证规则
const rules = {
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
  type: [{ required: true, message: '请选择权限类型', trigger: 'change' }]
}

// 筛选树节点
const filterNode = (value, data) => {
  if (!value) return true
  return data.name.includes(value) || data.code.includes(value)
}

watch(filterText, (val) => {
  treeRef.value?.filter(val)
})

// 加载权限树
const loadPermissionTree = async () => {
  try {
    const res = await getPermissionTree()
    if (res.code === 200) {
      permissionTree.value = res.data || []
    }
  } catch (error) {
    console.error('加载权限树失败:', error)
  }
}

// 点击树节点
const handleNodeClick = (data) => {
  selectedNode.value = data
  formVisible.value = false
}

// 添加顶级权限
const handleAddTop = () => {
  isEdit.value = false
  formVisible.value = true
  selectedNode.value = null
  resetForm()
  permissionForm.parentId = 0
}

// 添加子权限
const handleAddChild = () => {
  if (!selectedNode.value) return
  isEdit.value = false
  formVisible.value = true
  resetForm()
  permissionForm.parentId = selectedNode.value.id
  permissionForm.type = selectedNode.value.type === 'menu' ? 'menu' : 'button'
}

// 编辑权限
const handleEdit = () => {
  if (!selectedNode.value) return
  isEdit.value = true
  formVisible.value = true
  Object.assign(permissionForm, {
    id: selectedNode.value.id,
    name: selectedNode.value.name,
    code: selectedNode.value.code,
    type: selectedNode.value.type,
    parentId: selectedNode.value.parentId || 0,
    path: selectedNode.value.path || '',
    icon: selectedNode.value.icon || '',
    sortOrder: selectedNode.value.sortOrder || 0,
    status: selectedNode.value.status,
    description: selectedNode.value.description || ''
  })
}

// 删除权限
const handleDelete = () => {
  if (!selectedNode.value) return
  
  if (selectedNode.value.children && selectedNode.value.children.length > 0) {
    ElMessage.warning('存在子权限，请先删除子权限')
    return
  }
  
  ElMessageBox.confirm(`确定要删除权限"${selectedNode.value.name}"吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deletePermission(selectedNode.value.id)
      if (res.code === 200) {
        ElMessage.success('删除成功')
        selectedNode.value = null
        loadPermissionTree()
      } else {
        ElMessage.error(res.message || '删除失败')
      }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      let res
      if (isEdit.value) {
        res = await updatePermission(permissionForm.id, permissionForm)
      } else {
        res = await createPermission(permissionForm)
      }
      
      if (res.code === 200) {
        ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
        formVisible.value = false
        loadPermissionTree()
        // 更新选中节点
        if (isEdit.value) {
          selectedNode.value = res.data
        }
      } else {
        ElMessage.error(res.message || '操作失败')
      }
    } catch (error) {
      ElMessage.error('操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 取消
const handleCancel = () => {
  formVisible.value = false
  if (isEdit.value && selectedNode.value) {
    // 恢复详情显示
  } else {
    selectedNode.value = null
  }
}

// 重置表单
const resetForm = () => {
  Object.assign(permissionForm, {
    id: null,
    name: '',
    code: '',
    type: 'menu',
    parentId: 0,
    path: '',
    icon: '',
    sortOrder: 0,
    status: 'active',
    description: ''
  })
}

// 类型标签
const getTypeTag = (type) => {
  const map = { menu: 'primary', button: 'success', api: 'warning' }
  return map[type] || 'info'
}

const getTypeName = (type) => {
  const map = { menu: '菜单', button: '按钮', api: '接口' }
  return map[type] || type
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  loadPermissionTree()
})
</script>

<style scoped lang="scss">
.permission-management {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .tree-node {
    display: flex;
    align-items: center;
  }
  
  :deep(.el-tree-node__content) {
    height: 36px;
  }
}
</style>
