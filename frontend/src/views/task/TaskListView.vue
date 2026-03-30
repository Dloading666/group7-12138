<template>
  <div class="resource-page" data-testid="task-list-page">
    <section class="surface-panel toolbar">
      <div class="toolbar-head">
        <div>
          <h1>任务列表</h1>
          <p>按任务名称、类型、状态和优先级进行筛选。</p>
        </div>
        <el-button
          v-permission="'task:create'"
          type="primary"
          data-testid="task-create-button"
          @click="openCreate"
        >
          + 创建任务
        </el-button>
      </div>

      <el-form :inline="true" :model="filters" class="filter-form" data-testid="task-filter-bar">
        <el-form-item label="任务名称">
          <el-input v-model="filters.name" placeholder="请输入任务名称" clearable />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-select v-model="filters.type" placeholder="请选择" clearable style="width: 140px">
            <el-option label="数据采集" value="数据采集" />
            <el-option label="数据同步" value="数据同步" />
            <el-option label="文件处理" value="文件处理" />
            <el-option label="报表生成" value="报表生成" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="请选择" clearable style="width: 140px">
            <el-option label="等待中" value="pending" />
            <el-option label="执行中" value="running" />
            <el-option label="已完成" value="completed" />
            <el-option label="失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="filters.priority" placeholder="请选择" clearable style="width: 140px">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="surface-panel table-shell">
      <div data-testid="task-table">
        <el-table :data="paginatedRows" :loading="loading" height="520">
          <el-table-column prop="taskId" label="任务编号" width="160" />
          <el-table-column prop="name" label="任务名称" min-width="180" />
          <el-table-column prop="type" label="任务类型" width="120" />
          <el-table-column prop="status" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="progress" label="进度" width="170">
            <template #default="{ row }">
              <el-progress :percentage="row.progress" :status="progressStatus(row.status)" :stroke-width="8" />
            </template>
          </el-table-column>
          <el-table-column prop="priority" label="优先级" width="100">
            <template #default="{ row }">
              <el-tag :type="priorityTag(row.priority)" effect="plain">
                {{ priorityText(row.priority) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="robotName" label="执行机器人" width="130" />
          <el-table-column prop="createTime" label="创建时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="320" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="showDetail(row)">详情</el-button>
              <el-button
                v-permission="'task:start'"
                size="small"
                type="success"
                :disabled="row.status !== 'pending'"
                @click="toggleTask(row, 'start')"
              >
                启动
              </el-button>
              <el-button
                v-permission="'task:stop'"
                size="small"
                type="warning"
                :disabled="row.status !== 'running'"
                @click="toggleTask(row, 'stop')"
              >
                停止
              </el-button>
              <el-button
                v-permission="'task:delete'"
                size="small"
                type="danger"
                :disabled="row.status === 'running'"
                @click="removeTask(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无任务数据" />
          </template>
        </el-table>
      </div>

      <div class="pagination-shell" data-testid="task-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="filteredRows.length"
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="任务类型" prop="type">
          <el-select v-model="form.type" style="width: 100%" placeholder="请选择">
            <el-option label="数据采集" value="数据采集" />
            <el-option label="数据同步" value="数据同步" />
            <el-option label="文件处理" value="文件处理" />
            <el-option label="报表生成" value="报表生成" />
          </el-select>
        </el-form-item>
        <el-form-item label="机器人" prop="robotName">
          <el-input v-model="form.robotName" placeholder="请输入执行机器人名称" />
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio-button label="high">高</el-radio-button>
            <el-radio-button label="medium">中</el-radio-button>
            <el-radio-button label="low">低</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="执行方式" prop="executeType">
          <el-radio-group v-model="form.executeType">
            <el-radio label="immediate">立即执行</el-radio>
            <el-radio label="scheduled">定时执行</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.executeType === 'scheduled'" label="计划时间" prop="scheduledTime">
          <el-date-picker
            v-model="form.scheduledTime"
            type="datetime"
            style="width: 100%"
            placeholder="请选择计划时间"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入任务描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTask">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="任务详情" width="720px">
      <el-descriptions v-if="currentRow" :column="2" border>
        <el-descriptions-item label="任务编号">{{ currentRow.taskId }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ currentRow.name }}</el-descriptions-item>
        <el-descriptions-item label="任务类型">{{ currentRow.type }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(currentRow.status) }}</el-descriptions-item>
        <el-descriptions-item label="进度">
          <el-progress :percentage="currentRow.progress" :status="progressStatus(currentRow.status)" />
        </el-descriptions-item>
        <el-descriptions-item label="优先级">{{ priorityText(currentRow.priority) }}</el-descriptions-item>
        <el-descriptions-item label="机器人">{{ currentRow.robotName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(currentRow.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentRow.description || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { createTask, deleteTask, getTasks, startTask, stopTask, updateTask } from '@/api/tasks'
import { demoTasks } from '@/mock/demo-data'
import { formatDateTime } from '@/utils/format'
import type { TaskItem } from '@/types/domain'

const route = useRoute()
const loading = ref(false)
const saving = ref(false)
const offlineMode = ref(false)
const rows = ref<TaskItem[]>(demoTasks)
const currentPage = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const dialogTitle = ref('创建任务')
const formRef = ref<FormInstance>()
const currentRow = ref<TaskItem | null>(null)
const editingId = ref<number | null>(null)

const filters = reactive({
  name: '',
  type: '',
  status: '',
  priority: ''
})

const form = reactive<Partial<TaskItem>>({
  name: '',
  type: '',
  robotName: '',
  priority: 'medium',
  executeType: 'immediate',
  scheduledTime: '',
  description: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  robotName: [{ required: true, message: '请输入机器人名称', trigger: 'blur' }]
}

const filteredRows = computed(() =>
  rows.value.filter((row) => {
    const matchName = !filters.name || row.name.includes(filters.name)
    const matchType = !filters.type || row.type === filters.type
    const matchStatus = !filters.status || row.status === filters.status
    const matchPriority = !filters.priority || row.priority === filters.priority
    return matchName && matchType && matchStatus && matchPriority
  })
)

const paginatedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

function statusText(status: TaskItem['status']) {
  const map = {
    pending: '等待中',
    running: '执行中',
    completed: '已完成',
    failed: '失败',
    stopped: '已停止'
  } as const
  return map[status] || status
}

function statusTag(status: TaskItem['status']) {
  const map = {
    pending: 'info',
    running: 'primary',
    completed: 'success',
    failed: 'danger',
    stopped: 'warning'
  } as const
  return map[status] || 'info'
}

function priorityText(priority: TaskItem['priority']) {
  const map = { high: '高', medium: '中', low: '低' } as const
  return map[priority] || priority
}

function priorityTag(priority: TaskItem['priority']) {
  const map = { high: 'danger', medium: 'warning', low: 'info' } as const
  return map[priority] || 'info'
}

function progressStatus(status: TaskItem['status']) {
  if (status === 'completed') return 'success'
  if (status === 'failed') return 'exception'
  return undefined
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await getTasks(filters)
    if (res.code === 200 && res.data) {
      rows.value = res.data.list || []
      offlineMode.value = false
      return
    }
  } catch {
    // fall back to demo data below
  } finally {
    loading.value = false
  }

  rows.value = demoTasks
  offlineMode.value = true
}

function search() {
  currentPage.value = 1
  loadTasks()
}

function reset() {
  filters.name = ''
  filters.type = ''
  filters.status = ''
  filters.priority = ''
  currentPage.value = 1
  loadTasks()
}

function openCreate() {
  dialogTitle.value = '创建任务'
  editingId.value = null
  Object.assign(form, {
    name: '',
    type: '',
    robotName: '',
    priority: 'medium',
    executeType: 'immediate',
    scheduledTime: '',
    description: ''
  })
  dialogVisible.value = true
}

function showDetail(row: TaskItem) {
  currentRow.value = row
  detailVisible.value = true
}

function handlePageChange(page: number) {
  currentPage.value = page
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
}

async function saveTask() {
  await formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = { ...form } as Partial<TaskItem>
      if (offlineMode.value) {
        if (editingId.value) {
          rows.value = rows.value.map((item) =>
            item.id === editingId.value ? ({ ...item, ...payload } as TaskItem) : item
          )
        } else {
          rows.value = [
            {
              id: Date.now(),
              taskId: `T-${Date.now()}`,
              status: 'pending',
              progress: 0,
              priority: (payload.priority as TaskItem['priority']) || 'medium',
              name: payload.name || '',
              type: payload.type || '',
              robotName: payload.robotName || '',
              executeType: payload.executeType as TaskItem['executeType'],
              scheduledTime: payload.scheduledTime,
              createTime: new Date().toISOString(),
              description: payload.description
            },
            ...rows.value
          ]
        }
      } else if (editingId.value) {
        await updateTask(editingId.value, payload)
      } else {
        await createTask(payload)
      }
      ElMessage.success(editingId.value ? '任务已更新' : '任务已创建')
      dialogVisible.value = false
      await loadTasks()
    } catch {
      ElMessage.error('保存失败')
    } finally {
      saving.value = false
    }
  })
}

async function toggleTask(row: TaskItem, action: 'start' | 'stop') {
  try {
    if (action === 'start') {
      if (!offlineMode.value) await startTask(row.id)
      row.status = 'running'
      row.progress = Math.max(row.progress, 10)
      ElMessage.success('任务已启动')
    } else {
      if (!offlineMode.value) await stopTask(row.id)
      row.status = 'stopped'
      ElMessage.success('任务已停止')
    }
    await loadTasks()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function removeTask(row: TaskItem) {
  if (row.status === 'running') {
    ElMessage.warning('执行中的任务不能删除')
    return
  }
  await ElMessageBox.confirm(`确认删除任务「${row.name}」吗？`, '提示', { type: 'warning' })
  try {
    if (!offlineMode.value) await deleteTask(row.id)
  } catch {
    // keep optimistic behavior for demo mode
  } finally {
    rows.value = rows.value.filter((item) => item.id !== row.id)
    ElMessage.success('删除成功')
  }
}

watch(
  () => route.query.create,
  (value) => {
    if (value === '1') openCreate()
  },
  { immediate: true }
)

onMounted(loadTasks)
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

.filter-form {
  margin-top: 10px;
}

.pagination-shell {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
