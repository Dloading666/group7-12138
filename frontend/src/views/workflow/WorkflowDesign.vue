<template>
  <div class="workflow-design-page app-page">
    <div class="page-header-bar">
      <div class="page-title-block">
        <h2>{{ pageTitle }}</h2>
        <p>当前采用轻量顺序编排：把流程拆成可读的步骤卡片，先保证配置清晰，再逐步演进为更复杂的图形设计器。</p>
      </div>
      <div class="page-header-actions">
        <el-tag :type="statusTagType" effect="plain">{{ statusText }}</el-tag>
        <el-button @click="handleBack">返回列表</el-button>
        <el-button v-if="!isViewMode" @click="handleGenerateCode">生成编码</el-button>
        <el-button
          v-if="!isViewMode"
          type="success"
          plain
          :disabled="!workflowId || workflowForm.status === 'published'"
          :loading="publishing"
          @click="handlePublish"
        >
          发布流程
        </el-button>
        <el-button v-if="!isViewMode" type="primary" :loading="saving" @click="handleSave">保存流程</el-button>
      </div>
    </div>

    <div class="workflow-layout">
      <div class="workflow-main-stack">
        <div class="page-section padded">
          <div class="section-heading compact-heading">
            <div>
              <h3>流程信息</h3>
              <p>名称、编码和说明会直接影响列表页检索与执行记录展示，建议写得直白可搜索。</p>
            </div>
          </div>

          <el-form ref="formRef" :model="workflowForm" :rules="formRules" label-position="top">
            <div class="workflow-meta-grid">
              <el-form-item label="流程名称" prop="name">
                <el-input v-model="workflowForm.name" :disabled="isViewMode" placeholder="例如 MiniMax 官网巡检流程" />
              </el-form-item>
              <el-form-item label="流程编码" prop="workflowCode">
                <el-input v-model="workflowForm.workflowCode" :disabled="isViewMode" placeholder="例如 WF_MINIMAX_AUDIT" />
              </el-form-item>
              <el-form-item label="状态">
                <el-radio-group v-model="workflowForm.status" :disabled="isViewMode" class="status-radio-group">
                  <el-radio-button label="draft">草稿</el-radio-button>
                  <el-radio-button label="published">已发布</el-radio-button>
                  <el-radio-button label="archived">已归档</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </div>
            <el-form-item label="流程描述" prop="description">
              <el-input
                v-model="workflowForm.description"
                :disabled="isViewMode"
                type="textarea"
                :rows="4"
                maxlength="300"
                show-word-limit
                placeholder="描述流程目标、适用场景和关键依赖。"
              />
            </el-form-item>
          </el-form>
        </div>

        <div class="page-section padded">
          <div class="section-heading steps-heading">
            <div>
              <h3>步骤编排</h3>
              <p>步骤按顺序执行。每一步都可以填写节点类型、说明和配置 JSON，方便后续接入 AI 工作流执行器。</p>
            </div>
            <div class="page-header-actions wrap-actions" v-if="!isViewMode">
              <el-select v-model="selectedNodeType" filterable placeholder="选择节点类型" style="width: 240px">
                <el-option
                  v-for="item in nodeTypeOptions"
                  :key="item.type"
                  :label="`${item.name} (${item.type})`"
                  :value="item.type"
                />
              </el-select>
              <el-button type="primary" @click="handleAddStep()">
                <el-icon><Plus /></el-icon>
                添加步骤
              </el-button>
            </div>
          </div>

          <el-empty v-if="steps.length === 0" description="还没有步骤，可以从右上角选择节点类型后添加。">
            <el-button v-if="!isViewMode" type="primary" @click="handleAddStep()">添加第一步</el-button>
          </el-empty>

          <div v-else class="step-stack">
            <div v-for="(step, index) in steps" :key="step.id" class="workflow-step-card">
              <div class="workflow-step-head">
                <div class="step-index">{{ index + 1 }}</div>
                <div class="step-header-main">
                  <div class="step-header-row">
                    <el-input
                      v-model="step.label"
                      :disabled="isViewMode"
                      placeholder="步骤标题，例如 抓取官网页面"
                    />
                    <el-select v-model="step.type" :disabled="isViewMode" style="width: 220px">
                      <el-option
                        v-for="item in nodeTypeOptions"
                        :key="item.type"
                        :label="item.name"
                        :value="item.type"
                      />
                    </el-select>
                    <el-switch v-model="step.enabled" :disabled="isViewMode" inline-prompt active-text="启用" inactive-text="停用" />
                  </div>
                  <el-input
                    v-model="step.description"
                    :disabled="isViewMode"
                    placeholder="说明这一环节做什么，失败时应该从哪里排查。"
                  />
                </div>
                <div class="step-toolbar" v-if="!isViewMode">
                  <el-button link :disabled="index === 0" @click="moveStep(index, -1)">上移</el-button>
                  <el-button link :disabled="index === steps.length - 1" @click="moveStep(index, 1)">下移</el-button>
                  <el-button link type="danger" @click="removeStep(index)">删除</el-button>
                </div>
              </div>

              <div class="step-config-area">
                <div class="step-config-head">
                  <span>节点配置 JSON / 文本</span>
                  <el-tag effect="plain">{{ getNodeTypeName(step.type) }}</el-tag>
                </div>
                <el-input
                  v-model="step.configText"
                  :disabled="isViewMode"
                  type="textarea"
                  :rows="8"
                  placeholder='例如 { "url": "https://www.minimax.io/", "method": "GET" }'
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="workflow-side-stack">
        <div class="page-section padded side-card">
          <div class="side-card-title">流程摘要</div>
          <div class="status-grid compact-status-grid">
            <div class="status-metric">
              <div class="metric-label">步骤数量</div>
              <div class="metric-value">{{ steps.length }}</div>
            </div>
            <div class="status-metric">
              <div class="metric-label">启用步骤</div>
              <div class="metric-value">{{ enabledStepCount }}</div>
            </div>
            <div class="status-metric">
              <div class="metric-label">当前模式</div>
              <div class="metric-value">{{ modeLabel }}</div>
            </div>
          </div>
        </div>

        <div class="page-section padded side-card">
          <div class="side-card-title">节点类型</div>
          <div class="node-type-list">
            <div v-for="item in nodeTypeOptions" :key="item.type" class="node-type-item">
              <span class="node-type-dot" :style="{ background: item.color || '#94a3b8' }"></span>
              <div>
                <strong>{{ item.name }}</strong>
                <small>{{ item.type }}</small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createWorkflow, getAllNodeTypes, getWorkflowById, publishWorkflow, updateWorkflow } from '../../api/workflow.js'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const saving = ref(false)
const publishing = ref(false)
const nodeTypeOptions = ref([])
const steps = ref([])
const selectedNodeType = ref('')

const workflowId = computed(() => (route.query.id ? Number(route.query.id) : null))
const mode = computed(() => String(route.query.mode || (workflowId.value ? 'edit' : 'create')))
const isViewMode = computed(() => mode.value === 'view')

const workflowForm = reactive({
  name: '',
  workflowCode: '',
  description: '',
  status: 'draft'
})

const formRules = {
  name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
  workflowCode: [
    { required: true, message: '请输入流程编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_\-]+$/, message: '流程编码仅支持字母、数字、下划线和中横线', trigger: 'blur' }
  ]
}

const pageTitle = computed(() => {
  if (isViewMode.value) return '查看流程'
  return workflowId.value ? '编辑流程' : '新建流程'
})

const modeLabel = computed(() => {
  const map = {
    create: '新建',
    edit: '编辑',
    design: '设计',
    view: '查看'
  }
  return map[mode.value] || mode.value
})

const statusText = computed(() => {
  const map = {
    draft: '草稿',
    published: '已发布',
    archived: '已归档'
  }
  return map[workflowForm.status] || workflowForm.status || '-'
})

const statusTagType = computed(() => {
  const map = {
    draft: 'info',
    published: 'success',
    archived: 'warning'
  }
  return map[workflowForm.status] || 'info'
})

const enabledStepCount = computed(() => steps.value.filter((item) => item.enabled !== false).length)

const getNodeTypeName = (type) => {
  const matched = nodeTypeOptions.value.find((item) => item.type === type)
  return matched?.name || type || '-'
}

const createDefaultConfigText = (type) => {
  const matched = nodeTypeOptions.value.find((item) => item.type === type)
  if (matched?.defaultConfig) {
    try {
      return JSON.stringify(JSON.parse(matched.defaultConfig), null, 2)
    } catch (error) {
      return matched.defaultConfig
    }
  }
  if (type === 'http') {
    return JSON.stringify({ url: 'https://www.minimax.io/', method: 'GET' }, null, 2)
  }
  if (type === 'email') {
    return JSON.stringify({ to: '', subject: '', body: '' }, null, 2)
  }
  return ''
}

const stringifyConfig = (config) => {
  if (config === null || config === undefined || config === '') {
    return ''
  }
  if (typeof config === 'string') {
    return config
  }
  try {
    return JSON.stringify(config, null, 2)
  } catch (error) {
    return String(config)
  }
}

const normalizeStep = (item = {}, index = 0) => ({
  id: String(item.id || `node_${Date.now()}_${index}`),
  type: item.type || selectedNodeType.value || nodeTypeOptions.value[0]?.type || 'http',
  label: item.label || item.title || item.name || `步骤 ${index + 1}`,
  description: item.description || '',
  enabled: item.enabled !== false,
  configText: stringifyConfig(item.config)
})

const parseNodeConfig = (text) => {
  if (!text || !String(text).trim()) {
    return {}
  }
  try {
    return JSON.parse(text)
  } catch (error) {
    return { raw: text }
  }
}

const normalizeWorkflowConfig = (rawConfig) => {
  if (!rawConfig) {
    return []
  }
  try {
    const parsed = typeof rawConfig === 'string' ? JSON.parse(rawConfig) : rawConfig
    const items = Array.isArray(parsed?.nodes) ? parsed.nodes : Array.isArray(parsed?.steps) ? parsed.steps : []
    return items.map((item, index) => normalizeStep(item, index))
  } catch (error) {
    console.error('解析流程配置失败:', error)
    return []
  }
}

const buildWorkflowConfig = () => {
  const nodes = steps.value.map((step, index) => ({
    id: step.id,
    type: step.type,
    label: step.label,
    description: step.description,
    enabled: step.enabled,
    sortOrder: index + 1,
    config: parseNodeConfig(step.configText)
  }))

  const edges = nodes.slice(1).map((node, index) => ({
    id: `edge_${nodes[index].id}_${node.id}`,
    source: nodes[index].id,
    target: node.id
  }))

  return JSON.stringify({ nodes, edges }, null, 2)
}

const handleAddStep = (type = selectedNodeType.value) => {
  const resolvedType = type || nodeTypeOptions.value[0]?.type || 'http'
  steps.value.push({
    id: `node_${Date.now()}_${steps.value.length + 1}`,
    type: resolvedType,
    label: `${getNodeTypeName(resolvedType)} ${steps.value.length + 1}`,
    description: '',
    enabled: true,
    configText: createDefaultConfigText(resolvedType)
  })
}

const moveStep = (index, delta) => {
  const nextIndex = index + delta
  if (nextIndex < 0 || nextIndex >= steps.value.length) {
    return
  }
  const next = [...steps.value]
  ;[next[index], next[nextIndex]] = [next[nextIndex], next[index]]
  steps.value = next
}

const removeStep = async (index) => {
  try {
    await ElMessageBox.confirm('确认删除这个步骤吗？', '删除步骤', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    steps.value.splice(index, 1)
    ElMessage.success('步骤已删除')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除步骤失败:', error)
    }
  }
}

const handleGenerateCode = () => {
  const source = workflowForm.name || 'workflow'
  const normalized = source
    .replace(/[^A-Za-z0-9\u4e00-\u9fa5]+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '')
  workflowForm.workflowCode = (normalized || 'WORKFLOW').toUpperCase()
}

const loadNodeTypes = async () => {
  try {
    const res = await getAllNodeTypes()
    nodeTypeOptions.value = (res.data || []).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    if (!selectedNodeType.value) {
      selectedNodeType.value = nodeTypeOptions.value[0]?.type || 'http'
    }
  } catch (error) {
    console.error('加载节点类型失败:', error)
    nodeTypeOptions.value = [
      { type: 'http', name: 'HTTP 请求', color: '#409EFF' },
      { type: 'data_process', name: '数据处理', color: '#E6A23C' },
      { type: 'email', name: '邮件发送', color: '#F56C6C' }
    ]
    selectedNodeType.value = 'http'
  }
}

const loadWorkflow = async () => {
  if (!workflowId.value) {
    return
  }
  try {
    const res = await getWorkflowById(workflowId.value)
    const workflow = res.data || {}
    workflowForm.name = workflow.name || ''
    workflowForm.workflowCode = workflow.workflowCode || ''
    workflowForm.description = workflow.description || ''
    workflowForm.status = workflow.status || 'draft'
    steps.value = normalizeWorkflowConfig(workflow.config)
  } catch (error) {
    console.error('加载流程详情失败:', error)
    ElMessage.error('加载流程详情失败')
    handleBack()
  }
}

const handleSave = async () => {
  if (isViewMode.value) {
    return
  }

  try {
    await formRef.value?.validate()
    saving.value = true

    const payload = {
      workflowCode: workflowForm.workflowCode.trim(),
      name: workflowForm.name.trim(),
      description: workflowForm.description?.trim() || '',
      status: workflowForm.status,
      config: buildWorkflowConfig()
    }

    let res
    if (workflowId.value) {
      res = await updateWorkflow(workflowId.value, payload)
    } else {
      res = await createWorkflow(payload)
    }

    const savedWorkflow = res.data || {}
    workflowForm.status = savedWorkflow.status || workflowForm.status
    ElMessage.success('流程已保存')

    if (!workflowId.value && savedWorkflow.id) {
      router.replace({ path: '/workflow/design', query: { id: savedWorkflow.id, mode: 'edit' } })
    }
  } catch (error) {
    if (error) {
      console.error('保存流程失败:', error)
      ElMessage.error('保存流程失败')
    }
  } finally {
    saving.value = false
  }
}

const handlePublish = async () => {
  if (!workflowId.value) {
    ElMessage.warning('请先保存流程，再进行发布')
    return
  }

  try {
    publishing.value = true
    await publishWorkflow(workflowId.value)
    workflowForm.status = 'published'
    ElMessage.success('流程已发布')
  } catch (error) {
    console.error('发布流程失败:', error)
    ElMessage.error('发布流程失败')
  } finally {
    publishing.value = false
  }
}

const handleBack = () => {
  router.push('/workflow/list')
}

onMounted(async () => {
  await loadNodeTypes()
  await loadWorkflow()
})
</script>

<style scoped lang="scss">
.workflow-design-page {
  padding: 4px;
}

.workflow-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
}

.workflow-main-stack,
.workflow-side-stack {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.compact-heading {
  margin-bottom: 18px;
}

.workflow-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 16px;
}

.status-radio-group {
  display: flex;
  flex-wrap: wrap;
}

.steps-heading {
  margin-bottom: 18px;
}

.wrap-actions {
  flex-wrap: wrap;
}

.step-stack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.workflow-step-card {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 18px;
  padding: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.96));
}

.workflow-step-head {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: flex-start;
}

.step-index {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 16px;
  color: #fff;
  font-weight: 800;
  background: linear-gradient(135deg, #0f766e, #2563eb);
  box-shadow: 0 12px 30px rgba(14, 116, 144, 0.22);
}

.step-header-main {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.step-header-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px 120px;
  gap: 10px;
}

.step-toolbar {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.step-config-area {
  margin-top: 14px;
}

.step-config-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  color: var(--app-text-muted);
  font-size: 13px;
}

.side-card {
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

.node-type-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.node-type-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.node-type-item strong {
  display: block;
  color: var(--app-text);
}

.node-type-item small {
  color: var(--app-text-muted);
}

.node-type-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  flex-shrink: 0;
}

@media (max-width: 1280px) {
  .workflow-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .workflow-meta-grid,
  .step-header-row,
  .workflow-step-head {
    grid-template-columns: 1fr;
  }

  .step-toolbar {
    flex-direction: row;
    justify-content: flex-start;
    align-items: center;
  }
}
</style>
