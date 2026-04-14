<template>
  <div class="task-create">
    <el-card>
      <template #header>
        <span>创建任务</span>
      </template>

      <el-form :model="taskForm" :rules="rules" ref="taskFormRef" label-width="120px" style="max-width: 800px;">
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="taskForm.name" placeholder="请输入任务名称" />
        </el-form-item>

        <el-form-item label="任务类型" prop="type">
          <el-select v-model="taskForm.type" placeholder="请选择任务类型" style="width: 100%;">
            <el-option label="数据采集" value="data-collection" />
            <el-option label="报表生成" value="report" />
            <el-option label="文件处理" value="file-process" />
            <el-option label="数据同步" value="data-sync" />
          </el-select>
        </el-form-item>

        <el-form-item label="执行机器人" prop="robotId">
          <el-select v-model="taskForm.robotId" placeholder="请选择执行机器人" style="width: 100%;" :loading="robotLoading">
            <el-option 
              v-for="robot in robotList" 
              :key="robot.id" 
              :label="robot.name" 
              :value="robot.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="执行方式" prop="executeType">
          <el-radio-group v-model="taskForm.executeType">
            <el-radio value="immediate">立即执行</el-radio>
            <el-radio value="scheduled">定时执行</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="执行时间" prop="scheduledTime" v-if="taskForm.executeType === 'scheduled'">
          <el-date-picker
            v-model="taskForm.scheduledTime"
            type="datetime"
            placeholder="选择执行时间"
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item label="优先级" prop="priority">
          <el-select v-model="taskForm.priority" placeholder="请选择优先级" style="width: 100%;">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>

        <el-form-item label="任务参数" prop="params">
          <el-input
            v-model="taskForm.params"
            type="textarea"
            :rows="6"
            placeholder="请输入任务参数（JSON格式）"
          />
        </el-form-item>

        <el-form-item label="任务描述" prop="description">
          <el-input
            v-model="taskForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入任务描述"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">创建任务</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAllRobots } from '../../api/robot.js'
import { createTask } from '../../api/task.js'

const router = useRouter()
const taskFormRef = ref(null)
const robotLoading = ref(false)
const submitting = ref(false)
const robotList = ref([])

const taskForm = reactive({
  name: '',
  type: '',
  robotId: '',
  executeType: 'immediate',
  scheduledTime: '',
  priority: 'medium',
  params: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择任务类型', trigger: 'change' }],
  robotId: [{ required: true, message: '请选择执行机器人', trigger: 'change' }],
  executeType: [{ required: true, message: '请选择执行方式', trigger: 'change' }]
}

// 加载机器人列表
const loadRobots = async () => {
  robotLoading.value = true
  try {
    const res = await getAllRobots()
    robotList.value = res.data || []
  } catch (error) {
    console.error('加载机器人列表失败:', error)
    ElMessage.error('加载机器人列表失败')
  } finally {
    robotLoading.value = false
  }
}

// 提交创建任务
const handleSubmit = async () => {
  const valid = await taskFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    // 获取选中的机器人名称
    const selectedRobot = robotList.value.find(r => r.id === taskForm.robotId)
    
    const taskData = {
      name: taskForm.name,
      type: taskForm.type,
      robotId: taskForm.robotId,
      robotName: selectedRobot?.name || '',
      priority: taskForm.priority,
      executeType: taskForm.executeType,
      scheduledTime: taskForm.scheduledTime,
      description: taskForm.description
    }

    await createTask(taskData)
    ElMessage.success('任务创建成功')
    router.push('/task/list')
  } catch (error) {
    console.error('创建任务失败:', error)
    ElMessage.error(error.response?.data?.message || '创建任务失败')
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  router.push('/task/list')
}

onMounted(() => {
  loadRobots()
})
</script>

<style scoped lang="scss">
.task-create {
  padding: 20px;
}
</style>
