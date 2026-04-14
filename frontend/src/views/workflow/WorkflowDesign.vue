<template>
  <div class="workflow-design-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <el-divider direction="vertical" />
        <span class="page-title">{{ workflowName || '新流程' }}</span>
      </div>
      <div class="header-right">
        <el-button @click="handleBack">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </div>
    </div>

    <!-- 流程信息区 -->
    <div class="workflow-info-section">
      <el-form :model="workflowForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="流程名称">
              <el-input v-model="workflowForm.name" placeholder="请输入流程名称" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="流程编码">
              <el-input v-model="workflowForm.workflowCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-tag :type="getStatusType(workflowForm.status)">
                {{ getStatusText(workflowForm.status) }}
              </el-tag>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="流程描述">
          <el-input 
            v-model="workflowForm.description" 
            type="textarea" 
            :rows="2"
            placeholder="请输入流程描述"
          />
        </el-form-item>
      </el-form>
    </div>

    <!-- 步骤配置区 -->
    <div class="steps-section">
      <div class="section-header">
        <h3>步骤配置</h3>
        <el-button type="primary" @click="handleAddStep">
          <el-icon><Plus /></el-icon>
          添加步骤
        </el-button>
      </div>

      <!-- 步骤卡片列表 -->
      <div class="steps-list" v-if="steps.length > 0">
        <div 
          v-for="(step, index) in steps" 
          :key="step.id" 
          class="step-card"
        >
          <!-- 步骤头部 -->
          <div class="step-header">
            <div class="step-info">
              <div class="step-number">{{ index + 1 }}</div>
              <div class="step-title-area">
                <el-input 
                  v-model="step.title" 
                  class="step-title-input"
                  placeholder="请输入步骤标题"
                  @blur="updateStepTitle(step)"
                />
                <el-select 
                  v-model="step.type" 
                  class="step-type-select"
                  placeholder="选择类型"
                  @change="updateStepType(step)"
                >
                  <el-option label="Java 爬虫代码" value="java_crawler" />
                  <el-option label="Python 爬虫代码" value="python_crawler" />
                  <el-option label="数据清洗" value="data_clean" />
                  <el-option label="数据存储" value="data_store" />
                  <el-option label="API调用" value="api_call" />
                  <el-option label="邮件通知" value="email_notify" />
                  <el-option label="其他" value="other" />
                </el-select>
              </div>
            </div>
            <div class="step-actions">
              <el-button 
                type="danger" 
                link 
                @click="handleDeleteStep(index)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>

          <!-- 步骤内容 -->
          <div class="step-content">
            <div 
              class="code-preview"
              :class="{ 'collapsed': !step.expanded }"
            >
              <el-input
                v-model="step.code"
                type="textarea"
                :rows="step.expanded ? 15 : 3"
                placeholder="请输入代码或配置内容"
                class="code-textarea"
              />
            </div>
            
            <!-- 展开/收起按钮 -->
            <div class="expand-btn" @click="toggleStepExpand(step)">
              <el-icon v-if="!step.expanded"><ArrowDown /></el-icon>
              <el-icon v-else><ArrowUp /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div class="empty-steps" v-else>
        <el-empty description="暂无步骤，请点击上方按钮添加">
          <el-button type="primary" @click="handleAddStep">
            <el-icon><Plus /></el-icon>
            添加步骤
          </el-button>
        </el-empty>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="footer-actions">
      <el-button @click="handleBack">取消</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving">
        保存
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  getWorkflowById, 
  createWorkflow, 
  updateWorkflow,
  publishWorkflow 
} from '../../api/workflow.js'

const router = useRouter()
const route = useRoute()

// 流程信息
const workflowName = ref('')
const workflowForm = ref({
  name: '',
  workflowCode: '',
  description: '',
  status: 'draft'
})

// 步骤列表
const steps = ref([])

// 保存状态
const saving = ref(false)

// 类型标签映射
const typeLabelMap = {
  'java_crawler': 'Java 爬虫代码',
  'python_crawler': 'Python 爬虫代码',
  'data_clean': '数据清洗',
  'data_store': '数据存储',
  'api_call': 'API调用',
  'email_notify': '邮件通知',
  'other': '其他'
}

// 加载流程数据
const loadWorkflow = async () => {
  const workflowId = route.query.id
  if (!workflowId) return

  try {
    const res = await getWorkflowById(workflowId)
    if (res.code === 200) {
      const workflow = res.data
      workflowName.value = workflow.name
      workflowForm.value = {
        name: workflow.name,
        workflowCode: workflow.workflowCode,
        description: workflow.description,
        status: workflow.status
      }

      // 解析步骤配置
      if (workflow.config) {
        const config = JSON.parse(workflow.config)
        steps.value = config.steps || []
      }
    }
  } catch (error) {
    console.error('加载流程失败:', error)
    ElMessage.error('加载流程失败')
  }
}

// 添加步骤
const handleAddStep = () => {
  steps.value.push({
    id: `step_${Date.now()}`,
    title: `步骤${steps.value.length + 1}`,
    type: 'java_crawler',
    code: '',
    expanded: false
  })
}

// 删除步骤
const handleDeleteStep = async (index) => {
  try {
    await ElMessageBox.confirm('确定要删除这个步骤吗？', '提示', {
      type: 'warning'
    })
    steps.value.splice(index, 1)
    ElMessage.success('删除成功')
  } catch (error) {
    // 取消删除
  }
}

// 展开/收起步骤
const toggleStepExpand = (step) => {
  step.expanded = !step.expanded
}

// 更新步骤标题
const updateStepTitle = (step) => {
  // 可以在这里添加自动保存逻辑
}

// 更新步骤类型
const updateStepType = (step) => {
  // 可以在这里添加自动保存逻辑
}

// 保存流程
const handleSave = async () => {
  if (!workflowForm.value.name) {
    ElMessage.warning('请输入流程名称')
    return
  }

  saving.value = true
  try {
    const workflowData = {
      name: workflowForm.value.name,
      description: workflowForm.value.description,
      config: JSON.stringify({
        steps: steps.value
      })
    }

    const workflowId = route.query.id
    let res
    
    if (workflowId) {
      res = await updateWorkflow(workflowId, workflowData)
    } else {
      res = await createWorkflow(workflowData)
    }

    if (res.code === 200) {
      ElMessage.success('保存成功')
      if (!workflowId && res.data.id) {
        router.replace({ 
          path: '/workflow/design', 
          query: { id: res.data.id } 
        })
      }
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 返回列表
const handleBack = () => {
  router.push('/workflow/list')
}

// 状态类型映射
const getStatusType = (status) => {
  const map = {
    'draft': 'info',
    'published': 'success',
    'archived': 'warning',
    'enabled': 'success'
  }
  return map[status] || 'info'
}

// 状态文本映射
const getStatusText = (status) => {
  const map = {
    'draft': '草稿',
    'published': '已发布',
    'archived': '已归档',
    'enabled': '启用'
  }
  return map[status] || status
}

// 页面加载
onMounted(() => {
  loadWorkflow()
})
</script>

<style scoped lang="scss">
.workflow-design-page {
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
  padding-bottom: 70px;

  // 页面头部
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 15px 20px;
    background: #fff;
    border-bottom: 1px solid #e8e8e8;
    position: sticky;
    top: 0;
    z-index: 100;

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .page-title {
        font-size: 16px;
        font-weight: 600;
        color: #333;
      }
    }

    .header-right {
      display: flex;
      gap: 10px;
    }
  }

  // 流程信息区
  .workflow-info-section {
    background: #fff;
    padding: 20px;
    margin: 20px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  // 步骤配置区
  .steps-section {
    background: #fff;
    padding: 20px;
    margin: 0 20px 20px;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #333;
      }
    }

    .steps-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .step-card {
      border: 1px solid #e8e8e8;
      border-radius: 8px;
      padding: 20px;
      background: #fafafa;
      transition: all 0.3s;

      &:hover {
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      }

      .step-header {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 15px;

        .step-info {
          display: flex;
          align-items: flex-start;
          gap: 15px;

          .step-number {
            width: 32px;
            height: 32px;
            background: #1890ff;
            color: #fff;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 600;
            font-size: 14px;
            flex-shrink: 0;
          }

          .step-title-area {
            display: flex;
            flex-direction: column;
            gap: 8px;

            .step-title-input {
              width: 300px;
            }

            .step-type-select {
              width: 150px;
            }
          }
        }

        .step-actions {
          display: flex;
          gap: 8px;
        }
      }

      .step-content {
        .code-preview {
          margin-bottom: 10px;

          &.collapsed {
            :deep(.el-textarea__inner) {
              resize: none;
            }
          }
        }

        .code-textarea {
          :deep(.el-textarea__inner) {
            font-family: 'Courier New', Consolas, monospace;
            font-size: 13px;
            line-height: 1.6;
            background: #f8f9fa;
          }
        }

        .expand-btn {
          text-align: center;
          cursor: pointer;
          color: #1890ff;
          padding: 5px;
          
          &:hover {
            background: #f0f0f0;
          }
        }
      }
    }

    .empty-steps {
      padding: 40px 0;
    }
  }

  // 底部操作栏
  .footer-actions {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 15px 20px;
    background: #fff;
    border-top: 1px solid #e8e8e8;
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    z-index: 100;
  }
}

// Element Plus 样式覆盖
:deep(.el-input__wrapper) {
  background: #fff;
}

:deep(.el-select .el-input__wrapper) {
  background: #fff;
}
</style>
