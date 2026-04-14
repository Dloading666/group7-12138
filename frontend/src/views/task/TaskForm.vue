<template>
  <div class="task-form-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <el-button @click="handleBack" class="back-btn">
          <el-icon><ArrowLeft /></el-icon>
          返回
        </el-button>
        <span class="page-title">{{ isEdit ? '编辑任务' : '新增任务' }}</span>
      </div>
    </div>

    <!-- 表单区域 -->
    <div class="form-container">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
        class="task-form"
      >
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="任务名称" prop="name">
                <el-input 
                  v-model="formData.name" 
                  placeholder="请输入任务名称"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="任务类型" prop="type">
                <el-select 
                  v-model="formData.type" 
                  placeholder="请选择任务类型"
                  style="width: 100%"
                >
                  <el-option label="数据采集" value="数据采集" />
                  <el-option label="报表生成" value="报表生成" />
                  <el-option label="文件处理" value="文件处理" />
                  <el-option label="数据同步" value="数据同步" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="执行机器人" prop="robotId">
                <el-select 
                  v-model="formData.robotId" 
                  placeholder="请选择执行机器人"
                  style="width: 100%"
                >
                  <el-option 
                    v-for="robot in robotList" 
                    :key="robot.id" 
                    :label="`${robot.code} (${robot.name})`" 
                    :value="robot.id" 
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="优先级" prop="priority">
                <el-radio-group v-model="formData.priority">
                  <el-radio value="high">高</el-radio>
                  <el-radio value="medium">中</el-radio>
                  <el-radio value="low">低</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-title">执行设置</div>
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="执行方式" prop="executeType">
                <el-radio-group v-model="formData.executeType">
                  <el-radio value="immediate">立即执行</el-radio>
                  <el-radio value="scheduled">定时执行</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="12" v-if="formData.executeType === 'scheduled'">
              <el-form-item label="计划时间" prop="scheduledTime">
                <el-date-picker
                  v-model="formData.scheduledTime"
                  type="datetime"
                  placeholder="选择计划执行时间"
                  style="width: 100%"
                  :disabled-date="disabledDate"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-title">任务配置</div>
          <el-row :gutter="24">
            <el-col :span="24">
              <el-form-item label="任务描述" prop="description">
                <el-input 
                  v-model="formData.description" 
                  type="textarea"
                  placeholder="请输入任务描述"
                  maxlength="500"
                  show-word-limit
                  :rows="4"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 表单操作按钮 -->
        <div class="form-actions">
          <el-button @click="handleBack">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createTask, updateTask, getTaskDetail } from '../../api/task.js'
import { getRobotList } from '../../api/robot.js'

const router = useRouter()
const route = useRoute()

// 判断是编辑还是新增
const isEdit = computed(() => !!route.query.id)
const taskId = computed(() => route.query.id)

// 表单引用
const formRef = ref(null)
const submitLoading = ref(false)

// 机器人列表
const robotList = ref([])

// 表单数据
const formData = reactive({
  id: null,
  name: '',
  type: '',
  robotId: null,
  robotName: '',
  priority: 'medium',
  executeType: 'immediate',
  scheduledTime: null,
  description: ''
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { max: 100, message: '任务名称长度不能超过100个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择任务类型', trigger: 'change' }
  ],
  robotId: [
    { required: true, message: '请选择执行机器人', trigger: 'change' }
  ],
  executeType: [
    { required: true, message: '请选择执行方式', trigger: 'change' }
  ],
  scheduledTime: [
    { required: true, message: '请选择计划时间', trigger: 'change' }
  ]
}

// 加载机器人列表
const loadRobotList = async () => {
  try {
    const res = await getRobotList()
    if (res.code === 200) {
      // 只显示在线的机器人
      robotList.value = (res.data.content || []).filter(r => r.status === 'online')
    }
  } catch (error) {
    console.error('获取机器人列表失败:', error)
  }
}

// 加载任务详情
const loadTaskDetail = async () => {
  if (!taskId.value) return
  
  try {
    const res = await getTaskDetail(taskId.value)
    if (res.code === 200) {
      const task = res.data
      Object.assign(formData, {
        id: task.id,
        name: task.name,
        type: task.type,
        robotId: task.robotId,
        robotName: task.robotName,
        priority: task.priority || 'medium',
        executeType: task.executeType || 'immediate',
        scheduledTime: task.scheduledTime,
        description: task.description || ''
      })
    } else {
      ElMessage.error(res.message || '获取任务详情失败')
    }
  } catch (error) {
    console.error('获取任务详情失败:', error)
    ElMessage.error('获取任务详情失败')
  }
}

// 禁用过去的时间
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7 // 禁用今天之前的日期
}

// 返回列表
const handleBack = () => {
  router.push('/task/list')
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    submitLoading.value = true
    
    // 构建提交数据
    const submitData = {
      name: formData.name,
      type: formData.type,
      robotId: formData.robotId,
      robotName: robotList.value.find(r => r.id === formData.robotId)?.name || '',
      priority: formData.priority,
      executeType: formData.executeType,
      scheduledTime: formData.executeType === 'scheduled' ? formData.scheduledTime : null,
      description: formData.description
    }
    
    let res
    if (isEdit.value) {
      submitData.id = taskId.value
      res = await updateTask(submitData)
    } else {
      res = await createTask(submitData)
    }
    
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '保存成功' : '创建成功')
      router.push('/task/list')
    } else {
      ElMessage.error(res.message || (isEdit.value ? '保存失败' : '创建失败'))
    }
  } catch (error) {
    if (error !== false) {
      console.error('提交表单失败:', error)
      ElMessage.error('表单验证失败')
    }
  } finally {
    submitLoading.value = false
  }
}

// 初始化
onMounted(async () => {
  await loadRobotList()
  if (isEdit.value) {
    await loadTaskDetail()
  }
})
</script>

<style scoped lang="scss">
.task-form-page {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);

  // 页面头部
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 15px;

      .back-btn {
        display: flex;
        align-items: center;
        gap: 5px;
        color: #606266;
        border-color: #dcdfe6;

        &:hover {
          color: #409eff;
          border-color: #c6e2ff;
          background-color: #ecf5ff;
        }
      }

      .page-title {
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }
    }
  }

  // 表单容器
  .form-container {
    background: #fff;
    border-radius: 8px;
    padding: 30px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

    .task-form {
      max-width: 800px;

      // 分组标题
      .form-section {
        margin-bottom: 30px;

        .section-title {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 20px;
          padding-bottom: 10px;
          border-bottom: 1px solid #ebeef5;
        }
      }

      // 表单操作按钮
      .form-actions {
        display: flex;
        justify-content: center;
        gap: 15px;
        margin-top: 40px;
        padding-top: 30px;
        border-top: 1px solid #ebeef5;
      }
    }
  }
}

// Element Plus 样式覆盖
:deep(.el-form-item) {
  margin-bottom: 22px;

  .el-form-item__label {
    font-weight: 500;
    color: #606266;
  }
}

:deep(.el-input__wrapper) {
  border-radius: 4px;
}

:deep(.el-select .el-input__wrapper) {
  border-radius: 4px;
}

:deep(.el-radio-group) {
  .el-radio {
    margin-right: 20px;
    
    .el-radio__label {
      color: #606266;
    }
  }
}

:deep(.el-textarea__inner) {
  border-radius: 4px;
}

:deep(.el-button--primary) {
  background-color: #409eff;
  border-color: #409eff;
  padding: 12px 30px;

  &:hover {
    background-color: #66b1ff;
    border-color: #66b1ff;
  }
}

:deep(.el-button) {
  padding: 12px 30px;
  border-radius: 4px;
}

:deep(.el-date-editor) {
  .el-input__wrapper {
    border-radius: 4px;
  }
}
</style>
