<template>
  <div class="resource-page">
    <section class="surface-panel toolbar">
      <div class="toolbar-head">
        <div>
          <h1>机器人管理</h1>
          <p>管理机器人在线状态、执行任务数和基础信息。</p>
        </div>
        <el-button v-permission="'robot:create'" type="primary" @click="openCreate">+ 新建机器人</el-button>
      </div>
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="名称"><el-input v-model="filters.name" placeholder="请输入机器人名称" clearable /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="请选择" style="width: 150px">
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
            <el-option label="忙碌" value="busy" />
            <el-option label="禁用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="surface-panel table-shell">
      <el-table :data="filteredRows" :loading="loading" height="520">
        <el-table-column prop="name" label="机器人名称" width="150" />
        <el-table-column prop="robotId" label="编号" width="120" />
        <el-table-column prop="ip" label="IP 地址" width="140" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }"><el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="taskCount" label="任务数" width="100" />
        <el-table-column prop="lastHeartbeat" label="最后心跳" width="180">
          <template #default="{ row }">{{ formatDateTime(row.lastHeartbeat) }}</template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editRobot(row)">编辑</el-button>
            <el-button v-permission="'robot:start'" size="small" type="success" @click="start(row)" :disabled="row.status === 'busy'">启动</el-button>
            <el-button v-permission="'robot:stop'" size="small" type="warning" @click="stop(row)" :disabled="row.status === 'offline'">停止</el-button>
            <el-button v-permission="'robot:delete'" size="small" type="danger" @click="removeRobot(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" placeholder="请输入机器人名称" /></el-form-item>
        <el-form-item label="编号" prop="robotId"><el-input v-model="form.robotId" placeholder="请输入机器人编号" /></el-form-item>
        <el-form-item label="IP 地址" prop="ip"><el-input v-model="form.ip" placeholder="请输入 IP 地址" /></el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="在线" value="online" />
            <el-option label="离线" value="offline" />
            <el-option label="忙碌" value="busy" />
            <el-option label="禁用" value="disabled" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入描述" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRobot">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createRobot, deleteRobot, getRobots, startRobot, stopRobot, updateRobot } from '@/api/robots'
import { demoRobots } from '@/mock/demo-data'
import { formatDateTime } from '@/utils/format'
import type { RobotItem } from '@/types/domain'

const loading = ref(false)
const saving = ref(false)
const rows = ref<RobotItem[]>(demoRobots)
const dialogVisible = ref(false)
const dialogTitle = ref('新建机器人')
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const filters = reactive({
  name: '',
  status: ''
})

const form = reactive<Partial<RobotItem>>({
  name: '',
  robotId: '',
  ip: '',
  status: 'online',
  description: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入机器人名称', trigger: 'blur' }],
  robotId: [{ required: true, message: '请输入机器人编号', trigger: 'blur' }],
  ip: [{ required: true, message: '请输入 IP 地址', trigger: 'blur' }]
}

const filteredRows = computed(() =>
  rows.value.filter((row) => {
    const matchName = !filters.name || row.name.includes(filters.name)
    const matchStatus = !filters.status || row.status === filters.status
    return matchName && matchStatus
  })
)

function statusText(status: RobotItem['status']) {
  const map = { online: '在线', offline: '离线', busy: '忙碌', disabled: '禁用' }
  return map[status] || status
}

function statusTag(status: RobotItem['status']) {
  const map = { online: 'success', busy: 'warning', offline: 'info', disabled: 'danger' }
  return map[status] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getRobots(filters)
    rows.value = res.data?.list || demoRobots
  } catch {
    rows.value = demoRobots
  } finally {
    loading.value = false
  }
}

function search() {
  loadData()
}

function reset() {
  filters.name = ''
  filters.status = ''
  loadData()
}

function openCreate() {
  dialogTitle.value = '新建机器人'
  editingId.value = null
  Object.assign(form, { name: '', robotId: '', ip: '', status: 'online', description: '' })
  dialogVisible.value = true
}

function editRobot(row: RobotItem) {
  dialogTitle.value = '编辑机器人'
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function saveRobot() {
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (editingId.value) {
        await updateRobot(editingId.value, form)
      } else {
        await createRobot(form)
      }
      dialogVisible.value = false
      await loadData()
    } catch {
      dialogVisible.value = false
      await loadData()
    } finally {
      saving.value = false
    }
  })
}

async function start(row: RobotItem) {
  try {
    await startRobot(row.id)
  } catch {
    // ignore
  } finally {
    row.status = 'busy'
  }
}

async function stop(row: RobotItem) {
  try {
    await stopRobot(row.id)
  } catch {
    // ignore
  } finally {
    row.status = 'offline'
  }
}

async function removeRobot(row: RobotItem) {
  await ElMessageBox.confirm(`确认删除机器人「${row.name}」吗？`, '提示', { type: 'warning' })
  try {
    await deleteRobot(row.id)
  } catch {
    // ignore
  } finally {
    rows.value = rows.value.filter((item) => item.id !== row.id)
  }
}

onMounted(loadData)
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
</style>

