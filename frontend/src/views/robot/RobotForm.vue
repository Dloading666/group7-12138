<template>
  <div class="robot-form-page app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>{{ isEdit ? '编辑机器人' : '新增机器人' }}</h2>
        <p>统一维护机器人基础资料、状态与用途说明，避免表单与操作按钮重复占位。</p>
      </div>
      <div class="page-header-actions">
        <el-button @click="handleBack">返回列表</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">{{ isEdit ? '保存修改' : '创建机器人' }}</el-button>
      </div>
    </div>

    <div class="robot-layout" v-loading="loading">
      <div class="robot-main-stack">
        <div class="page-section padded">
          <div class="section-heading compact-heading">
            <div>
              <h3>基础信息</h3>
              <p>编码建议保持稳定，便于在任务调度、日志和执行记录里快速定位同一个机器人。</p>
            </div>
          </div>

          <el-form ref="formRef" :model="robotForm" :rules="formRules" label-position="top" class="robot-form-grid">
            <div class="form-grid two-columns">
              <el-form-item label="机器人编码" prop="robotCode">
                <el-input v-model="robotForm.robotCode" :disabled="isEdit" placeholder="例如 DC-002" />
              </el-form-item>

              <el-form-item label="机器人名称" prop="name">
                <el-input v-model="robotForm.name" placeholder="例如 MiniMax 数据采集机器人" />
              </el-form-item>

              <el-form-item label="机器人类型" prop="type">
                <el-select
                  v-model="robotForm.type"
                  filterable
                  allow-create
                  default-first-option
                  placeholder="选择或输入类型"
                >
                  <el-option v-for="item in typeOptions" :key="item.value" :label="item.label" :value="item.value" />
                </el-select>
              </el-form-item>

              <el-form-item label="运行状态" prop="status">
                <el-radio-group v-model="robotForm.status" class="status-radio-group">
                  <el-radio-button value="online">在线</el-radio-button>
                  <el-radio-button value="offline">离线</el-radio-button>
                  <el-radio-button value="running">运行中</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </div>

            <el-form-item label="描述说明" prop="description">
              <el-input
                v-model="robotForm.description"
                type="textarea"
                :rows="6"
                maxlength="300"
                show-word-limit
                placeholder="补充这个机器人负责什么、适合执行哪类任务、是否依赖特定环境。"
              />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <div class="robot-side-stack">
        <div class="page-section padded overview-card">
          <div class="side-card-title">状态概览</div>
          <div class="status-grid compact-status-grid">
            <div class="status-metric">
              <div class="metric-label">当前状态</div>
              <div class="metric-value">
                <el-tag :type="statusTagType">{{ statusLabel }}</el-tag>
              </div>
            </div>
            <div class="status-metric">
              <div class="metric-label">机器人类型</div>
              <div class="metric-value">{{ typeLabel }}</div>
            </div>
          </div>

          <template v-if="isEdit && robotStats">
            <div class="mini-metrics">
              <div>
                <span>累计任务</span>
                <strong>{{ robotStats.totalTasks ?? 0 }}</strong>
              </div>
              <div>
                <span>成功率</span>
                <strong>{{ formatSuccessRate(robotStats.successRate) }}</strong>
              </div>
              <div>
                <span>最近执行</span>
                <strong>{{ formatDateTime(robotStats.lastExecuteTime) }}</strong>
              </div>
            </div>
          </template>
          <template v-else>
            <ul class="hint-list compact-list">
              <li>采集机器人建议使用 <code>data_collector</code> 类型，便于配置页筛选。</li>
              <li>编码建议使用稳定前缀，例如 <code>DC</code>、<code>RG</code>、<code>TS</code>。</li>
              <li>描述里写清用途，后续在任务列表里更容易判断该选哪个机器人。</li>
            </ul>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createRobot, getRobotById, updateRobot } from '../../api/robot.js'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const loading = ref(false)
const submitLoading = ref(false)
const robotStats = ref(null)

const robotId = computed(() => route.params.id)
const isEdit = computed(() => Boolean(robotId.value))

const typeOptions = [
  { label: '数据采集', value: 'data_collector' },
  { label: '报表生成', value: 'report_generator' },
  { label: '任务调度', value: 'task_scheduler' },
  { label: '消息通知', value: 'notification' },
  { label: '文件处理', value: 'file_processor' }
]

const robotForm = reactive({
  robotCode: '',
  name: '',
  type: 'data_collector',
  description: '',
  status: 'offline'
})

const formRules = {
  robotCode: [{ required: true, message: '请输入机器人编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入机器人名称', trigger: 'blur' }],
  type: [{ required: true, message: '请输入机器人类型', trigger: 'change' }]
}

const statusLabel = computed(() => {
  const map = {
    online: '在线',
    offline: '离线',
    running: '运行中'
  }
  return map[robotForm.status] || robotForm.status || '-'
})

const statusTagType = computed(() => {
  const map = {
    online: 'success',
    offline: 'info',
    running: 'warning'
  }
  return map[robotForm.status] || 'info'
})

const typeLabel = computed(() => {
  const matched = typeOptions.find((item) => item.value === robotForm.type)
  return matched?.label || robotForm.type || '-'
})

const loadRobotData = async () => {
  if (!robotId.value) {
    return
  }

  loading.value = true
  try {
    const res = await getRobotById(robotId.value)
    const robot = res.data || {}
    robotForm.robotCode = robot.robotCode || ''
    robotForm.name = robot.name || ''
    robotForm.type = robot.type || 'data_collector'
    robotForm.description = robot.description || ''
    robotForm.status = robot.status || 'offline'
    robotStats.value = robot
  } catch (error) {
    console.error('加载机器人数据失败:', error)
    ElMessage.error('加载机器人数据失败')
    handleBack()
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
    submitLoading.value = true

    const payload = {
      robotCode: robotForm.robotCode.trim(),
      name: robotForm.name.trim(),
      type: robotForm.type?.trim(),
      description: robotForm.description?.trim() || '',
      status: robotForm.status
    }

    if (isEdit.value) {
      await updateRobot(robotId.value, payload)
      ElMessage.success('机器人信息已更新')
    } else {
      await createRobot(payload)
      ElMessage.success('机器人已创建')
    }

    router.push('/robot/list')
  } catch (error) {
    if (error) {
      console.error('保存机器人失败:', error)
      ElMessage.error('保存机器人失败')
    }
  } finally {
    submitLoading.value = false
  }
}

const handleBack = () => {
  router.push('/robot/list')
}

const formatSuccessRate = (value) => {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  const numeric = Number(value)
  if (Number.isNaN(numeric)) {
    return String(value)
  }
  if (numeric <= 1) {
    return `${(numeric * 100).toFixed(1)}%`
  }
  return `${numeric.toFixed(1)}%`
}

const formatDateTime = (value) => {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

onMounted(() => {
  loadRobotData()
})
</script>

<style scoped lang="scss">
.robot-form-page {
  padding: 4px;
}

.robot-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
}

.robot-main-stack,
.robot-side-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.compact-heading {
  margin-bottom: 18px;
}

.form-grid.two-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.status-radio-group {
  display: flex;
  flex-wrap: wrap;
}

.overview-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.side-card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text);
}

.compact-status-grid {
  grid-template-columns: 1fr;
}

.mini-metrics {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mini-metrics div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.mini-metrics span {
  color: var(--app-text-muted);
}

.mini-metrics strong {
  color: var(--app-text);
}

.compact-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
  color: var(--app-text);
}

.compact-list code {
  padding: 2px 5px;
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.06);
}

@media (max-width: 1200px) {
  .robot-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .form-grid.two-columns {
    grid-template-columns: 1fr;
  }
}
</style>
