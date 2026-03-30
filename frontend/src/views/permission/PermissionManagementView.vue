<template>
  <div class="permission-page" data-testid="permission-management-page">
    <section class="surface-panel tree-panel">
      <div class="panel-head">
        <div>
          <h1>权限管理</h1>
          <p>维护菜单、按钮和接口权限节点，统一作为角色授权来源。</p>
        </div>
        <div class="actions">
          <el-button type="primary" @click="openCreateRoot">+ 新建根节点</el-button>
        </div>
      </div>

      <div class="tree-shell">
        <el-tree :data="rows" node-key="id" default-expand-all highlight-current @node-click="selectNode">
          <template #default="{ data }">
            <span class="node-row">
              <el-tag size="small" :type="tagType(data.type)">{{ data.type }}</el-tag>
              <span class="node-name">{{ data.name }}</span>
              <span class="node-code">{{ data.code }}</span>
            </span>
          </template>
        </el-tree>
      </div>
    </section>

    <section class="surface-panel detail-panel">
      <div class="panel-head">
        <div>
          <h2>{{ editing ? '权限编辑' : currentNode ? '节点详情' : '权限编辑' }}</h2>
          <p>{{ editing ? '修改节点信息后保存即可立即生效。' : currentNode ? '可查看当前节点信息，并继续新增子节点或编辑。' : '请先在左侧选择一个权限节点。' }}</p>
        </div>
        <div class="actions" v-if="currentNode && !editing">
          <el-switch
            :model-value="currentNode.status === 'active'"
            inline-prompt
            active-text="启用"
            inactive-text="停用"
            @change="toggleNodeStatus(currentNode)"
          />
          <el-button v-if="currentNode.type === 'MENU'" type="primary" plain @click="openAddChild(currentNode)">+ 子节点</el-button>
          <el-button @click="openEdit(currentNode)">编辑</el-button>
          <el-button type="danger" @click="removeNode(currentNode)">删除</el-button>
        </div>
      </div>

      <el-empty v-if="!editing && !currentNode" description="请先选择一个权限节点" />

      <div v-else-if="!editing && currentNode" class="node-detail">
        <div class="detail-grid">
          <div>
            <span>名称</span>
            <strong>{{ currentNode.name }}</strong>
          </div>
          <div>
            <span>编码</span>
            <strong>{{ currentNode.code }}</strong>
          </div>
          <div>
            <span>类型</span>
            <strong>{{ currentNode.type }}</strong>
          </div>
          <div>
            <span>父级</span>
            <strong>{{ parentLabelMap.get(currentNode.parentId || 0) || '根节点' }}</strong>
          </div>
          <div>
            <span>路径</span>
            <strong>{{ currentNode.path || '-' }}</strong>
          </div>
          <div>
            <span>组件</span>
            <strong>{{ currentNode.component || '-' }}</strong>
          </div>
          <div>
            <span>图标</span>
            <strong>{{ currentNode.icon || '-' }}</strong>
          </div>
          <div>
            <span>排序</span>
            <strong>{{ currentNode.sortOrder ?? 0 }}</strong>
          </div>
        </div>
      </div>

      <el-form v-else ref="formRef" :model="form" :rules="rules" label-width="96px" class="edit-form">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入权限名称" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="例如 system:user:view" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%" placeholder="请选择类型">
            <el-option label="MENU" value="MENU" />
            <el-option label="BUTTON" value="BUTTON" />
            <el-option label="API" value="API" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级">
          <el-input :model-value="editingParentLabel" disabled />
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="form.path" placeholder="请输入路由路径" />
        </el-form-item>
        <el-form-item label="组件">
          <el-input v-model="form.component" placeholder="请输入组件标识" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="请输入图标名称" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :step="1" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="active">启用</el-radio>
            <el-radio label="inactive">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="saveNode">保存</el-button>
          <el-button @click="cancelEdit">取消</el-button>
        </el-form-item>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { demoPermissions } from '@/mock/demo-data'
import { createPermission, deletePermission, getPermissionTree, updatePermission, updatePermissionStatus } from '@/api/permissions'
import type { PermissionNode } from '@/types/domain'
import { collectPermissionLabelMap } from '@/utils/admin'

const rows = ref<PermissionNode[]>(demoPermissions)
const currentNode = ref<PermissionNode | null>(null)
const editing = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: undefined as number | undefined,
  name: '',
  code: '',
  type: 'MENU' as PermissionNode['type'],
  parentId: null as number | null,
  path: '',
  component: '',
  icon: '',
  sortOrder: 0,
  status: 'active' as 'active' | 'inactive'
})

const rules: FormRules<typeof form> = {
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入权限编码', trigger: 'blur' }],
  type: [{ required: true, message: '请选择权限类型', trigger: 'change' }]
}

const parentLabelMap = computed(() => collectPermissionLabelMap(rows.value))
const editingParentLabel = computed(() => {
  if (form.parentId == null) return '根节点'
  return parentLabelMap.value.get(form.parentId) || '根节点'
})

async function loadData() {
  try {
    const res = await getPermissionTree()
    const tree = res.data || []
    rows.value = tree.length ? tree : demoPermissions
  } catch {
    rows.value = demoPermissions
  }
}

function resetFormState() {
  Object.assign(form, {
    id: undefined,
    name: '',
    code: '',
    type: 'MENU',
    parentId: null,
    path: '',
    component: '',
    icon: '',
    sortOrder: 0,
    status: 'active'
  })
  formRef.value?.clearValidate()
}

function selectNode(node: PermissionNode) {
  currentNode.value = node
  editing.value = false
}

function openCreateRoot() {
  currentNode.value = null
  editing.value = true
  resetFormState()
}

function openAddChild(parent: PermissionNode) {
  currentNode.value = parent
  editing.value = true
  Object.assign(form, {
    id: undefined,
    name: '',
    code: '',
    type: parent.type === 'MENU' ? 'MENU' : 'BUTTON',
    parentId: parent.id,
    path: '',
    component: '',
    icon: '',
    sortOrder: 0,
    status: 'active'
  })
  formRef.value?.clearValidate()
}

function openEdit(node: PermissionNode) {
  editing.value = true
  Object.assign(form, {
    id: node.id,
    name: node.name,
    code: node.code,
    type: node.type,
    parentId: node.parentId ?? null,
    path: node.path || '',
    component: node.component || '',
    icon: node.icon || '',
    sortOrder: node.sortOrder ?? 0,
    status: node.status || 'active'
  })
  formRef.value?.clearValidate()
}

function cancelEdit() {
  editing.value = false
}

async function saveNode() {
  if (!formRef.value) return
  await formRef.value.validate()

  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      code: form.code.trim(),
      type: form.type,
      parentId: form.parentId,
      path: form.path.trim(),
      component: form.component.trim(),
      icon: form.icon.trim(),
      sortOrder: form.sortOrder,
      status: form.status
    }

    if (form.id) {
      await updatePermission(form.id, payload)
      ElMessage.success('权限已更新')
    } else {
      await createPermission(payload)
      ElMessage.success('权限已创建')
    }

    editing.value = false
    await loadData()
  } finally {
    saving.value = false
  }
}

async function toggleNodeStatus(node: PermissionNode) {
  const nextStatus = node.status === 'active' ? 'inactive' : 'active'
  await updatePermissionStatus(node.id, nextStatus)
  node.status = nextStatus
  ElMessage.success(nextStatus === 'active' ? '权限已启用' : '权限已停用')
}

async function removeNode(node: PermissionNode) {
  try {
    await ElMessageBox.confirm(`确认删除权限「${node.name}」吗？`, '提示', { type: 'warning' })
  } catch {
    return
  }

  await deletePermission(node.id)
  ElMessage.success('权限已删除')
  currentNode.value = null
  editing.value = false
  await loadData()
}

function tagType(type: string) {
  if (type === 'MENU') return 'primary'
  if (type === 'BUTTON') return 'success'
  return 'warning'
}

onMounted(() => {
  void loadData()
})
</script>

<style scoped lang="scss">
.permission-page {
  display: grid;
  grid-template-columns: 380px minmax(0, 1fr);
  gap: 16px;
}

.tree-panel,
.detail-panel {
  padding: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 12px;
}

.panel-head h1,
.panel-head h2,
.panel-head p {
  margin: 0;
}

.panel-head p {
  margin-top: 6px;
  color: var(--app-text-muted);
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tree-shell {
  max-height: 720px;
  overflow: auto;
}

.node-row {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.node-name {
  font-weight: 600;
}

.node-code {
  color: var(--app-text-muted);
  font-size: 12px;
}

.node-detail {
  max-width: 760px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;

  div {
    padding: 14px 16px;
    border-radius: 16px;
    background: rgba(148, 163, 184, 0.08);
  }

  span {
    display: block;
    font-size: 12px;
    color: var(--app-text-muted);
    margin-bottom: 6px;
  }

  strong {
    display: block;
    line-height: 1.5;
    word-break: break-word;
  }
}

.edit-form {
  max-width: 720px;
}

@media (max-width: 1080px) {
  .permission-page {
    grid-template-columns: 1fr;
  }
}
</style>
